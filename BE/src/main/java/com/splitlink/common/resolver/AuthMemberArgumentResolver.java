package com.splitlink.common.resolver;

import com.splitlink.common.annotation.AuthMember;
import com.splitlink.common.jwt.JwtProvider;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * JWT 토큰에서 로그인한 멤버 정보(memberId)를 추출하여
 * 컨트롤러의 @AuthMember 어노테이션이 달린 매개변수에 주입하는 Resolver
 */
@Component
@RequiredArgsConstructor
public class AuthMemberArgumentResolver implements HandlerMethodArgumentResolver {

    /** JWT 토큰 파싱 및 검증 전담 컴포넌트 */
    private final JwtProvider jwtProvider;

    /**
     * 바인딩 대상 파라미터 검증
     * @AuthMember 어노테이션이 붙어 있고, 타입이 Long인 경우에만 처리합니다.
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @AuthMember 어노테이션이 붙어있고 Long 타입인 파라미터에만 동작
        return parameter.hasParameterAnnotation(AuthMember.class)
                && Long.class.isAssignableFrom(parameter.getParameterType());
    }

    /**
     * HTTP 요청 헤더의 JWT 토큰에서 memberId를 추출하여 반환
     */
    @Nullable
    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  @Nullable ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  @Nullable WebDataBinderFactory binderFactory) throws Exception {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("HttpServletRequest를 가져올 수 없습니다.");
        }

        String authHeader = request.getHeader("Authorization");

        // 1. 헤더 존재 여부 및 Bearer 타입 검증
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("유효한 Authorization 헤더가 없습니다.");
        }

        String token = authHeader.substring(7);

        // 2. 토큰 서명 및 만료 유효성 검증 (JwtProvider 내validateToken 활용)
        if (!jwtProvider.validateToken(token)) {
            throw new IllegalArgumentException("만료되거나 유효하지 않은 토큰입니다.");
        }

        // 3. 토큰에서 memberId 꺼내서 컨트롤러 파라미터로 주입
        return jwtProvider.getMemberId(token);
    }
}
