package com.splitlink.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * JWT 토큰에서 파싱된 memberId를 컨트롤러 파라미터로 주입받기 위한 어노테이션
 */
@Target(ElementType.PARAMETER) // 오직 메서드의 파라미터 앞에만 붙일 수 있음
@Retention(RetentionPolicy.RUNTIME) // 앱이 실행중일 때까지 어노테이션 정보 유지
public @interface AuthMember { // 마커 어노테이션
}
