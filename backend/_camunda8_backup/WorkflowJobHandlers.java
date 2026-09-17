package com.czdr.work.service.impl;

import com.czdr.work.model.entity.WorkflowInstance;
import com.czdr.work.model.enums.WorkflowConstants;
import com.czdr.work.service.WorkflowContentGateway;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import io.camunda.client.annotation.JobWorker;
import io.camunda.client.api.response.ActivatedJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Camunda 服务任务处理器：把流程里的「内容上线 / 内容暂时下线」落到业务表。
 *
 * <p>为什么用 serviceTask 而不是在网关分支上直接改状态：</p>
 * <ul>
 *   <li>Zeebe 的排他网关只做条件判断，不具备「带重试的副作用」语义；
 *       服务任务由 job worker 执行，失败会自动重试并形成 incident，可在 Operate 里看到；</li>
 *   <li>上线/下线是必须留痕、必须可重入的动作，放 job worker 里天然幂等。</li>
 * </ul>
 *
 * @author cz
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowJobHandlers {

    private final WorkflowContentGateway contentGateway;
    private final EasyEntityQuery entityQuery;
    private final WorkflowServiceImpl workflowService;

    /**
     * 内容上线：审批通过（或修改后重新上线）时执行。
     * <p>对应 BPMN 中 {@code Task_Publish} 的 taskDefinition type = content-publish。</p>
     */
    @JobWorker(type = WorkflowConstants.JOB_PUBLISH, autoComplete = true)
    @Transactional
    public Map<String, Object> handlePublish(ActivatedJob job) {
        Context ctx = context(job);
        log.info("【工作流】内容上线: {}#{} v{}", ctx.entryType(), ctx.entryId(), ctx.contentVersion());

        contentGateway.updateStatus(ctx.entryType(), ctx.entryId(), WorkflowConstants.CONTENT_PUBLISHED);

        WorkflowInstance instance = ctx.instance();
        if (instance != null) {
            instance.setCurrentStage(WorkflowConstants.STAGE_PENDING_INSPECT);
            instance.setUpdatedAt(LocalDateTime.now());
            entityQuery.updatable(instance).executeRows();
            workflowService.addSystemOpinion(instance, WorkflowConstants.OPINION_PUBLISH,
                    WorkflowConstants.DECISION_APPROVED, "审批通过，内容已上线");
            workflowService.syncReviewSnapshot(ctx.entryType(), ctx.entryId(),
                    WorkflowConstants.REVIEW_PENDING, instance.getId(),
                    instance.getContentVersion(), "审批通过，内容已上线");
        }
        return Map.of("published", true);
    }

    /**
     * 内容暂时下线：内容管理员审查发现问题时执行。
     * <p>对应 BPMN 中 {@code Task_Offline} 的 taskDefinition type = content-offline。</p>
     */
    @JobWorker(type = WorkflowConstants.JOB_OFFLINE, autoComplete = true)
    @Transactional
    public Map<String, Object> handleOffline(ActivatedJob job) {
        Context ctx = context(job);
        log.info("【工作流】内容暂时下线: {}#{} v{}", ctx.entryType(), ctx.entryId(), ctx.contentVersion());

        contentGateway.updateStatus(ctx.entryType(), ctx.entryId(), WorkflowConstants.CONTENT_OFFLINE);

        WorkflowInstance instance = ctx.instance();
        String reason = stringVar(job, WorkflowConstants.VAR_OFFLINE_REASON);
        if (reason == null) {
            reason = stringVar(job, WorkflowConstants.VAR_INSPECTION_OPINION);
        }
        if (instance != null) {
            instance.setCurrentStage(WorkflowConstants.STAGE_REVISING);
            instance.setUpdatedAt(LocalDateTime.now());
            entityQuery.updatable(instance).executeRows();
            workflowService.addSystemOpinion(instance, WorkflowConstants.OPINION_OFFLINE,
                    WorkflowConstants.DECISION_ISSUE,
                    "内容已暂时下线，待内容编辑修改" + (reason == null ? "" : "：" + reason));
            workflowService.syncReviewSnapshot(ctx.entryType(), ctx.entryId(),
                    WorkflowConstants.REVIEW_REVISING, instance.getId(),
                    instance.getContentVersion(), reason);
        }
        return Map.of("offline", true);
    }

    // ==================== 工具 ====================

    private Context context(ActivatedJob job) {
        Map<String, Object> vars = job.getVariablesAsMap();
        String entryType = String.valueOf(vars.get(WorkflowConstants.VAR_ENTRY_TYPE));
        String entryIdRaw = String.valueOf(vars.get(WorkflowConstants.VAR_ENTRY_ID));
        UUID entryId = UUID.fromString(entryIdRaw);
        Object versionRaw = vars.get(WorkflowConstants.VAR_CONTENT_VERSION);
        Integer version = versionRaw == null ? null : Integer.valueOf(String.valueOf(versionRaw));

        WorkflowInstance instance = entityQuery.queryable(WorkflowInstance.class)
                .where(i -> i.processInstanceKey().eq(job.getProcessInstanceKey()))
                .firstOrNull();
        return new Context(entryType, entryId, version, instance);
    }

    private String stringVar(ActivatedJob job, String key) {
        Object v = job.getVariablesAsMap().get(key);
        return v == null ? null : String.valueOf(v);
    }

    private record Context(String entryType, UUID entryId, Integer contentVersion, WorkflowInstance instance) {
    }
}
