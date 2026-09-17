package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.ProcessBindingProxy;
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
 * 业务类型 → Camunda 流程定义绑定（网页版 BPMN Modeler 的持久化）。
 *
 * @author cz
 */
@Table(value = "process_binding")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class ProcessBinding implements ProxyEntityAvailable<ProcessBinding, ProcessBindingProxy> {
    @Column(primaryKey = true)
    public UUID id;
    /** 流程标识，如 ethnic-content-review */
    public String processKey;
    public String name;
    /** 业务类型：ethnic / festival / art / topic / * */
    public String entryType;
    public Integer version;
    /** BPMN XML 原文 */
    public String bpmnXml;
    /** 部署到 Camunda 后回填 */
    public String camundaDefinitionId;
    public Integer camundaDefinitionVersion;
    public Boolean enabled;
    public String remark;
    public UUID createdBy;
    public UUID updatedBy;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
