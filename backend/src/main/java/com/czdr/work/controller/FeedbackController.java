package com.czdr.work.controller;

import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.request.FeedbackSubmitRequest;
import com.czdr.work.service.FeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户反馈公开接口（C 端首页浮动按钮提交，无需登录）
 *
 * @author cz
 */
@Tag(name = "用户反馈 Feedback", description = "C 端用户反馈提交（无需登录）")
@RestController
@RequestMapping("feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * 提交用户反馈
     */
    @Operation(summary = "提交反馈", description = "提交首页反馈表单（称呼/联系方式/类型/满意度/内容/访问日期），无需登录")
    @PostMapping
    Result<Void> submit(@RequestBody FeedbackSubmitRequest request) {
        feedbackService.submit(request);
        return Result.success(null);
    }
}
