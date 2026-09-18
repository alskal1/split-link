package com.splitlink.service;

import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.MemberNetBalanceDto;
import com.splitlink.entity.Room;
import com.splitlink.entity.Settlement;
import com.splitlink.mapper.ExpenseMapper;
import com.splitlink.mapper.RoomMapper;
import com.splitlink.mapper.SettlementMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final RoomMapper roomMapper;
    private final SettlementMapper settlementMapper;
    private final ExpenseMapper expenseMapper;
    private final RoomAccessValidator roomAccessValidator;

    @Transactional
    public void executeSettlement(String slug, Long memberId) {

        // 방 권한 검증 및 Room 엔티티 가져오기
        Room room = roomAccessValidator.validateAndGetRoom(slug, memberId);

        // 이미 잠기거나 마감된 방인지 확인
        if (room.isLocked() || room.isClosed()) {
            throw new IllegalArgumentException("이미 정산이 실행되었거나 마감된 방입니다.");
        }

        // 지출 입력 잠금 상태(is_locked = true)로 업데이트
        int updatedRows = roomMapper.updateRoomLockStatus(room.getRoomId(), true);
        if (updatedRows == 0) {
            throw new IllegalArgumentException("지출 입력 잠금 처리에 실패했습니다.");
        }

        // 최소 송금 알고리즘 수행 (지출 내역을 기반으로 Settlements 목록 생성)
        List<Settlement> calculatedSettlements = calculateMinimumTransfers(room.getRoomId());

        // 계산 결과가 존재하면 settlements 테이블에 BATCH INSERT
        if (calculatedSettlements != null && !calculatedSettlements.isEmpty()) {
            settlementMapper.insertSettlements(calculatedSettlements);
        }
    }

    /**
     * 최소 송금 알고리즘 계산 로직
     */
    private List<Settlement> calculateMinimumTransfers(Long roomId) {
        // TODO: expenses & expense_shares 데이터를 기반으로 순 상계 금액(Net Balance) 계산 후 최소 송금 목록 반환

        // 1. DB에서 멤버별 순 상계 금액(netBalance) 조회
        List<MemberNetBalanceDto> netBalances = expenseMapper.findNetBalancesByRoomId(roomId);

        // 2. 보낼 사람(음수)과 받을 사람(양수) 분리
        // netBalance < 0 -> debtors (채무자)
        // netBalance > 0 -> creditors (채권자)

        // 3. 투 포인터(그리디)로 절댓값이 큰 사람끼리 상계 처리하면서 Settlement 객체 생성
        // senderId, receiverId, min(보낼돈, 받을돈) 짝지어주고 balance 갱신

        return Collections.emptyList();
    }
}
