package com.jfl.appointment.config;


import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(
            CorsRegistry registry) {

        registry
                .addMapping("/**")
                .allowedOriginPatterns("https://*.trycloudflare.com")
                .allowedOrigins(
                        "http://host.docker.internal:3000",
                        "http://app.holamd.app",
                        "https://app.holamd.app/",
                        "https://n8n.holamd.app/",
                        "http://n8n.holamd.app/",
                        "http://localhost:3000",
                        "https://localhost:5173",
                        "http://localhost:5500",
                        "http://127.0.0.1:5500",
                        "https://chetan0003.github.io"
                )
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )
                .allowedHeaders("*")
                .allowCredentials(false)
                .maxAge(3600);
    }
}