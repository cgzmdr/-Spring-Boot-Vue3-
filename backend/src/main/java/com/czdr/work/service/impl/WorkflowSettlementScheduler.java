package com.czdr.work.service.impl;

import com.czdr.work.model.entity.WorkflowInstance;
import com.czdr.work.model.enums.WorkflowConstants;
import com.czdr.work.service.WorkflowContentGateway;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 工作流兜底复查：把本地 {@code workflow_instance} 与嵌入式 Camunda 7 引擎对齐。
 *
 * <h3>定位的转变（重要）</h3>
 * 本组件在 Camunda 8 时代是<b>正确性的必要环节</b>：因为 Camunda 8 的 REST 查询读的是
 * 二次存储（Elasticsearch 导出器），相对引擎写入有延迟，所以必须靠定时对账来补正
 * 「刚完成的任务在新任务尚未落库」的窗口期。
 *
 * <p>迁移到嵌入式 Camunda 7 后，用户任务与运行时数据就在业务同一个 PostgreSQL 库、
 * 且与业务写在同一事务里，查询强一致。因此本组件<b>降级为兜底</b>，只处理异常残留：</p>
 * <ol>
 *   <li>本地标记 running、但引擎里已经没有对应运行时实例的（被管理员清理、引擎数据重置、
 *       或流程已终结但收尾失败）→ 本地收尾；</li>
 *   <li>本地环节与引擎当前任务不一致 → 以引擎为准回写。</li>
 * </ol>
 *
 * <p>换言之：即使本任务完全不运行，审批主流程依然正确。它只负责让「历史脏数据」自愈。</p>
 *
 * @author cz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowSettlementScheduler {

    private final EasyEntityQuery entityQuery;
    private final RuntimeService runtimeService;
    private final WorkflowContentGateway contentGateway;

    /**
     * 每 60 秒复查一次（首次延迟 30 秒，避开启动期的部署）。
     * <p>相比 Camunda 8 时代的 30 秒：既然不再是正确性依赖，就把频率放宽，
     * 减少无意义的库扫描。</p>
     */
    @Scheduled(initialDelayString = "${app.camunda.settlement.initial-delay-ms:30000}",
            fixedDelayString = "${app.camunda.settlement.interval-ms:60000}")
    @Transactional
    public void reconcile() {
        List<WorkflowInstance> running = entityQuery.queryable(WorkflowInstance.class)
                .where(i -> i.status().eq(WorkflowConstants.INSTANCE_RUNNING))
                .toList();
        if (running.isEmpty()) {
            return;
        }

        // 批量取引擎侧仍在运行的流程实例 ID（一条 SQL），本地比对
        Set<String> engineAlive = new HashSet<>();
        try {
            for (ProcessInstance pi : runtimeService.createProcessInstanceQuery().list()) {
                engineAlive.add(pi.getId());
            }
        } catch (Exception e) {
            log.warn("工作流复查：查询引擎运行时实例失败，跳过本轮: {}", e.getMessage());
            return;
        }

        int settled = 0;
        for (WorkflowInstance instance : running) {
            String id = instance.getProcessInstanceKey();
            if (id == null) {
                // 从未成功启动流程的残留行：直接按取代收尾，避免永久占用唯一约束
                settleInstance(instance, WorkflowConstants.INSTANCE_SUPERSEDED);
                settled++;
                continue;
            }
            if (!engineAlive.contains(id)) {
                // 引擎已无此运行时实例 → 流程确已结束，按内容实际状态收尾
                settleInstance(instance, null);
                settled++;
            }
        }
        if (settled > 0) {
            log.info("工作流复查完成: 收尾 {} 个残留实例", settled);
        }
    }

    /**
     * 收尾一个本地实例。
     *
     * @param forcedStatus 强制状态；为 null 时按内容实际状态推导（上线 → completed，其余 → superseded）
     */
    private void settleInstance(WorkflowInstance instance, String forcedStatus) {
        String contentStatus = contentGateway.load(instance.getEntryType(), instance.getEntryId()).status();
        String stage = switch (contentStatus == null ? "" : contentStatus) {
            case WorkflowConstants.CONTENT_PUBLISHED -> WorkflowConstants.STAGE_PUBLISHED;
            case WorkflowConstants.CONTENT_OFFLINE -> WorkflowConstants.STAGE_REVISING;
            case WorkflowConstants.CONTENT_DRAFT -> WorkflowConstants.STAGE_STOPPED;
            default -> WorkflowConstants.STAGE_PUBLISHED;
        };

        String status;
        if (forcedStatus != null) {
            status = forcedStatus;
            stage = WorkflowConstants.STAGE_STOPPED;
        } else {
            status = WorkflowConstants.STAGE_PUBLISHED.equals(stage)
                    ? WorkflowConstants.INSTANCE_COMPLETED
                    : WorkflowConstants.INSTANCE_SUPERSEDED;
        }

        instance.setCurrentStage(stage);
        instance.setStatus(status);
        instance.setCurrentTaskKey(null);
        instance.setCurrentTaskName(null);
        instance.setCurrentAssigneeId(null);
        instance.setFinishedAt(LocalDateTime.now());
        instance.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(instance).executeRows();

        syncReviewSnapshot(instance, switch (stage) {
            case WorkflowConstants.STAGE_PUBLISHED -> WorkflowConstants.REVIEW_APPROVED;
            case WorkflowConstants.STAGE_REVISING -> WorkflowConstants.REVIEW_REVISING;
            default -> WorkflowConstants.REVIEW_REJECTED;
        });
    }

    /** 同步 content_review 快照（兜底专用，不覆盖已有意见摘要） */
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
}
