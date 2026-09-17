package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.ContentReviewProxy;
import com.easy.query.core.annotation.Column;
import com.easy.query.core.annotation.EntityProxy;
import com.easy.query.core.annotation.Table;
import com.easy.query.core.proxy.ProxyEntityAvailable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

/**
 * @author cz
 */
@Table(value = "content_review")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class ContentReview implements ProxyEntityAvailable<ContentReview, ContentReviewProxy> {
    @Column(primaryKey = true)
    public UUID id;
    public String entryType;
    public UUID entryId;
    /** pending 待审批 / approved 已通过 / rejected 已驳回 / offline 已暂时下线 / revising 待修改 */
    public String status;
    public UUID submitterId;
    public UUID reviewerId;
    public String rejectReason;
    public LocalDateTime submittedAt;
    public LocalDateTime reviewedAt;
    /** 关联的工作流实例（可空，兼容历史数据） */
    public UUID instanceId;
    /** 内容版本号 */
    public Integer contentVersion;
    /** 最近一条审批/审查意见摘要，列表页直接展示 */
    public String lastOpinion;

    public ContentReview(UUID id, String entryType, UUID entryId, String status) {
        this.id = id;
        this.entryType = entryType;
        this.entryId = entryId;
        this.status = status;
        this.submitterId = null;
        this.reviewerId = null;
        this.rejectReason = "";
        this.submittedAt = LocalDateTime.now();
        this.reviewedAt = LocalDateTime.now();
    }
}
