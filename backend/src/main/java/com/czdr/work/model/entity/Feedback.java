package com.czdr.work.model.entity;

import com.czdr.work.model.entity.proxy.FeedbackProxy;
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
 * 用户反馈（C 端首页浮动按钮提交）
 *
 * @author cz
 */
@Table(value = "feedback")
@Data
@EntityProxy
@AllArgsConstructor
@NoArgsConstructor
public class Feedback implements ProxyEntityAvailable<Feedback, FeedbackProxy> {
    @Column(primaryKey = true)
    public UUID id;
    /** 称呼 */
    public String name;
    /** 联系方式（邮箱 / 手机号） */
    public String contact;
    /** 反馈类型：correction / suggestion / bug */
    public String topic;
    /** 满意度：good / ok / bad */
    public String rating;
    /** 反馈内容 */
    public String content;
    /** 访问日期（前端 date 控件，按字符串存储） */
    public String visitDate;
    public LocalDateTime createdAt;
}
