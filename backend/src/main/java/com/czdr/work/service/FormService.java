package com.czdr.work.service;

import com.czdr.work.model.entity.FormConfig;
import com.easy.query.core.api.pagination.EasyPageResult;
import org.springframework.data.domain.Pageable;

/**
 * 动态表单配置服务
 *
 * @author cz
 */
public interface FormService {

    /** 表单配置分页列表（后台） */
    EasyPageResult<FormConfig> list(String keyword, String status, Pageable pageable);

    /** 后台详情（含草稿/禁用） */
    FormConfig getById(String id);

    /** 创建表单配置 */
    void create(FormConfig body);

    /** 更新表单配置 */
    void update(String id, FormConfig body);

    /** 删除表单配置 */
    void delete(String id);

    /** 公开查询：按 ID 获取启用的表单配置（前端按 id 渲染） */
    FormConfig getActiveById(String id);

    /** 公开查询：按编码获取启用的表单配置 */
    FormConfig getActiveByCode(String code);
}
