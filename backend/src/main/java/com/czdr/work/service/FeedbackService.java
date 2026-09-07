package com.czdr.work.service;

import com.czdr.work.model.request.FeedbackSubmitRequest;

/**
 * 用户反馈（C 端提交）
 *
 * @author cz
 */
public interface FeedbackService {
    /**
     * 提交用户反馈
     */
    void submit(FeedbackSubmitRequest request);
}
