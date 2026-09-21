package com.splitlink.controller;

import com.splitlink.common.annotation.AuthMember;
import com.splitlink.common.api.ApiResponse;
import com.splitlink.service.SettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/rooms/{slug}/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;

    /**
     * 정산 실행
     * (방 잠금 + 최소 송금 알고리즘 1회 계산 + DB 결과 저장)
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> executeSettlement(
            @PathVariable String slug,
            @AuthMember Long memberId) {

        log.info(">>>> [Settlement Execution] slug: {}, memberId: {}", slug, memberId);

        settlementService.executeSettlement(slug, memberId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
