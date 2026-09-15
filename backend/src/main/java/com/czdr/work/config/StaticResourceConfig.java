package com.czdr.work.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.RequiredArgsConstructor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author cz
 */
@Configuration
@RequiredArgsConstructor
public class StaticResourceConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(StaticResourceConfig.class);

    private final UploadProperties uploadProperties;

    @Value("${app.static-location:}")
    private String configuredStaticLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 用户上传内容（帖子配图等）与内容静态图分开映射
        Path uploadRoot = uploadProperties.root();
        if (Files.isDirectory(uploadRoot)) {
            String location = uploadRoot.toUri().toString();
            if (!location.endsWith("/")) {
                location = location + "/";
            }
            registry.addResourceHandler("/uploads/**").addResourceLocations(location);
            log.info("用户上传资源目录: {}", location);
        }
        String externalLocation = resolveExternalStaticLocation();
        if (externalLocation != null) {
            if (!Files.isDirectory(Paths.get(externalLocation))) {
                log.warn("外部静态资源目录不存在，请确认已把 static 目录放在 jar 同级目录下: {}", externalLocation);
            }
            Path externalRoot = Paths.get(externalLocation);
            if (!"images".equals(externalRoot.getFileName() == null ? "" : externalRoot.getFileName().toString())) {
                externalRoot = externalRoot.resolve("images");
            }
            // 用 Path#toUri 生成规范 file:/// URL（Windows 下形如 file:///C:/...），
            // 手写 "file:" + 路径会产生非层级 URI，导致资源解析异常（图片全部 500）。
            String externalImagesLocation = externalRoot.toAbsolutePath().normalize().toUri().toString();
            if (!externalImagesLocation.endsWith("/")) {
                externalImagesLocation = externalImagesLocation + "/";
            }
            log.info("静态图片资源目录: {} (存在: {})", externalImagesLocation, Files.isDirectory(externalRoot));
            registry.addResourceHandler("/images/**")
                    .addResourceLocations(
                            externalImagesLocation,
                            "classpath:/static/images/"
                    );
        } else {
            registry.addResourceHandler("/images/**")
                    .addResourceLocations("classpath:/static/images/");
        }
    }

    private String resolveExternalStaticLocation() {
        Path jarParent = getJarParent();
        if (configuredStaticLocation != null && !configuredStaticLocation.isBlank()) {
            Path configured = Paths.get(configuredStaticLocation);
            if (configured.isAbsolute()) {
                return configured.normalize().toString();
            }
            if (jarParent != null) {
                return jarParent.resolve(configured).normalize().toString();
            }
            return configured.toAbsolutePath().normalize().toString();
        }
        if (jarParent != null) {
            return jarParent.resolve("static").normalize().toString();
        }
        return null;
    }

    private Path getJarParent() {
        try {
            var codeSource = StaticResourceConfig.class.getProtectionDomain().getCodeSource();
            if (codeSource == null) {
                return null;
            }
            // code source 位置在各种运行方式下形式不同：
            //   file:/app.jar                                  —— 普通 jar
            //   jar:file:/app.jar!/BOOT-INF/classes!/           —— Spring Boot fat jar（旧）
            //   jar:nested:/app.jar/!BOOT-INF/classes!/         —— Spring Boot 3.2+ nested jar
            //   file:/build/classes/java/main/                  —— 开发环境（bootRun / IDE）
            String location = codeSource.getLocation().toString();
            location = location.replaceFirst("^(jar:)?(nested:)?(file:)?", "");
            int bangIndex = location.indexOf('!');
            if (bangIndex >= 0) {
                location = location.substring(0, bangIndex);
            }
            int jarEnd = location.lastIndexOf(".jar");
            if (jarEnd < 0) {
                // 开发环境：classpath 中存在 static，无需外部目录
                return null;
            }
            String jarPath = URLDecoder.decode(location.substring(0, jarEnd + 4), StandardCharsets.UTF_8);
            // Windows 下 URI 形式为 /C:/path/app.jar，需去掉前导斜杠
            if (jarPath.matches("^/[A-Za-z]:/.*")) {
                jarPath = jarPath.substring(1);
            }
            Path jar = Paths.get(jarPath);
            return Files.isRegularFile(jar) ? jar.getParent() : null;
        } catch (Exception e) {
            log.warn("解析 jar 所在目录失败，静态资源将回退到 classpath：{}", e.getMessage());
            return null;
        }
    }
}