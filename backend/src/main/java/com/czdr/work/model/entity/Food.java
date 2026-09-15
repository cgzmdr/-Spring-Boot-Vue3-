package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.FoodProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Navigate;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.enums.RelationTypeEnum;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "food")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Food implements ProxyEntityAvailable<Food, FoodProxy> {
    /**
     * 主键。必须显式标注 {@code primaryKey}：EasyQuery 需要据此生成 WHERE 条件，
     * 缺失时报「entity:Food not found primary key properties」，
     * 导致 food 表的任何按 id 更新（如英文正文回填）全部失败。
     */
    @Column(primaryKey = true)
    public UUID id;
    public UUID ethnicGroupId;
    public String name;
    public String nameEn;
    public String description;
    /** 英文正文（方向 C-3）；为空时前端回退显示中文 description */
    public String descriptionEn;
    /** 英文正文来源：machine 机器翻译 / reviewed 人工校对 / manual 后台录入 */
    public String descriptionEnSource;
    /** 发展沿革 */
    public String origin;
    public String image;
    public Integer orderNum;
    public LocalDateTime createdAt;
    public LocalDateTime updatedAt;

    @Navigate(value = RelationTypeEnum.ManyToOne, selfProperty = "ethnicGroupId", targetProperty = "id")
    public EthnicGroup ethnicGroup;
}
