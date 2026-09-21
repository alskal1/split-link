package com.splitlink.mapper;

import com.splitlink.dto.MemberNetBalanceDto;
import com.splitlink.dto.request.ExpenseUpdateRequest;
import com.splitlink.dto.response.ExpenseDetailResponse;
import com.splitlink.dto.response.ExpenseListResponse;
import com.splitlink.dto.response.ExpenseUpdateFormResponse;
import com.splitlink.entity.Expense;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 지출 및 지출 분할 데이터 영속성 처리를 담당하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface ExpenseMapper {

    /** 메인 지출 정보 단건 저장 */
    void insertExpense(Expense expense);

    /** 개별 금액이 포함된 부담금 Bulk Insert */
    void insertExpenseShares(@Param("shares") List<ExpenseShareParam> shares);

    /** 방 전체 총 지출 금액 합산 */
    BigDecimal findTotalExpenseAmountByRoomId(@Param("roomId") Long roomId);

    /** 하단 지출 목록 조회 */
    List<ExpenseListResponse.ExpenseItemResponse> findExpenseItems(@Param("roomId") Long roomId,
                                                                   @Param("memberId") Long memberId);

    /** 지출 기본 및 결제자 정보 단건 조회 */
    Optional<ExpenseDetailResponse> findExpenseDetailById(@Param("expenseId") Long expenseId,
                                                          @Param("roomId") Long roomId,
                                                          @Param("memberId") Long memberId);

    /** 지출 참여자 목록 및 부담 금액 조회 */
    List<ExpenseDetailResponse.TargetMemberDetail> findExpenseSharesByExpenseId(@Param("expenseId") Long expenseId,
                                                                                @Param("roomId") Long roomId,
                                                                                @Param("memberId") Long memberId);

    /** 지출 수정 폼 기본 데이터 조회 (제목, 금액, 통화, 결제일시, 결제자ID) */
    Optional<ExpenseUpdateFormResponse> findExpenseUpdateFormById(@Param("expenseId") Long expenseId,
                                                                   @Param("roomId") Long roomId);

    /** 지출에 참여 중인 멤버 ID 목록 조회 */
    List<Long> findTargetMemberIdsByExpenseId(@Param("expenseId") Long expenseId);

    /** 방 PK 기준 각 멤버별 순 상계 금액(netAmount = 결제총액 - 부담총액) 조회 */
    List<MemberNetBalanceDto> findNetBalancesByRoomId(@Param("roomId") Long roomId);

    /** 방 PK 기준 지출 건수 조회 */
    int countExpensesByRoomId(@Param("roomId") Long roomId);

    /** 지출 메인 정보 수정 */
    int updateExpense(@Param("expenseId") Long expenseId,
                      @Param("roomId") Long roomId,
                      @Param("payerId") Long payerId,
                      @Param("title") String title,
                      @Param("amount") BigDecimal amount,
                      @Param("spentAt") LocalDateTime spentAt);

    /** 특정 지출의 기존 부담금(expense_shares) 일괄 삭제 */
    int deleteExpenseSharesByExpenseId(@Param("expenseId") Long expenseId);

    /** 지출 ID와 방 ID 조건을 함께 검증하여 삭제 */
    int deleteExpenseById(@Param("expenseId") Long expenseId,
                          @Param("roomId") Long roomId);

    /**
     * 지출 부담금 Bulk Insert 전용 파라미터 전달 DTO
     */
    @Getter
    @AllArgsConstructor
    class ExpenseShareParam {
        private Long expenseId;
        private Long memberId;
        private BigDecimal amount;
    }
}
