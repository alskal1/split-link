package com.splitlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 멤버 선택 및 방 진입 성공 시 반환되는 응답 DTO
 */
@Getter
@AllArgsConstructor
public class SelectMemberResponse {

    /** 방 접근 권한 인증용 JWT (프론트엔드 저장용) */
    private String accessToken;

    private Long roomId;
    private String slug;
    private String title;
    private Long memberId;
    private String memberName;

    /** 멤버의 방 진입 활성화 여부 */
    private boolean isActive;
}
