package com.splitlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 방 상세 정보 및 멤버 목록 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomDetailResponse {

    /** 방 PK (데이터베이스 고유 식별자) */
    private Long roomId;

    /** 방 고유 식별자 (UUID) */
    private String slug;

    /** 방 제목 */
    private String title;

    /** 기준 통화 (예: KRW, USD) */
    private String baseCurrency;

    /** 등록된 멤버 상세 목록 */
    private List<MemberResponse> members;

    /**
     * 방 상세 정보 응답 용 멤버 단건 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberResponse{

        /** 멤버 PK (데이터베이스 고유 식별자) */
        private Long memberId;

        /** 멤버 이름 */
        private String name;

        /** 방 최초 접속 여부 */
        private boolean isActive;
    }
}
