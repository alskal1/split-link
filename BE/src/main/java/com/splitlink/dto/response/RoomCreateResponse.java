package com.splitlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 방 생성 성공 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateResponse {

    /** 방 고유 식별자 (페이지 이동 및 라우팅용 UUID) */
    private String slug;

    /** 방 제목 */
    private String title;

    /** 기준 통화 (예: KRW, USD) */
    private String baseCurrency;

    /** 입장코드 PIN 번호 (생성 결과 확인용) */
    private String pin;

    /** 등록된 멤버 이름 목록 */
    private List<String> memberNames;
}
