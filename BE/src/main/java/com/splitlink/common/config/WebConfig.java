package com.splitlink.common.config;

import com.splitlink.common.resolver.AuthMemberArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring MVC Web 설정 클래스
 * Custom ArgumentResolver 및 웹 관련 설정을 등록합니다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    /** JWT 토큰 기반 로그인 멤버 주입용 Resolver */
    private final AuthMemberArgumentResolver authMemberArgumentResolver;

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    /**
     * 컨트롤러 매개변수를 처리할 Custom ArgumentResolver 등록
     *
     * @param resolvers 등록할 HandlerMethodArgumentResolver 목록
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(authMemberArgumentResolver);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
