package com.czdr.work.service.impl;

import com.czdr.work.model.entity.WorkflowInstance;
import com.czdr.work.model.enums.WorkflowConstants;
import com.czdr.work.service.WorkflowContentGateway;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import io.camunda.client.CamundaClient;
import io.camunda.client.api.search.enums.UserTaskState;
import io.camunda.client.api.search.response.UserTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 工作流对账（reconciliation）：把本地的 {@code workflow_instance} 与 Camunda 引擎对齐。
 *
 * <h3>为什么需要它</h3>
 * Camunda 8 的 REST 查询读的是**二次存储**（RDBMS/Elasticsearch 导出器），相对引擎写入有
 * 短暂延迟。用户任务刚完成时，新任务可能还没落库、流程实例也还没进终态。若在提交请求的
 * 事务里死等，会拖长事务并可能超时；因此：
 * <ul>
 *   <li>提交路径只做有上限的短轮询（见 {@code WorkflowServiceImpl#refreshInstanceFromEngine}）；</li>
 *   <li>没追上就交给本组件周期性复查，把实例状态/环节补正、期满未完结的实例收尾。</li>
 * </ul>
 *
 * <h3>对账规则</h3>
 * <ol>
 *   <li>本地为 running 的实例，若引擎里已无对应待办任务且流程实例已结束 → 收尾
 *       （status=completed/stopped，current_stage 按内容实际状态决定）；</li>
 *   <li>本地为 running 的实例，若引擎里已有新的待办任务 → 把 current_stage / task 信息刷新；</li>
 *   <li>引擎里已不存在该流程实例（被清理/删除）→ 本地标记为 superseded 收尾。</li>
 * </ol>
 *
 * @author cz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowSettlementScheduler {

    private final EasyEntityQuery entityQuery;
    private final CamundaClient camundaClient;
    private final WorkflowContentGateway contentGateway;

    /**
     * 每 30 秒对账一次（首次延迟 20 秒，避开启动期的部署与索引建立）。
     * <p>用 Cron 而不是 fixedRate：本项目未开启 @EnableScheduling 级别的全局开关，
     * 这里显式使用 {@code fixedDelayString} 由 Spring Boot 自动配置的调度器驱动。</p>
     */
    @Scheduled(initialDelayString = "${app.camunda.settlement.initial-delay-ms:20000}",
            fixedDelayString = "${app.camunda.settlement.interval-ms:30000}")
    @Transactional
    public void reconcile() {
        List<WorkflowInstance> running = entityQuery.queryable(WorkflowInstance.class)
                .where(i -> i.status().eq(WorkflowConstants.INSTANCE_RUNNING))
                .toList();
        if (running.isEmpty()) {
            return;
        }

        // 一次性拉取引擎所有未完成用户任务，本地按实例 key 匹配（避免逐条查询）
        Map<Long, UserTask> engineTasks = new HashMap<>();
        try {
            camundaClient.newUserTaskSearchRequest()
                    .filter(f -> f.state(UserTaskState.CREATED))
                    .send()
                    .join()
                    .items()
                    .stream()
                    .sorted(Comparator.comparing(UserTask::getCreationDate,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .forEach(t -> engineTasks.putIfAbsent(t.getProcessInstanceKey(), t));
        } catch (Exception e) {
            log.warn("工作流对账：查询引擎用户任务失败，跳过本轮: {}", e.getMessage());
            return;
        }

        Set<Long> instanceKeys = running.stream()
                .map(WorkflowInstance::getProcessInstanceKey)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> engineStates = queryEngineStates(instanceKeys);

        int settled = 0;
        int refreshed = 0;
        for (WorkflowInstance instance : running) {
            Long key = instance.getProcessInstanceKey();
            if (key == null) {
                continue;
            }
            UserTask task = engineTasks.get(key);
            if (task != null) {
                String stage = stageOfElement(task.getElementId());
                boolean changed = !stage.equals(instance.getCurrentStage())
                        || !task.getUserTaskKey().equals(instance.getCurrentTaskKey());
                if (changed) {
                    instance.setCurrentStage(stage);
                    instance.setCurrentTaskKey(task.getUserTaskKey());
                    instance.setCurrentTaskName(task.getName());
                    instance.setUpdatedAt(LocalDateTime.now());
                    entityQuery.updatable(instance).executeRows();
                    refreshed++;
                }
                continue;
            }
            String state = engineStates.get(key);
            if (state == null) {
                // 引擎里已查不到该流程实例：视为已被清理，按取代收尾
                instance.setStatus(WorkflowConstants.INSTANCE_SUPERSEDED);
                instance.setCurrentStage(WorkflowConstants.STAGE_STOPPED);
                instance.setCurrentTaskKey(null);
                instance.setCurrentTaskName(null);
                instance.setFinishedAt(LocalDateTime.now());
                instance.setUpdatedAt(LocalDateTime.now());
                entityQuery.updatable(instance).executeRows();
                settled++;
            } else if ("COMPLETED".equalsIgnoreCase(state) || "TERMINATED".equalsIgnoreCase(state)) {
                settleInstance(instance);
                settled++;
            }
        }
        if (settled > 0 || refreshed > 0) {
            log.info("工作流对账完成: 收尾 {} 个实例，刷新 {} 个实例环节", settled, refreshed);
        }
    }

    /** 批量查流程实例状态（key -> state 名） */
    private Map<Long, String> queryEngineStates(Set<Long> keys) {
        Map<Long, String> map = new HashMap<>();
        for (Long key : keys) {
            try {
                var pi = camundaClient.newProcessInstanceGetRequest(key).send().join();
                if (pi.getState() != null) {
                    map.put(key, pi.getState().name());
                }
            } catch (Exception e) {
                // 查不到（已归档/被删）时不放入 map，由调用方按「引擎无此实例」处理
                log.debug("对账：查询流程实例 {} 状态失败: {}", key, e.getMessage());
            }
        }
        return map;
    }

    /**
     * 收尾：按内容最终状态决定实例的 status / current_stage，并同步审核态快照。
     * <p>与 {@code WorkflowServiceImpl#finishInstance} 同口径，这里独立实现以避免
     * 对本组件引入对 service 的循环依赖。</p>
     */
    private void settleInstance(WorkflowInstance instance) {
        String contentStatus = contentGateway.load(instance.getEntryType(), instance.getEntryId()).status();
        String stage = switch (contentStatus == null ? "" : contentStatus) {
            case WorkflowConstants.CONTENT_PUBLISHED -> WorkflowConstants.STAGE_PUBLISHED;
            case WorkflowConstants.CONTENT_OFFLINE -> WorkflowConstants.STAGE_REVISING;
            case WorkflowConstants.CONTENT_DRAFT -> WorkflowConstants.STAGE_STOPPED;
            default -> WorkflowConstants.STAGE_PUBLISHED;
        };
        boolean completed = WorkflowConstants.STAGE_PUBLISHED.equals(stage);
        instance.setCurrentStage(stage);
        instance.setStatus(completed ? WorkflowConstants.INSTANCE_COMPLETED : WorkflowConstants.INSTANCE_SUPERSEDED);
        instance.setCurrentTaskKey(null);
        instance.setCurrentTaskName(null);
        instance.setCurrentAssigneeId(null);
        instance.setFinishedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(instance).executeRows();

        String reviewStatus = switch (stage) {
            case WorkflowConstants.STAGE_PUBLISHED -> WorkflowConstants.REVIEW_APPROVED;
            case WorkflowConstants.STAGE_REVISING -> WorkflowConstants.REVIEW_REVISING;
            default -> WorkflowConstants.REVIEW_REJECTED;
        };
        syncReviewSnapshot(instance, reviewStatus);
    }

    /** 同步 content_review 快照（对账专用，不覆盖已有意见摘要） */
    private void syncReviewSnapshot(WorkflowInstance instance, String reviewStatus) {
        var review = entityQuery.queryable(com.czdr.work.model.entity.ContentReview.class)
                .where(r -> {
                    r.entryType().eq(instance.getEntryType());
                    r.entryId().eq(instance.getEntryId());
                })
                .firstOrNull();
        if (review == null) {
            return;
        }
        if (!reviewStatus.equals(review.getStatus())) {
            review.setStatus(reviewStatus);
            entityQuery.updatable(review).executeRows();
        }
    }

    /** BPMN elementId -> 环节（与 WorkflowServiceImpl 保持一致） */
    private String stageOfElement(String elementId) {
        if (elementId == null) {
            return WorkflowConstants.STAGE_PENDING_REVIEW;
        }
        return switch (elementId) {
            case "Task_ReviewerApprove" -> WorkflowConstants.STAGE_PENDING_REVIEW;
            case "Task_ContentInspect" -> WorkflowConstants.STAGE_PENDING_INSPECT;
            case "Task_EditorRevise" -> WorkflowConstants.STAGE_REVISING;
            default -> WorkflowConstants.STAGE_PENDING_REVIEW;
        };
    }
}
