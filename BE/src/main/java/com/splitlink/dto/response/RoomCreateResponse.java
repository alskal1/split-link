package com.splitlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomCreateResponse {
    private String slug;                // 주소 이동 및 식별용
    private String fullUrl;             // 링크 복사 버튼용 전체 URL
    private String title;               // 화면 상단 방 이름 렌더링용
    private String pin;                 // 생성된 방의 핀 번호 확인용
    private List<String> memberNames;   // 방에 포함된 멤버 목록 렌더링용
}
