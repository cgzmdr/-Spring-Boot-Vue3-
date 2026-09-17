package com.czdr.work.service.impl;

import com.czdr.work.model.entity.WorkflowInstance;
import com.czdr.work.model.enums.WorkflowConstants;
import com.czdr.work.service.WorkflowContentGateway;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * 内容暂时下线：内容管理员审查发现问题时执行。
 *
 * <p>对应 BPMN 中 {@code Task_Offline} 的 {@code camunda:class}。
 * 下线原因取 {@code offlineReason}，缺失时回退到 {@code inspectionOpinion}，
 * 保证「为什么被下线」始终有据可查。</p>
 *
 * @author cz
 */
@Slf4j
@Component("workflowOfflineDelegate")
public class WorkflowOfflineDelegate extends AbstractContentDelegate {

    public WorkflowOfflineDelegate(WorkflowContentGateway contentGateway,
                                   EasyEntityQuery entityQuery,
                                   WorkflowServiceImpl workflowService) {
        super(contentGateway, entityQuery, workflowService);
    }

    @Override
    public void execute(DelegateExecution execution) {
        Context ctx = resolve(execution);
        log.info("【工作流】内容暂时下线: {}#{} v{}", ctx.entryType(), ctx.entryId(), ctx.contentVersion());

        contentGateway.updateStatus(ctx.entryType(), ctx.entryId(), WorkflowConstants.CONTENT_OFFLINE);

        String reason = stringVar(execution, WorkflowConstants.VAR_OFFLINE_REASON);
        if (reason == null) {
            reason = stringVar(execution, WorkflowConstants.VAR_INSPECTION_OPINION);
        }

        WorkflowInstance instance = ctx.instance();
        updateStage(instance, WorkflowConstants.STAGE_REVISING);
        if (instance != null) {
            workflowService.addSystemOpinion(instance, WorkflowConstants.OPINION_OFFLINE,
                    WorkflowConstants.DECISION_ISSUE,
                    "内容已暂时下线，待内容编辑修改" + (reason == null ? "" : "：" + reason));
            workflowService.syncReviewSnapshot(ctx.entryType(), ctx.entryId(),
                    WorkflowConstants.REVIEW_REVISING, instance.getId(),
                    instance.getContentVersion(), reason);
        }
    }
}
