package com.splitlink.service;

import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.MemberNetBalanceDto;
import com.splitlink.entity.Room;
import com.splitlink.entity.Settlement;
import com.splitlink.mapper.ExpenseMapper;
import com.splitlink.mapper.RoomMapper;
import com.splitlink.mapper.SettlementMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SettlementServiceTest {

    @InjectMocks
    private SettlementService settlementService;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private SettlementMapper settlementMapper;

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private RoomAccessValidator roomAccessValidator;

    @Nested
    @DisplayName("정산 실행 (executeSettlement)")
    class ExecuteSettlementTest {

        private final String slug = "test-room-slug";
        private final Long roomId = 10L;
        private final Long memberId = 1L;

        @Test
        @DisplayName("성공: 최소 송금 내역이 계산되면 방을 잠금 처리하고 settlements에 일괄 저장한다.")
        void executeSettlementSuccess() {
            // given
            Room room = Room.builder()
                    .roomId(roomId)
                    .slug(slug)
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(room);
            given(roomMapper.updateRoomLockStatus(roomId, true)).willReturn(1);

            // A: -30,000원(채무자), B: +10,000원(채권자), C: +20,000원(채권자)
            MemberNetBalanceDto debtorA = new MemberNetBalanceDto(101L, new BigDecimal("-30000"));
            MemberNetBalanceDto creditorB = new MemberNetBalanceDto(102L, new BigDecimal("10000"));
            MemberNetBalanceDto creditorC = new MemberNetBalanceDto(103L, new BigDecimal("20000"));

            given(expenseMapper.findNetBalancesByRoomId(roomId))
                    .willReturn(List.of(debtorA, creditorB, creditorC));

            // when
            settlementService.executeSettlement(slug, memberId);

            // then
            verify(roomAccessValidator).validateAndGetRoom(slug, memberId);
            verify(roomMapper).updateRoomLockStatus(roomId, true);
            verify(expenseMapper).findNetBalancesByRoomId(roomId);

            // BATCH INSERT 파라미터 검증
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Settlement>> captor = ArgumentCaptor.forClass(List.class);
            verify(settlementMapper).insertSettlements(captor.capture());

            List<Settlement> insertedSettlements = captor.getValue();
            assertThat(insertedSettlements).hasSize(2);

            // 1번째 거래: A -> C (20,000원) - 절댓값이 큰 채권자부터 매칭
            assertThat(insertedSettlements.get(0).getSenderId()).isEqualTo(101L);
            assertThat(insertedSettlements.get(0).getReceiverId()).isEqualTo(103L);
            assertThat(insertedSettlements.get(0).getAmount()).isEqualByComparingTo("20000");

            // 2번째 거래: A -> B (10,000원)
            assertThat(insertedSettlements.get(1).getSenderId()).isEqualTo(101L);
            assertThat(insertedSettlements.get(1).getReceiverId()).isEqualTo(102L);
            assertThat(insertedSettlements.get(1).getAmount()).isEqualByComparingTo("10000");
        }

        @Test
        @DisplayName("성공: 지출 내역이 없어 netBalances가 비어있으면 insertSettlements를 호출하지 않는다.")
        void executeSettlementSuccessWithEmptyNetBalances() {
            // given
            Room room = Room.builder()
                    .roomId(roomId)
                    .slug(slug)
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(room);
            given(roomMapper.updateRoomLockStatus(roomId, true)).willReturn(1);
            given(expenseMapper.findNetBalancesByRoomId(roomId)).willReturn(Collections.emptyList());

            // when
            settlementService.executeSettlement(slug, memberId);

            // then
            verify(roomAccessValidator).validateAndGetRoom(slug, memberId);
            verify(roomMapper).updateRoomLockStatus(roomId, true);
            verify(expenseMapper).findNetBalancesByRoomId(roomId);
            verify(settlementMapper, never()).insertSettlements(anyList());
        }

        @Test
        @DisplayName("예외: 이미 isLocked=true 상태인 방은 정산 실행 시 IllegalArgumentException 예외가 발생한다.")
        void executeSettlementThrowExceptionWhenRoomIsLocked() {
            // given
            Room room = Room.builder()
                    .roomId(roomId)
                    .slug(slug)
                    .isLocked(true)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(room);

            // when & then
            assertThatThrownBy(() -> settlementService.executeSettlement(slug, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 정산이 실행되었거나 마감된 방입니다.");

            verify(roomAccessValidator).validateAndGetRoom(slug, memberId);
            verifyNoInteractions(expenseMapper);
            verifyNoInteractions(settlementMapper);
        }

        @Test
        @DisplayName("예외: 이미 isClosed=true 상태인 방은 정산 실행 시 IllegalArgumentException 예외가 발생한다.")
        void executeSettlementThrowExceptionWhenRoomIsClosed() {
            // given
            Room room = Room.builder()
                    .roomId(roomId)
                    .slug(slug)
                    .isLocked(false)
                    .isClosed(true)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(room);

            // when & then
            assertThatThrownBy(() -> settlementService.executeSettlement(slug, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 정산이 실행되었거나 마감된 방입니다.");

            verify(roomAccessValidator).validateAndGetRoom(slug, memberId);
            verifyNoInteractions(expenseMapper);
            verifyNoInteractions(settlementMapper);
        }

        @Test
        @DisplayName("예외: 방 잠금(updateRoomLockStatus) 실패 시 updatedRows가 0이면 IllegalArgumentException 예외가 발생한다.")
        void executeSettlementThrowExceptionWhenLockStatusUpdateFails() {
            // given
            Room room = Room.builder()
                    .roomId(roomId)
                    .slug(slug)
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(room);
            given(roomMapper.updateRoomLockStatus(roomId, true)).willReturn(0); // 0개 행 수정 실패

            // when & then
            assertThatThrownBy(() -> settlementService.executeSettlement(slug, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("지출 입력 잠금 처리에 실패했습니다.");

            verify(roomAccessValidator).validateAndGetRoom(slug, memberId);
            verify(roomMapper).updateRoomLockStatus(roomId, true);
            verifyNoInteractions(expenseMapper);
            verifyNoInteractions(settlementMapper);
        }
    }
}