package com.example.enterprise.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;

/**
 * Web 配置: 跨域 + 静态资源映射
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Resource
    private EnterpriseProperties properties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 将上传目录映射到 /files/** 便于前端预览合并后的 Excel
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + properties.getUploadDir() + "/");
    }
}

