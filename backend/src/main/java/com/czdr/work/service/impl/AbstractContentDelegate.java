package com.czdr.work.service.impl;

import com.czdr.work.model.entity.WorkflowInstance;
import com.czdr.work.model.enums.WorkflowConstants;
import com.czdr.work.service.WorkflowContentGateway;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Camunda 7 服务任务 handler 的公共基类：解析流程变量 + 定位本地工作流实例。
 *
 * <h3>为什么用 JavaDelegate</h3>
 * 原先 Camunda 8 用的是「job worker + 异步外部任务」：需要 worker 轮询拉取 job，
 * 完成后引擎状态经二次存储（Elasticsearch）才可见，因此产生了可见性延迟与随之而来的
 * 对账/轮询补偿逻辑。Camunda 7 的 {@link JavaDelegate} 由引擎在**流程推进的同一线程、
 * 同一事务**内同步调用，执行完立即写历史库，不存在延迟 ——
 * 这是迁移后能够移除「短轮询 + 定时对账」的根因。
 *
 * <h3>引擎与业务表的关联</h3>
 * Camunda 7 的流程实例 ID 是字符串（默认形如 {@code <processKey>-<uuid>}），
 * 与本地 {@code workflow_instance.process_instance_key} 一一对应。
 *
 * @author cz
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractContentDelegate implements JavaDelegate {

    protected final WorkflowContentGateway contentGateway;
    protected final EasyEntityQuery entityQuery;
    protected final WorkflowServiceImpl workflowService;

    /** 从流程变量解析出的业务上下文 */
    protected record Context(String entryType, UUID entryId, Integer contentVersion, WorkflowInstance instance) {
    }

    /**
     * 解析流程变量并定位本地工作流实例。
     * <p>流程实例查不到时不抛异常：内容状态副作用仍应执行，只是跳过本地实例的环节刷新
     * （例如管理员已删除该内容、或本地实例被清理）。</p>
     */
    protected Context resolve(DelegateExecution execution) {
        String entryType = stringVar(execution, WorkflowConstants.VAR_ENTRY_TYPE);
        String entryIdRaw = stringVar(execution, WorkflowConstants.VAR_ENTRY_ID);
        UUID entryId = entryIdRaw == null ? null : UUID.fromString(entryIdRaw);
        Object versionRaw = execution.getVariable(WorkflowConstants.VAR_CONTENT_VERSION);
        Integer version = versionRaw == null ? null : Integer.valueOf(String.valueOf(versionRaw));

        WorkflowInstance instance = entityQuery.queryable(WorkflowInstance.class)
                .where(i -> i.processInstanceKey().eq(execution.getProcessInstanceId()))
                .firstOrNull();
        return new Context(entryType, entryId, version, instance);
    }

    protected String stringVar(DelegateExecution execution, String key) {
        Object v = execution.getVariable(key);
        return v == null ? null : String.valueOf(v);
    }

    /** 刷新本地实例的环节并落库 */
    protected void updateStage(WorkflowInstance instance, String stage) {
        if (instance == null) {
            return;
        }
        instance.setCurrentStage(stage);
        instance.setUpdatedAt(LocalDateTime.now());
        entityQuery.updatable(instance).executeRows();
    }
}
