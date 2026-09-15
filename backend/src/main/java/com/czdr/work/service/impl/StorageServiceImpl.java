package com.czdr.work.service.impl;

import com.czdr.work.comment.exception.BusinessException;
import com.czdr.work.comment.exception.ErrorCode;
import com.czdr.work.config.UploadProperties;
import com.czdr.work.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 本地磁盘图片存储：
 * · 类型白名单（jpg/png/webp/gif）+ 文件头魔数校验 + 大小上限，避免伪装文件；
 * · 文件名使用 UUID，按月分目录；返回 /uploads/... 由静态资源映射对外提供。
 *
 * @author cz
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

    private static final long MAX_SIZE = 5 * 1024 * 1024L;
    private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("yyyyMM");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/webp", ".webp",
            "image/gif", ".gif"
    );

    private final UploadProperties uploadProperties;

    @Override
    public String storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片过大（最大 5MB）");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            // 兜底：按扩展名判断
            String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
            extension = EXTENSIONS.entrySet().stream()
                    .filter(e -> name.endsWith(e.getValue()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        if (extension == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "仅支持 JPG / PNG / WebP / GIF 图片");
        }
        verifyMagic(file, extension);

        String month = LocalDate.now().format(MONTH);
        Path dir = uploadProperties.root().resolve(month);
        String filename = UUID.randomUUID().toString().replace("-", "") + extension;
        try {
            Files.createDirectories(dir);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("保存上传图片失败", e);
            throw new BusinessException(ErrorCode.SERVER_ERROR, "图片保存失败，请稍后重试");
        }
        return uploadProperties.urlPrefix() + month + "/" + filename;
    }

    /** 文件头校验：拒绝改名的非图片文件 */
    private void verifyMagic(MultipartFile file, String extension) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            if (head.length < 4) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "图片内容不完整");
            }
            boolean ok = switch (extension) {
                case ".jpg" -> (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8;
                case ".png" -> (head[0] & 0xFF) == 0x89 && head[1] == 'P' && head[2] == 'N' && head[3] == 'G';
                case ".gif" -> head[0] == 'G' && head[1] == 'I' && head[2] == 'F';
                case ".webp" -> head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
                        && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P';
                default -> false;
            };
            if (!ok) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "图片格式校验失败");
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "图片读取失败");
        }
    }
}
