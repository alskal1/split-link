package com.splitlink.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 방 내 로그인한 사용자 기준 정산 내역 조회 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomMySettlementResponse {

    private BigDecimal totalSendAmount;    // 내가 보낼 총 금액
    private BigDecimal totalReceiveAmount; // 내가 받을 총 금액

    private List<SendItem> sendList;       // 내가 보낼 대상 목록
    private List<ReceiveItem> receiveList; // 내가 받을 대상 목록

    /**
     * 내가 돈을 보내야 하는 대상 정보 (Debtor -> Creditor)
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendItem {
        private Long settlementId;
        private Long receiverId;
        private String receiverName;
        private BigDecimal amount;
        private String bankName;
        private String accountNumber;
        private String remittanceLink; // 딥링크

        @JsonProperty("isDone")
        private boolean isDone;
    }

    /**
     * 내가 돈을 받아야 하는 대상 정보 (Creditor <- Debtor)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReceiveItem {
        private Long settlementId;
        private Long senderId;
        private String senderName;
        private BigDecimal amount;

        @JsonProperty("isDone")
        private boolean isDone;
    }
}
