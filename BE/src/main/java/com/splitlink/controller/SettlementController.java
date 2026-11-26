package com.splitlink.controller;

import com.splitlink.common.annotation.AuthMember;
import com.splitlink.common.api.ApiResponse;
import com.splitlink.dto.request.RemittanceStatusUpdateRequest;
import com.splitlink.dto.response.RoomMySettlementResponse;
import com.splitlink.service.SettlementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

        settlementService.executeSettlement(slug, memberId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /**
     * 내 정산 내역 조회
     * (내가 보낼 돈/계좌/딥링크 목록 + 내가 받을 돈 목록 + 총액)
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<RoomMySettlementResponse>> getMySettlement(
            @PathVariable String slug,
            @AuthMember Long memberId) {

        RoomMySettlementResponse response = settlementService.getMySettlement(slug, memberId);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 개별 송금 완료 상태 변경
     */
    @PatchMapping("/{settlementId}")
    public ResponseEntity<ApiResponse<Void>> updateRemittanceStatus(
            @PathVariable String slug,
            @PathVariable Long settlementId,
            @AuthMember Long memberId,
            @Valid @RequestBody RemittanceStatusUpdateRequest request) {

        settlementService.updateRemittanceStatus(slug, settlementId, memberId, request);

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
