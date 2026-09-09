package com.splitlink.service;

import com.splitlink.dto.request.ExpenseBatchCreateRequest;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.entity.Expense;
import com.splitlink.mapper.ExpenseMapper;
import com.splitlink.mapper.MemberMapper;
import com.splitlink.mapper.RoomMapper;
import com.splitlink.mapper.RoomMemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final RoomMapper roomMapper;
    private final ExpenseMapper expenseMapper;

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

        // TODO: [보안] 향후 계좌번호 AES-256 암복호화 유틸리티 적용 예정
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

    /**
     * 지출 내역 일괄 등록 (결제자 계좌 업데이트 + 지출 메인 생성 + 참여자 분할 저장)
     *
     * @param slug     방 식별자 (UUID/Slug)
     * @param memberId 현재 JWT 인증된 사용자 PK (요청자 검증용)
     * @param request  지출 일괄 등록 요청 DTO
     */
    @Transactional
    public void createExpenses(String slug, Long memberId, ExpenseBatchCreateRequest request) {

        // 방 식별자로 방 PK 가져오기
        Long roomId = roomMapper.findRoomIdBySlug(slug);
        if (roomId == null) {
            throw new IllegalArgumentException("해당 방이 없습니다.");
        }

        // TODO: [보안] memberId 기반 현재 요청자가 해당 방의 멤버인지 검증하는 로직 추가 가능

        // 결제자 그룹 단위 처리
        for (ExpenseBatchCreateRequest.ExpenseGroupRequest group : request.getExpenseGroups()) {

            // 결제자 최신 계좌번호 업데이트
            memberMapper.updateAccountInfo(group.getPayerId(), group.getBankName(), group.getAccountNumber());

            // TODO: [환율] 추후 다국어/외화 결제 지원 시 고도화 예정 (현재 원화 KRW 1.0 고정)
            // 통화 및 환율 세팅 (원화 전용)
            String currency = (group.getCurrency() == null) ? "KRW" : group.getCurrency().trim().toUpperCase();
            BigDecimal fxRate = BigDecimal.ONE; // 원화니까 환율은 1.0 고정

            // 결제자 그룹 내 세부 지출 항목 등록
            for (ExpenseBatchCreateRequest.ExpenseItemRequest item : group.getItems()) {

                // Expense 메인 엔티티 생성
                Expense expense = Expense.builder()
                        .roomId(roomId)
                        .payerId(group.getPayerId())
                        .title(item.getTitle())
                        .amount(item.getAmount())
                        .currency(currency)
                        .fxRate(fxRate)
                        .spentAt(group.getSpentAt())
                        .build();

                // 메인 지출 데이터 저장 (MyBatis useGeneratedKeys를 통해 expenseId PK 주입됨)
                expenseMapper.insertExpense(expense);
                Long expenseId = expense.getExpenseId();

                // 1/N 지출 부담 금액 계산 (소수점 첫째자리 반올림)
                int memberCount = item.getTargetMemberIds().size();
                BigDecimal amountPerMember = item.getAmount().divide(
                        new BigDecimal(memberCount), 0, RoundingMode.HALF_UP
                );

                // 지출 부담 참여자 Bulk Insert
                expenseMapper.insertExpenseMembers(expenseId, item.getTargetMemberIds(), amountPerMember);
            }
        }
    }
}
