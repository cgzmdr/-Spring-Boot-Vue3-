package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.WorkflowOpinionProxy;
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
 * 审批 / 审查意见流水（append-only）。
 * <p>审批意见、审查意见、下线说明、修改说明全部落这张表，按内容版本归档，
 * 内容编辑据此查看「前面的审批意见和审查意见」。</p>
 *
 * @author cz
 */
@Table(value = "workflow_opinion")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowOpinion implements ProxyEntityAvailable<WorkflowOpinion, WorkflowOpinionProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public UUID instanceId;
    public String entryType;
    public UUID entryId;
    public Integer contentVersion;

    /** submit 提交 / approve 审批 / inspect 审查 / offline 下线 / revise 修改说明 / publish 上线 */
    public String stage;
    /** approved 通过 / rejected 驳回退回 / issue 发现问题 / resolved 已修正 / submitted 已提交 */
    public String decision;
    /** 意见正文 */
    public String opinion;

    public UUID operatorId;
    public String operatorName;
    /** reviewer / content_admin / editor */
    public String operatorRole;
    /** Camunda 用户任务 ID（Camunda 7 为字符串 ID，原 Camunda 8 为 Long key） */
    public String camundaTaskKey;
    public String taskName;

    public LocalDateTime createdAt;
}
