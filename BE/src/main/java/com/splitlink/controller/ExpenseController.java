package com.splitlink.controller;

import com.splitlink.common.annotation.AuthMember;
import com.splitlink.common.api.ApiResponse;
import com.splitlink.dto.request.ExpenseBatchCreateRequest;
import com.splitlink.dto.request.ExpenseUpdateRequest;
import com.splitlink.dto.response.ExpenseDetailResponse;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.dto.response.ExpenseListResponse;
import com.splitlink.dto.response.ExpenseUpdateFormResponse;
import com.splitlink.service.ExpenseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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

    /**
     * 지출 내역 일괄 등록
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> createExpenses(
            @PathVariable String slug,
            @AuthMember Long memberId,
            @Valid @RequestBody ExpenseBatchCreateRequest request) {
        log.info(">>>> [JWT Auth Success] slug: {}, memberId: {}", slug, memberId);

        expenseService.createExpenses(slug, memberId, request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(201, null));
    }

    /**
     * 지출 내역 목록 및 정산 요약 정보 조회
     */
    @GetMapping
    public ResponseEntity<ApiResponse<ExpenseListResponse>> getExpenseList(
            @PathVariable String slug,
            @AuthMember Long memberId) {
        log.info(">>>> [JWT Auth Success] slug: {}, memberId: {}", slug, memberId);

        ExpenseListResponse response = expenseService.getExpenseList(slug, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 지출 내역 상세 조회
     */
    @GetMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<ExpenseDetailResponse>> getExpenseDetail(
            @PathVariable String slug,
            @PathVariable Long expenseId,
            @AuthMember Long memberId) {
        log.info(">>>> [JWT Auth Success] slug: {}, memberId: {}", slug, memberId);

        ExpenseDetailResponse response = expenseService.getExpenseDetail(slug, expenseId, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 지출 내역 수정 폼 조회
     */
    @GetMapping("/{expenseId}/edit")
    public ResponseEntity<ApiResponse<ExpenseUpdateFormResponse>> getExpenseUpdateForm(
            @PathVariable String slug,
            @PathVariable Long expenseId,
            @AuthMember Long memberId) {

        log.info(">>>> [JWT Auth Success] slug: {}, expenseId: {}, memberId: {}", slug, expenseId, memberId);

        ExpenseUpdateFormResponse response = expenseService.getExpenseUpdateForm(slug, expenseId, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 지출 내역 수정
     */
    @PutMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> updateExpense(
            @PathVariable String slug,
            @PathVariable Long expenseId,
            @AuthMember Long memberId,
            @Valid @RequestBody ExpenseUpdateRequest request) {

        log.info(">>>> [JWT Auth Success] slug: {}, expenseId: {}, memberId: {}", slug, expenseId, memberId);

        expenseService.updateExpense(slug, expenseId, memberId, request);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 지출 내역 삭제
     */
    @DeleteMapping("/{expenseId}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable String slug,
            @PathVariable Long expenseId,
            @AuthMember Long memberId) {

        log.info(">>>> [JWT Auth Success] slug: {}, expenseId: {}, memberId: {}", slug, expenseId, memberId);

        expenseService.deleteExpense(slug, expenseId, memberId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
