package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.PersonProfileProxy;
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
 * 人物档案（B-7 民族人物 / 传承人专栏）
 *
 * <p>承载「人物维度」的扩展属性（角色类型 / 领域 / 简介 / 生卒年），
 * 以 {@code (person_name, ethnic_group_name)} 与 {@code art.inheritors} 关联。
 * <b>不复制传承人姓名</b> —— {@code art} 表仍是姓名的唯一数据源，避免两处维护不一致。</p>
 *
 * @author cz
 */
@Table(value = "person_profile")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class PersonProfile implements ProxyEntityAvailable<PersonProfile, PersonProfileProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String personName;
    public String ethnicGroupName;
    /** inheritor 代表性传承人 / master 历史文化名家 */
    public String roleType;
    /** 领域：音乐 / 舞蹈 / 戏剧 / 服饰 / 技艺 / 建筑 */
    public String domain;
    public String bio;
    /** 生卒年，如「1894—1961」 */
    public String lifespan;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;
}
