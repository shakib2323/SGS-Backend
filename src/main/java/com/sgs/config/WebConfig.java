package com.sgs.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    /** Public read endpoints may be cached briefly by browsers/Cloudflare. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if ("GET".equals(request.getMethod())) {
                    response.setHeader("Cache-Control", "public, max-age=60");
                }
                return true;
            }
        }).addPathPatterns("/api/public/**");
    }

    /** Dev convenience. In production Caddy serves /files directly from the uploads volume. */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (props.storage().serveFiles()) {
            String location = Path.of(props.storage().root()).toAbsolutePath().normalize().toUri().toString();
            registry.addResourceHandler("/files/**")
                    .addResourceLocations(location.endsWith("/") ? location : location + "/")
                    .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic());
        }
    }
}
