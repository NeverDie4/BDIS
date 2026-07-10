package com.bdis.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class UploadResourceConfig implements WebMvcConfigurer {

    private final Path uploadRoot;

    public UploadResourceConfig(@Value("${file.upload-path:uploads}") String uploadPath) {
        this.uploadRoot = Path.of(uploadPath).toAbsolutePath().normalize();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/herb/image/**")
                .addResourceLocations(resourceLocation("herb/image"));
        registry.addResourceHandler("/herb/atlas/**")
                .addResourceLocations(resourceLocation("herb/atlas"));
    }

    private String resourceLocation(String relativePath) {
        String location = uploadRoot.resolve(relativePath).toUri().toString();
        return location.endsWith("/") ? location : location + "/";
    }
}
