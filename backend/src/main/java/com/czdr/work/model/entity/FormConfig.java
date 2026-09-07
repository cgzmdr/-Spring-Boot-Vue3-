package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.FormConfigProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.JDBCType;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 动态表单配置。
 * <p>前端通过 id / code 获取 schema 并动态渲染表单，schema 为 JSON 文本：</p>
 * <pre>
 * {
 *   "fields": [
 *     { "key": "name", "label": "姓名", "type": "text", "required": true, "placeholder": "请输入" },
 *     { "key": "city", "label": "城市", "type": "select", "options": [{"label": "北京", "value": "bj"}] },
 *     { "key": "bio", "label": "简介", "type": "textarea" }
 *   ]
 * }
 * </pre>
 *
 * @author cz
 */
@Table(value = "form_config")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class FormConfig implements ProxyEntityAvailable<FormConfig, FormConfigProxy> {
    @Column(primaryKey = true)
    public UUID id;
    /** 表单编码（唯一，便于前端引用） */
    public String code;
    /** 表单名称 */
    public String name;
    /** 表单说明 */
    public String description;
    /** 表单 schema（JSON 文本） */
    @Column(dbType = "jsonb", jdbcType = JDBCType.JAVA_OBJECT,
            typeHandler = PgSQLStringSupportJsonbTypeHandler.class)
    public String schema;
    /** 状态：active / disabled */
    public String status;
    public UUID createdBy;
    public UUID updatedBy;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
