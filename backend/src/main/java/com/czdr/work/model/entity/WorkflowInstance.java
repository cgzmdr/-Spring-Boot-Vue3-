package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.WorkflowInstanceProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 内容审批工作流实例。
 * <p>一条内容的一次「提交审批」= 一行：绑定内容版本 + Camunda 流程实例 + 当前环节。</p>
 * <p>「当前正在跑」的行 status=running；闭环走完后置 completed，历史行保留用于回看前序意见。</p>
 *
 * @author cz
 */
@Table(value = "workflow_instance")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowInstance implements ProxyEntityAvailable<WorkflowInstance, WorkflowInstanceProxy> {
    @Column(primaryKey = true)
    public UUID id;
    /** 业务内容类型：ethnic / festival / art / topic */
    public String entryType;
    public UUID entryId;
    /** 冗余内容标题，待办列表免二次查询 */
    public String entryTitle;
    /** 第几版内容（内容表每次提交审批自增） */
    public Integer contentVersion;

    /** Camunda 流程定义 id（如 ethnic-content-review） */
    public String processDefinitionId;
    /** Camunda 流程定义版本 */
    public Integer processDefinitionVersion;
    /**
     * Camunda 流程实例 ID。
     * <p><b>Camunda 8 → 7 变更</b>：Camunda 8（Zeebe）的流程实例 key 是 2^53 量级的
     * 长整型，故原字段为 {@code Long}；Camunda 7 的流程实例 ID 是<b>字符串</b>
     * （默认形如 {@code <processKey>-<uuid>}），因此改为 {@code String}。</p>
     */
    public String processInstanceKey;
    /** 业务键：entryType:entryId:v版本 */
    public String businessKey;

    /** running / completed / withdrawn / superseded */
    public String status;
    /** pending_review 待审批 / pending_inspect 待审查 / revising 待修改 / published 已上线 / stopped 已终止 */
    public String currentStage;

    public UUID submitterId;
    public String submitterName;
    /** 当前待办负责人（可空表示由候选组认领） */
    public UUID currentAssigneeId;
    /** 当前用户任务 ID（Camunda 7 为字符串 ID，原 Camunda 8 为 Long key） */
    public String currentTaskKey;
    public String currentTaskName;
    /** 第几轮流转 */
    public Integer roundNo;

    public LocalDateTime startedAt;
    public LocalDateTime finishedAt;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
