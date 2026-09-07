package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.model.entity.Feedback;
import com.czdr.work.model.request.FeedbackSubmitRequest;
import com.czdr.work.service.FeedbackService;
import com.easy.query.api.proxy.client.EasyEntityQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

/**
 * @author cz
 */
@Service
@RequiredArgsConstructor
public class FeedbackServiceImpl implements FeedbackService {

    private static final Set<String> TOPICS = Set.of("correction", "suggestion", "bug");
    private static final Set<String> RATINGS = Set.of("good", "ok", "bad");

    private final EasyEntityQuery entityQuery;

    @Override
    public void submit(FeedbackSubmitRequest request) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "提交内容不能为空");
        }
        if (isBlank(request.topic())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈类型不能为空");
        }
        String topic = request.topic().trim();
        if (!TOPICS.contains(topic)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈类型不合法");
        }
        if (isBlank(request.content())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈内容不能为空");
        }
        String content = request.content().trim();
        if (content.length() > 2000) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "反馈内容过长（最多 2000 字）");
        }
        String rating = trimToNull(request.rating());
        if (rating != null && !RATINGS.contains(rating)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "满意度取值不合法");
        }
        // 与表单 schema（feedback）中的必填项保持一致：称呼 / 类型 / 内容
        if (isBlank(request.name())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请填写您的称呼");
        }
        String name = request.name().trim();
        if (name.length() > 64) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "称呼过长（最多 64 字）");
        }
        String contact = trimToNull(request.contact());
        if (contact != null && contact.length() > 128) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "联系方式过长（最多 128 字）");
        }
        String visitDate = trimToNull(request.visitDate());
        if (visitDate != null && visitDate.length() > 32) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "访问日期格式不正确");
        }

        Feedback feedback = new Feedback();
        feedback.setId(UUID.randomUUID());
        feedback.setName(name);
        feedback.setContact(contact);
        feedback.setTopic(topic);
        feedback.setRating(rating);
        feedback.setContent(content);
        feedback.setVisitDate(visitDate);
        feedback.setCreatedAt(LocalDateTime.now());
        entityQuery.insertable(feedback).executeRows();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
