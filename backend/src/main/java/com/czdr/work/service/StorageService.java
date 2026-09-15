package com.czdr.work.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 上传存储（当前仅图片）
 *
 * @author cz
 */
public interface StorageService {

    /** 保存图片并返回可访问 URL（如 /uploads/202609/xxx.webp） */
    String storeImage(MultipartFile file);
}
