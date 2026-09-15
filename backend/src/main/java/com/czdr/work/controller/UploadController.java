package com.czdr.work.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.czdr.work.comment.resource.Result;
import com.czdr.work.service.RateLimitService;
import com.czdr.work.service.StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 上传接口（需登录，用于讨论区配图）
 *
 * @author cz
 */
@Tag(name = "上传 Upload", description = "用户上传：讨论区配图")
@RestController
@RequestMapping("uploads")
@RequiredArgsConstructor
public class UploadController {

    private final StorageService storageService;
    private final RateLimitService rateLimitService;

    @Operation(summary = "上传图片", description = "支持 JPG/PNG/WebP/GIF，最大 5MB；返回可直接访问的 /uploads/... 地址")
    @PostMapping("image")
    Result<String> uploadImage(@RequestParam("file") MultipartFile file) {
        String userId = StpUtil.getLoginIdAsString();
        rateLimitService.consume("upload", userId, 30, 3600, "上传过于频繁，请稍后再试");
        return Result.success(storageService.storeImage(file));
    }
}
