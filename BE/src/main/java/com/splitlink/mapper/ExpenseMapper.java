package com.splitlink.mapper;

import com.splitlink.entity.Expense;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

/**
 * 지출 및 지출 분할 데이터 영속성 처리를 담당하는 MyBatis Mapper 인터페이스
 */
@Mapper
public interface ExpenseMapper {

    /** 메인 지출 정보 단건 저장 */
    void insertExpense(Expense expense);

    /** 지출 분할 참여자별 부담 금액 목록 일괄 저장 (Bulk Insert) */
    void insertExpenseMembers(@Param("expenseId") Long expenseId,
                              @Param("memberIds") List<Long> memberIds,
                              @Param("amount") BigDecimal amount);
}
