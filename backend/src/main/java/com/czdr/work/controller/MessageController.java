package com.czdr.work.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.model.resource.ConversationResource;
import com.czdr.work.model.resource.MessageResource;
import com.czdr.work.service.MessageService;
import com.easy.query.core.api.pagination.EasyPageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * 私信接口（均需登录；仅互相关注的好友之间可会话）
 *
 * @author cz
 */
@Tag(name = "私信 Message", description = "C 端：仅互相关注的好友可私信")
@RestController
@RequestMapping("me")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "获取或创建与某用户的会话", description = "非互相关注返回 1004")
    @PostMapping("conversations/with/{userId}")
    Result<ConversationResource> openConversation(@PathVariable String userId) {
        return Result.success(messageService.openConversation(currentUserId(), UUID.fromString(userId)));
    }

    @Operation(summary = "我的会话列表")
    @GetMapping("conversations")
    Result<EasyPageResult<ConversationResource>> conversations(
            @PageableDefault(page = 0, size = 20) Pageable pageable) {
        return Result.success(messageService.conversations(currentUserId(), pageable));
    }

    @Operation(summary = "会话详情")
    @GetMapping("conversations/{id}")
    Result<ConversationResource> conversation(@PathVariable String id) {
        return Result.success(messageService.conversation(currentUserId(), id));
    }

    @Operation(summary = "会话消息", description = "按时间倒序分页，前端反转后按时间正序展示")
    @GetMapping("conversations/{id}/messages")
    Result<EasyPageResult<MessageResource>> messages(
            @PathVariable String id,
            @PageableDefault(page = 0, size = 30) Pageable pageable) {
        return Result.success(messageService.messages(currentUserId(), id, pageable));
    }

    @Operation(summary = "发送私信", description = "内容与图片至少填一项；图片最多 4 张")
    @PostMapping("conversations/{id}/messages")
    Result<MessageResource> send(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String content = body == null ? null : (String) body.get("content");
        String lang = body == null ? null : (String) body.get("lang");
        List<String> images = List.of();
        Object raw = body == null ? null : body.get("images");
        if (raw instanceof List<?> list) {
            images = list.stream().filter(Objects::nonNull).map(String::valueOf).toList();
        }
        return Result.success(messageService.send(currentUserId(), id, content, lang, images));
    }

    @Operation(summary = "撤回私信", description = "仅发送者本人、2 分钟内可撤回")
    @PostMapping("messages/{id}/recall")
    Result<MessageResource> recall(@PathVariable String id) {
        return Result.success(messageService.recall(currentUserId(), id));
    }

    @Operation(summary = "拉黑 / 解除拉黑", description = "拉黑会解除双方互关，并阻止双方互发私信")
    @PostMapping("blocks/{userId}")
    Result<Boolean> block(@PathVariable String userId, @RequestParam(defaultValue = "true") boolean block) {
        return Result.success(messageService.block(currentUserId(), UUID.fromString(userId), block));
    }

    @Operation(summary = "标记会话已读")
    @PostMapping("conversations/{id}/read")
    Result<Integer> read(@PathVariable String id) {
        return Result.success(messageService.markRead(currentUserId(), id));
    }

    @Operation(summary = "未读私信总数（角标）")
    @GetMapping("conversations/unread-count")
    Result<Long> unread() {
        return Result.success(messageService.unreadTotal(currentUserId()));
    }

    private UUID currentUserId() {
        return UUID.fromString(StpUtil.getLoginIdAsString());
    }
}
