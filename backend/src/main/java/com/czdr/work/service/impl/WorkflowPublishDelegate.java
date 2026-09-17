package com.czdr.work.service.impl;

import com.czdr.work.model.entity.WorkflowInstance;
import com.czdr.work.model.enums.WorkflowConstants;
import com.czdr.work.service.WorkflowContentGateway;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.springframework.stereotype.Component;

/**
 * 内容上线：审批通过（或修改后直接重新上线）时执行。
 *
 * <p>对应 BPMN 中 {@code Task_Publish} 的 {@code camunda:class}。
 * 由引擎在流程推进的同一事务内同步调用；若状态写回失败会抛异常形成 incident，
 * 可在后台看到并重试，保证「上线」这一副作用不会静默丢失。</p>
 *
 * @author cz
 */
@Slf4j
@Component("workflowPublishDelegate")
public class WorkflowPublishDelegate extends AbstractContentDelegate {

    public WorkflowPublishDelegate(WorkflowContentGateway contentGateway,
                                   EasyEntityQuery entityQuery,
                                   WorkflowServiceImpl workflowService) {
        super(contentGateway, entityQuery, workflowService);
    }

    @Override
    public void execute(DelegateExecution execution) {
        Context ctx = resolve(execution);
        log.info("【工作流】内容上线: {}#{} v{}", ctx.entryType(), ctx.entryId(), ctx.contentVersion());

        contentGateway.updateStatus(ctx.entryType(), ctx.entryId(), WorkflowConstants.CONTENT_PUBLISHED);

        WorkflowInstance instance = ctx.instance();
        updateStage(instance, WorkflowConstants.STAGE_PENDING_INSPECT);
        if (instance != null) {
            workflowService.addSystemOpinion(instance, WorkflowConstants.OPINION_PUBLISH,
                    WorkflowConstants.DECISION_APPROVED, "审批通过，内容已上线");
            workflowService.syncReviewSnapshot(ctx.entryType(), ctx.entryId(),
                    WorkflowConstants.REVIEW_PENDING, instance.getId(),
                    instance.getContentVersion(), "审批通过，内容已上线");
        }
    }
}
