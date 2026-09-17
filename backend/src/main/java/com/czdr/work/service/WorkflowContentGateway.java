package com.czdr.work.service;

import java.util.UUID;

/**
 * 工作流与「业务内容表」之间的适配层。
 *
 * <p>工作流只关心四件事：内容标题、内容状态、内容版本号、内容是否存在。
 * 把它抽成接口后，新增业务类型（节日/艺术/专题，或以后的美食/人物）只需要实现/扩展这一层，
 * 不必改动 Camunda 流转逻辑与待办查询。</p>
 *
 * @author cz
 */
public interface WorkflowContentGateway {

    /** 内容摘要（缺失字段为 null） */
    record ContentSnapshot(UUID id, String title, String status, Integer contentVersion) {
    }

    /** 支持的内容类型 */
    boolean supports(String entryType);

    /** 读取内容摘要（不存在则抛业务异常） */
    ContentSnapshot load(String entryType, UUID entryId);

    /** 变更内容状态：draft / pending / published / rejected / offline */
    void updateStatus(String entryType, UUID entryId, String status);

    /** 内容版本号 +1（提交审批时调用） */
    void bumpContentVersion(String entryType, UUID entryId, int newVersion);

    /** 内容变更后刷新检索索引（可空实现） */
    default void reindex(String entryType, UUID entryId) {
    }
}
