package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.DiscussionReportProxy;
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
 * 讨论区举报
 *
 * @author cz
 */
@Table(value = "discussion_report")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class DiscussionReport implements ProxyEntityAvailable<DiscussionReport, DiscussionReportProxy> {
    @Column(primaryKey = true)
    public UUID id;
    /** discussion_topic / discussion_post */
    public String targetType;
    public UUID targetId;
    public UUID reporterId;
    /** spam / abuse / porn / political / copyright / other */
    public String reason;
    public String detail;
    /** pending / accepted / rejected */
    public String status;
    public UUID handledBy;
    public LocalDateTime handledAt;
    public String resultNote;
    public LocalDateTime createdAt;
}
