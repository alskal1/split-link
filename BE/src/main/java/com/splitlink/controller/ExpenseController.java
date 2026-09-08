package com.splitlink.controller;

import com.splitlink.common.annotation.AuthMember;
import com.splitlink.common.api.ApiResponse;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.service.ExpenseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 지출(Expense) 관련 API 요청을 처리하는 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/rooms/{slug}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;

    /**
     * 지출 입력 폼 초기 데이터 조회 (계좌 정보 및 방 멤버 목록)
     */
    @GetMapping("/new")
    public ResponseEntity<ApiResponse<ExpenseFormInitResponse>> getExpenseFormInit(
            @PathVariable String slug,
            @AuthMember Long memberId) {
        log.info(">>>> [JWT Auth Success] slug: {}, memberId: {}", slug, memberId);

        ExpenseFormInitResponse response = expenseService.getExpenseFormInit(slug, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
