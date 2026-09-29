package com.splitlink.dto;

import lombok.*;

/**
 * 방 수정 시 해당 방 멤버의 지출 참여 여부 등 정보 조회용 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberUsageDto {
    private Long memberId;
    private String memberName;
    private boolean hasExpenses; // 지출(결제자 또는 1/N 부담자) 참여 여부
}
