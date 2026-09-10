package com.splitlink.service;

import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.request.ExpenseBatchCreateRequest;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.entity.Expense;
import com.splitlink.mapper.ExpenseMapper;
import com.splitlink.mapper.MemberMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 지출 관련 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final MemberMapper memberMapper;
    private final ExpenseMapper expenseMapper;
    private final RoomAccessValidator roomAccessValidator;

    /**
     * 지출 입력 폼 초기화에 필요한 데이터 조회 (계좌 정보 + 방 멤버 목록)
     *
     * @param slug     방 식별자
     * @param memberId 현재 접속한 회원 PK
     * @return 지출 폼 초기화 응답 DTO
     */
    @Transactional(readOnly = true)
    public ExpenseFormInitResponse getExpenseFormInit(String slug, Long memberId) {

        roomAccessValidator.validateAndGetRoomId(slug, memberId);

        // 현재 접속한 사용자 존재 여부 확인 및 계좌 정보 조회
        ExpenseFormInitResponse.AccountInfo defaultAccount = memberMapper.findAccountInfoByMemberId(memberId)
                .orElse(null);

        // TODO: [보안] 향후 계좌번호 AES-256 암복호화 유틸리티 적용 예정
        // 회원은 존재하나 계좌 등록을 안한 경우 (bank_name이 null) DTO를 null로 치환하여 전달
        if (defaultAccount != null && defaultAccount.getBankName() == null) {
            defaultAccount = null;
        }

        // 해당 방에 속한 전체 멤버 목록 조회
        List<ExpenseFormInitResponse.MemberInfo> roomMembers = memberMapper.findRoomMembersBySlug(slug, memberId);

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
        Long roomId = roomAccessValidator.validateAndGetRoomId(slug, memberId);

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

                // 오차 보정된 멤버별 부담금 리스트 계산 (별도 메서드 호출)
                List<ExpenseMapper.ExpenseShareParam> shares = calculateShares(
                        expense.getExpenseId(),
                        item.getAmount(),
                        item.getTargetMemberIds()
                );

                // 지출 부담 참여자 Bulk Insert
                expenseMapper.insertExpenseShares(shares);
            }
        }
    }

    /**
     * 1/N 부담금 계산 및 1원 오차 보정 헬퍼 메서드
     * (소수점 버림 후 남은 차액은 첫 번째 참여자에게 가산)
     */
    private List<ExpenseMapper.ExpenseShareParam> calculateShares(Long expenseId, BigDecimal totalAmount, List<Long> memberIds) {
        int memberCount = memberIds.size();

        // 소수점 아래 버림 처리 (예: 10,000 / 3 = 3,333)
        BigDecimal baseAmount = totalAmount.divide(new BigDecimal(memberCount), 0, RoundingMode.DOWN);

        // 남은 오차 금액 계산 (예: 10,000 - (3,333 * 3) = 1)
        BigDecimal remainder = totalAmount.subtract(baseAmount.multiply(new BigDecimal(memberCount)));

        List<ExpenseMapper.ExpenseShareParam> shares = new ArrayList<>();

        for (int i = 0; i < memberCount; i++) {
            Long memberId = memberIds.get(i);

            // 첫 번째 사람(i == 0)에게 남은 오차(1원 등)를 몰아줌
            BigDecimal finalAmount = (i == 0) ? baseAmount.add(remainder) : baseAmount;

            shares.add(new ExpenseMapper.ExpenseShareParam(expenseId, memberId, finalAmount));
        }

        return shares;
    }
}
