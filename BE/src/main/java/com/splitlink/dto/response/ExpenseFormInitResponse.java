package com.splitlink.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 지출 입력 폼 초기화 정보 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseFormInitResponse {

    /** 현재 접속한 멤버 PK (기본 결제자 지정용) */
    private Long currentMemberId;

    // 결제일자 - 클라이언트 기기 기준의 날짜를 프론트에서 사용할 것

    /** 현재 멤버의 정산용 계좌 정보 (미등록 시 null) */
    private AccountInfo defaultAccount;

    /** 방 내 전체 참여 멤버 목록 (미입장자 포함 전원 반환) */
    private List<MemberInfo> roomMembers;

    /**
     * 정산 계좌 정보 응답 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountInfo {

        /** 은행명 (예: 카카오뱅크, 신한은행) */
        private String bankName;

        /** 계좌번호 */
        private String accountNumber;
    }

    /**
     * 지출 입력 폼용 멤버 단건 DTO
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MemberInfo {

        /** 멤버 PK */
        private Long memberId;

        /** 멤버 이름 */
        private String name;

        /** 방 진입 완료 여부 (UI '미입장' 표시용) */
        private boolean isActive;

        /** 현재 로그인한 본인 여부 (UI '나' 표시용) */
        private boolean isSelf;
    }
}
