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
