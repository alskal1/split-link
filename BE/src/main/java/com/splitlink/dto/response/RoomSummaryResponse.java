package com.splitlink.dto.response;

import lombok.*;

import java.util.List;

/**
 * 입장코드 입력 페이지용 방 요약 정보 응답 DTO
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSummaryResponse {

    /** 방 제목 */
    private String title;

    /** 참여 멤버 수 (Service 계층에서 목록 크기로 산출하여 세팅) */
    private int memberCount;

    /** 참여 멤버 이름 목록 */
    private List<String> memberNames;
}
