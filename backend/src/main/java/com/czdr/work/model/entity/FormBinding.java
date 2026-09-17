package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.FormBindingProxy;
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
 * Camunda Form（form-js schema）绑定：审批 / 审查 / 修改表单。
 * <p>前端用 {@code @bpmn-io/form-js} 的 FormRenderer 直接渲染 {@code schemaJson}，
 * 保证与审核页面 Element Plus 风格一致（字段级 appearance 统一由前端主题覆写）。</p>
 *
 * @author cz
 */
@Table(value = "form_binding")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class FormBinding implements ProxyEntityAvailable<FormBinding, FormBindingProxy> {
    @Column(primaryKey = true)
    public UUID id;
    /** Camunda Form 的 formId，与 BPMN 中 userTask 的 formId 一致 */
    public String formId;
    public String name;
    /** approval 审批 / inspection 审查 / revision 修改 */
    public String purpose;
    /** 业务类型，* 表示通用 */
    public String entryType;
    public Integer version;
    /** form-js schema JSON 原文 */
    public String schemaJson;
    public Boolean enabled;
    public String remark;
    public UUID createdBy;
    public UUID updatedBy;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
