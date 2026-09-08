package com.splitlink.service;

import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.mapper.MemberMapper;
import com.splitlink.mapper.RoomMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 지출 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final MemberMapper memberMapper;
    private final RoomMemberMapper roomMemberMapper;

    /**
     * 지출 입력 폼 초기화에 필요한 데이터 조회 (계좌 정보 + 방 멤버 목록)
     *
     * @param slug     방 식별자
     * @param memberId 현재 접속한 회원 PK
     * @return 지출 폼 초기화 응답 DTO
     */
    @Transactional(readOnly = true)
    public ExpenseFormInitResponse getExpenseFormInit(String slug, Long memberId) {

        // 현재 접속한 사용자 존재 여부 확인 및 계좌 정보 조회
        ExpenseFormInitResponse.AccountInfo defaultAccount = memberMapper.findAccountInfoByMemberId(memberId)
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버가 없습니다."));

        // 회원은 존재하나 계좌 등록을 안한 경우 (bank_name이 null) DTO를 null로 치환하여 전달
        if (defaultAccount.getBankName() == null) {
            defaultAccount = null;
        }

        // 해당 방에 속한 전체 멤버 목록 조회
        List<ExpenseFormInitResponse.MemberInfo> roomMembers = roomMemberMapper.findRoomMembersBySlug(slug, memberId);

        // 최종 DTO 생성 및 반환
        return ExpenseFormInitResponse.builder()
                .currentMemberId(memberId)
                .defaultAccount(defaultAccount)
                .roomMembers(roomMembers)
                .build();
    }
}
