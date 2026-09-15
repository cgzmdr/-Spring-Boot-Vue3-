package com.czdr.work.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 用户上传文件的存放目录（帖子配图等）。
 * 与「内容静态图（static/images）」分离：内容图随发行包提供，上传图是运行期数据。
 *
 * @author cz
 */
@Slf4j
@Component
public class UploadProperties {

    /** 相对路径按应用工作目录解析；可用 APP_UPLOAD_LOCATION 覆盖为绝对路径 */
    @Value("${app.upload.location:./uploads}")
    private String location;

    private Path root;

    @PostConstruct
    void init() {
        Path configured = Paths.get(location);
        root = configured.isAbsolute()
                ? configured.normalize()
                : configured.toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("用户上传目录: {} (存在: {})", root, Files.isDirectory(root));
        } catch (IOException e) {
            log.warn("创建上传目录失败（{}）：{}", root, e.getMessage());
        }
    }

    /** 上传根目录（绝对路径，已确保存在） */
    public Path root() {
        if (root == null) {
            init();
        }
        return root;
    }

    /** 静态访问前缀：/uploads/** */
    public String urlPrefix() {
        return "/uploads/";
    }
}
