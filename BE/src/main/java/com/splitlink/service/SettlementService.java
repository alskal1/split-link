package com.splitlink.service;

import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.MemberNetBalanceDto;
import com.splitlink.dto.response.RoomMySettlementResponse;
import com.splitlink.entity.Room;
import com.splitlink.entity.Settlement;
import com.splitlink.mapper.ExpenseMapper;
import com.splitlink.mapper.RoomMapper;
import com.splitlink.mapper.SettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 정산 및 최소 송금 계산 비즈니스 로직을 처리하는 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final RoomMapper roomMapper;
    private final SettlementMapper settlementMapper;
    private final ExpenseMapper expenseMapper;
    private final RoomAccessValidator roomAccessValidator;

    /**
     * 정산 실행 (방 잠금 + 최소 송금 알고리즘 계산 + 정산 내역 일괄 저장)
     *
     * @param slug     방 식별자 (UUID/Slug)
     * @param memberId 현재 JWT 인증된 회원 PK (요청자 검증용)
     */
    @Transactional
    public void executeSettlement(String slug, Long memberId) {

        // 방 접근 권한 및 상태 1차 검증 (안내 문구용)
        Room room = roomAccessValidator.validateAndGetRoom(slug, memberId);
        if (room.isLocked() || room.isClosed()) {
            throw new IllegalArgumentException("이미 정산이 실행되었거나 마감된 방입니다.");
        }

        // 지출 입력 잠금 상태(is_locked = true)로 원자적 업데이트
        int updatedRows = roomMapper.updateRoomLockStatus(room.getRoomId(), true);
        if (updatedRows != 1) {
            throw new IllegalArgumentException("이미 정산이 실행되었거나 마감된 방입니다.");
        }

        // 최소 송금 알고리즘 수행 (지출 내역을 기반으로 Settlements 목록 생성)
        List<Settlement> calculatedSettlements = calculateMinimumTransfers(room.getRoomId());

        // 계산 결과가 존재하면 settlements 테이블에 BATCH INSERT
        if (calculatedSettlements != null && !calculatedSettlements.isEmpty()) {
            settlementMapper.insertSettlements(calculatedSettlements);
        }
    }

    /**
     * 내 정산 내역 조회 (보낼 돈, 받을 돈, 요약 금액 및 송금 리스트)
     *
     * @param slug     방 식별자 (UUID/Slug)
     * @param memberId 현재 접속한 회원 PK
     * @return 내 정산 내역 응답 DTO
     */
    @Transactional(readOnly = true)
    public RoomMySettlementResponse getMySettlement(String slug, Long memberId) {

        // 방 존재 여부 및 접근 권한 검증 -> roomId 반환
        Long roomId = roomAccessValidator.validateAndGetRoomId(slug, memberId);

        // 총 보낼 금액 / 받을 금액 요약 조회
        SettlementMapper.SettlementSummary summary = settlementMapper.findSettlementSummary(roomId, memberId);

        // 보낼 송금 리스트 및 받은 송금 리스트 조회
        List<RoomMySettlementResponse.SendItem> sendList = settlementMapper.findMySendSettlements(roomId, memberId);
        List<RoomMySettlementResponse.ReceiveItem> receiveList = settlementMapper.findMyReceiveSettlements(roomId, memberId);

        // TODO: [딥링크] SendItem 내 송금 딥링크(remittanceLink) 가공 로직 연동 예정
        // sendList.forEach(item -> item.setRemittanceLink(generateLink(item)));

        // 응답 DTO 조립
        return RoomMySettlementResponse.builder()
                .totalSendAmount(summary != null ? summary.getTotalSendAmount() : BigDecimal.ZERO)
                .totalReceiveAmount(summary != null ? summary.getTotalReceiveAmount() : BigDecimal.ZERO)
                .sendList(sendList)
                .receiveList(receiveList)
                .build();
    }

    /**
     * 최소 송금 알고리즘 계산 로직 (Greedy & Two-Pointer)
     */
    private List<Settlement> calculateMinimumTransfers(Long roomId) {

        // DB에서 멤버별 순 상계 금액(netBalance) 조회
        List<MemberNetBalanceDto> netBalances = expenseMapper.findNetBalancesByRoomId(roomId);

        if (netBalances == null || netBalances.isEmpty()) {
            return Collections.emptyList();
        }

        // 채무자(Debtor: < 0)와 채권자(Creditor: > 0) 분리
        List<MemberNetBalanceDto> debtors = new ArrayList<>();
        List<MemberNetBalanceDto> creditors = new ArrayList<>();

        for (MemberNetBalanceDto netBalance : netBalances) {
            if (netBalance.getNetBalance().compareTo(BigDecimal.ZERO) < 0) {
                debtors.add(netBalance);
            } else if (netBalance.getNetBalance().compareTo(BigDecimal.ZERO) > 0) {
                creditors.add(netBalance);
            }
        }

        // 금액 절댓값이 큰 사람부터 우선 상계하기 위해 정렬
        // debtors: netBalance가 가장 음수인 순서 (오름차순 정렬 시 -10000, -5000...)
        debtors.sort(Comparator.comparing(MemberNetBalanceDto::getNetBalance));

        // creditors: netBalance가 가장 양수인 순서 (내림차순 정렬 시 +10000, +5000...)
        creditors.sort((a, b) -> b.getNetBalance().compareTo(a.getNetBalance()));

        List<Settlement> settlements = new ArrayList<>();
        int debtorIdx = 0;
        int creditorIdx = 0;

        // 투 포인터 상계 처리
        while (debtorIdx < debtors.size() && creditorIdx < creditors.size()) {
            MemberNetBalanceDto debtor = debtors.get(debtorIdx);
            MemberNetBalanceDto creditor = creditors.get(creditorIdx);

            BigDecimal sendAmount = debtor.getNetBalance().abs(); // 보낼 돈 (절대값)
            BigDecimal receiveAmount = creditor.getNetBalance(); // 받을 돈

            // 상계할 금액 = min(sendAmount, receiveAmount)
            BigDecimal settlementAmount = sendAmount.min(receiveAmount);

            // Settlement 생성
            Settlement settlement = Settlement.builder()
                    .roomId(roomId)
                    .senderId(debtor.getMemberId())
                    .receiverId(creditor.getMemberId())
                    .amount(settlementAmount)
                    .build();

            settlements.add(settlement);

            // 잔액 갱신 (BigDecimal은 연산 후 새 객체를 반환하므로 set 해줘야함)
            // debtor는 음수이므로 상계 금액을 더해주면 0에 가까워짐 (-30000 + 20000 = -10000)
            debtor.setNetBalance(debtor.getNetBalance().add(settlementAmount));

            // creditor는 양수이므로 상계 금액을 빼주면 0에 가까워짐 (+20000 - 20000 = 0)
            creditor.setNetBalance(creditor.getNetBalance().subtract(settlementAmount));

            // 보낼 금액(sendAmount)만큼 다 털어냈으면 다음 채무자로 이동
            if (sendAmount.compareTo(settlementAmount) == 0) {
                debtorIdx++;
            }

            // 받을 금액(receiveAmount)만큼 다 채웠으면 다음 채권자로 이동
            if (receiveAmount.compareTo(settlementAmount) == 0) {
                creditorIdx++;
            }
        }

        return settlements;
    }
}
