package com.czdr.work.model.entity;

import com.czdr.work.config.PgSQLStringSupportJsonbTypeHandler;
import com.czdr.work.model.entity.proxy.SearchDocumentProxy;
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
 * 统一检索索引（方向 D·V16）。
 *
 * <p>把 8 类内容聚合成一张宽表，使跨类型检索与统一排序只需一次查询。
 * 由 {@code SearchIndexService} 负责构建与重建。</p>
 *
 * @author cz
 */
@Table(value = "search_document")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class SearchDocument implements ProxyEntityAvailable<SearchDocument, SearchDocumentProxy> {
    @Column(primaryKey = true)
    public UUID id;
    /** 内容类型：ethnic/festival/art/food/custom/person/area/sport */
    public String docType;
    /** 业务表主键 */
    public UUID docId;
    /** 详情页路由 */
    public String url;
    /** 标题 */
    public String title;
    /** 摘要 */
    public String summary;
    /** 正文 */
    public String body;
    /** 所属民族名 */
    public String ethnicName;
    /** 封面图 */
    public String coverImage;
    /** 主题色 */
    public String themeColor;
    /** 归属地区 */
    public String region;
    /** 分类（节日类型/艺术类别/人物领域等） */
    public String category;
    /** 全拼 */
    public String pinyinFull;
    /** 首字母缩写 */
    public String pinyinAbbr;
    /** 英文标题 */
    public String titleEn;
    /** 英文正文 */
    public String bodyEn;
    /** 热度（浏览/点赞/收藏加权） */
    public Integer popularity;
    /** 内容更新时间（用于时效排序与推荐衰减） */
    public LocalDateTime contentAt;
    public LocalDateTime updatedAt;
}
