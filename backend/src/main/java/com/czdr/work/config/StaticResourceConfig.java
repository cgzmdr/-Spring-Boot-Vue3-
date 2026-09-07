package com.czdr.work.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author cz
 */
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(StaticResourceConfig.class);

    @Value("${app.static-location:}")
    private String configuredStaticLocation;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String externalLocation = resolveExternalStaticLocation();
        if (externalLocation != null) {
            if (!Files.isDirectory(Paths.get(externalLocation))) {
                log.warn("外部静态资源目录不存在，请确认已把 static 目录放在 jar 同级目录下: {}", externalLocation);
            }
            Path externalRoot = Paths.get(externalLocation);
            if (!"images".equals(externalRoot.getFileName() == null ? "" : externalRoot.getFileName().toString())) {
                externalRoot = externalRoot.resolve("images");
            }
            String externalImagesLocation = externalRoot.toString();
            registry.addResourceHandler("/images/**")
                    .addResourceLocations(
                            "file:" + toFileUrlPath(externalImagesLocation) + "/",
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
            String location = codeSource.getLocation().toString();
            if (location.startsWith("jar:")) {
                location = location.substring(4);
            }
            int bangIndex = location.indexOf('!');
            if (bangIndex >= 0) {
                location = location.substring(0, bangIndex);
            }
            URI uri = URI.create(location);
            Path path = Paths.get(uri);
            if (Files.isDirectory(path)) {
                return null;
            }
            return path.getParent();
        } catch (Exception e) {
            return null;
        }
    }

    private String toFileUrlPath(String path) {
        return path.replace('\\', '/');
    }
}
