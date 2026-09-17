package com.splitlink.mapper;

import com.splitlink.dto.response.ExpenseDetailResponse;
import com.splitlink.dto.response.ExpenseListResponse;
import com.splitlink.entity.Expense;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
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
    /** 방 PK 기준 지출 건수 조회 */
    int countExpensesByRoomId(@Param("roomId") Long roomId);

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
