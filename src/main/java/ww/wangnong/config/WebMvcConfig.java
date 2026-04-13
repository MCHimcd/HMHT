package ww.wangnong.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.PostConstruct;

/**
 * Purpose: 配置头像上传目录的静态资源映射。
 * Responsibilities: 启动时确保上传目录存在，并将本地文件系统目录暴露到 /uploads/avatars/** 路径。
 * Flow: @PostConstruct 创建目录 -> addResourceHandlers 注册静态映射 -> AuthService
 * 上传后的文件可被前端访问。
 * Dependencies: 文件系统、Spring MVC 资源处理链。
 * Input: hmht.upload.avatar-dir 配置。
 * Output: 可访问的头像静态资源映射。
 * Side Effects: 启动时可能创建本地目录。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    @Value("${hmht.upload.avatar-dir:uploads/avatars}")
    private String avatarDir;

    @PostConstruct
    public void init() {
        Path path = Paths.get(avatarDir);
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
                log.info("Created avatar upload directory: {}", path.toAbsolutePath());
            } catch (IOException e) {
                log.warn("Failed to create avatar upload directory: {}", path.toAbsolutePath(), e);
            }
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path absolutePath = Paths.get(avatarDir).toAbsolutePath();
        String resourceLocation = "file:" + absolutePath.toString().replace("\\", "/") + "/";

        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations(resourceLocation);

        log.info("Configured static resource mapping: /uploads/avatars/** -> {}", resourceLocation);
    }
}
