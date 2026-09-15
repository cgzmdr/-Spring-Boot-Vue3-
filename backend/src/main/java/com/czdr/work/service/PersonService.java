package com.czdr.work.service;

import com.czdr.work.model.resource.PersonDirectoryResource;

/**
 * 人物专栏（B-7）：民族人物 / 非遗传承人
 *
 * @author cz
 */
public interface PersonService {

    /**
     * 人物名录（支持关键词 / 领域 / 民族 / 角色类型筛选）。
     *
     * @param keyword  姓名或项目名关键词，可为空
     * @param domain   领域（音乐/舞蹈/戏剧/服饰/技艺/建筑），可为空
     * @param ethnic   民族名，可为空
     * @param roleType inheritor / master，可为空（全部）
     */
    PersonDirectoryResource directory(String keyword, String domain, String ethnic, String roleType);
}
