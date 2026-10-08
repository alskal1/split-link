package com.splitlink.service;

import com.splitlink.common.util.EncryptionUtil;
import com.splitlink.common.util.RemittanceLinkGenerator;
import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.MemberNetBalanceDto;
import com.splitlink.dto.request.RemittanceStatusUpdateRequest;
import com.splitlink.dto.response.RoomMySettlementResponse;
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

    @Mock
    private RemittanceLinkGenerator remittanceLinkGenerator;

    @Mock
    private EncryptionUtil encryptionUtil;

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

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Settlement>> captor = ArgumentCaptor.forClass(List.class);
            verify(settlementMapper).insertSettlements(captor.capture());

            List<Settlement> insertedSettlements = captor.getValue();
            assertThat(insertedSettlements).hasSize(2);
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
            given(roomMapper.updateRoomLockStatus(roomId, true)).willReturn(0);

            // when & then
            assertThatThrownBy(() -> settlementService.executeSettlement(slug, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 정산이 실행되었거나 마감된 방입니다.");
        }
    }

    @Nested
    @DisplayName("내 정산 내역 조회 (getMySettlement)")
    class GetMySettlementTest {

        private final String slug = "test-room-slug";
        private final Long roomId = 10L;
        private final Long memberId = 1L;

        @Test
        @DisplayName("성공: 암호화된 계좌를 복호화하여 DTO 세팅 및 딥링크를 생성한 뒤 정상 반환한다.")
        void getMySettlementSuccess() {
            // given
            String encryptedAccount = "EncryptedAccountText123";
            String decryptedAccount = "3333-12-345678";

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            SettlementMapper.SettlementSummary mockSummary = new SettlementMapper.SettlementSummary(
                    new BigDecimal("15000"),
                    new BigDecimal("30000")
            );
            given(settlementMapper.findSettlementSummary(roomId, memberId)).willReturn(mockSummary);

            RoomMySettlementResponse.SendItem sendItem = RoomMySettlementResponse.SendItem.builder()
                    .settlementId(100L)
                    .receiverId(2L)
                    .receiverName("뚱이")
                    .amount(new BigDecimal("15000"))
                    .bankName("카카오뱅크")
                    .accountNumber(encryptedAccount)
                    .remittanceLink(null)
                    .isDone(false)
                    .build();
            given(settlementMapper.findMySendSettlements(roomId, memberId)).willReturn(List.of(sendItem));

            RoomMySettlementResponse.ReceiveItem receiveItem = RoomMySettlementResponse.ReceiveItem.builder()
                    .settlementId(101L)
                    .senderId(3L)
                    .senderName("징징이")
                    .amount(new BigDecimal("30000"))
                    .isDone(false)
                    .build();
            given(settlementMapper.findMyReceiveSettlements(roomId, memberId)).willReturn(List.of(receiveItem));

            given(encryptionUtil.decrypt(encryptedAccount)).willReturn(decryptedAccount);

            String expectedTossLink = "supertoss://send?bank=%EC%B9%B4%EC%B9%B4%EC%96%B4%EB%B1%8D%ED%81%AC&accountNo=333312345678&amount=15000";
            given(remittanceLinkGenerator.generateTossLink(eq("카카오뱅크"), eq(decryptedAccount), eq(new BigDecimal("15000"))))
                    .willReturn(expectedTossLink);

            // when
            RoomMySettlementResponse response = settlementService.getMySettlement(slug, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalSendAmount()).isEqualByComparingTo("15000");
            assertThat(response.getTotalReceiveAmount()).isEqualByComparingTo("30000");

            // 보낼 내역 검증 (복호화된 계좌번호 및 딥링크 반영 확인)
            assertThat(response.getSendList()).hasSize(1);
            assertThat(response.getSendList().get(0).getReceiverName()).isEqualTo("뚱이");
            assertThat(response.getSendList().get(0).getBankName()).isEqualTo("카카오뱅크");
            assertThat(response.getSendList().get(0).getAccountNumber()).isEqualTo(decryptedAccount);
            assertThat(response.getSendList().get(0).getRemittanceLink()).isEqualTo(expectedTossLink);

            // 받을 내역 검증
            assertThat(response.getReceiveList()).hasSize(1);
            assertThat(response.getReceiveList().get(0).getSenderName()).isEqualTo("징징이");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(settlementMapper).findSettlementSummary(roomId, memberId);
            verify(settlementMapper).findMySendSettlements(roomId, memberId);
            verify(settlementMapper).findMyReceiveSettlements(roomId, memberId);
            verify(encryptionUtil).decrypt(encryptedAccount);
            verify(remittanceLinkGenerator).generateTossLink("카카오뱅크", decryptedAccount, new BigDecimal("15000"));
        }

        @Test
        @DisplayName("성공: 보낼 돈이나 받을 돈이 없는 경우 빈 리스트와 0원을 반환한다.")
        void getMySettlementSuccessWithEmptyList() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            SettlementMapper.SettlementSummary mockSummary = new SettlementMapper.SettlementSummary(
                    BigDecimal.ZERO, BigDecimal.ZERO
            );
            given(settlementMapper.findSettlementSummary(roomId, memberId)).willReturn(mockSummary);
            given(settlementMapper.findMySendSettlements(roomId, memberId)).willReturn(List.of());
            given(settlementMapper.findMyReceiveSettlements(roomId, memberId)).willReturn(List.of());

            // when
            RoomMySettlementResponse response = settlementService.getMySettlement(slug, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getTotalSendAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getTotalReceiveAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getSendList()).isEmpty();
            assertThat(response.getReceiveList()).isEmpty();

            verify(remittanceLinkGenerator, never()).generateTossLink(any(), any(), any());
        }

        @Test
        @DisplayName("예외: 권한이 없거나 존재하지 않는 방인 경우 IllegalArgumentException 예외가 발생한다.")
        void getMySettlementThrowExceptionWhenUnauthorizedOrNotFound() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId))
                    .willThrow(new IllegalArgumentException("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다."));

            // when & then
            assertThatThrownBy(() -> settlementService.getMySettlement(slug, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verifyNoInteractions(settlementMapper);
            verifyNoInteractions(remittanceLinkGenerator);
        }
    }

    @Nested
    @DisplayName("송금 완료 상태 변경 (updateRemittanceStatus)")
    class UpdateRemittanceStatusTest {

        private final String slug = "test-room-slug";
        private final Long roomId = 10L;
        private final Long settlementId = 100L;
        private final Long memberId = 1L;

        @Test
        @DisplayName("성공: 남은 미완료 송금이 존재할 때(remainCount > 0) isClosed = false 로 업데이트된다.")
        void updateRemittanceStatusSuccessWithRemainSettlements() {
            // given
            RemittanceStatusUpdateRequest request = new RemittanceStatusUpdateRequest(true);

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
            given(roomMapper.findRoomByIdForUpdate(roomId)).willReturn(new Room());
            given(settlementMapper.updateRemittanceStatus(roomId, settlementId, memberId, true)).willReturn(1);
            given(settlementMapper.countRemainSettlements(roomId)).willReturn(2);
            given(roomMapper.updateIsClosedByRoomId(roomId, false)).willReturn(1);

            // when
            settlementService.updateRemittanceStatus(slug, settlementId, memberId, request);

            // then
            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomByIdForUpdate(roomId);
            verify(settlementMapper).updateRemittanceStatus(roomId, settlementId, memberId, true);
            verify(settlementMapper).countRemainSettlements(roomId);
            verify(roomMapper).updateIsClosedByRoomId(roomId, false);
        }

        @Test
        @DisplayName("성공: 모든 송금이 완료되었을 때(remainCount == 0) isClosed = true 로 업데이트된다.")
        void updateRemittanceStatusSuccessWithAllCompleted() {
            // given
            RemittanceStatusUpdateRequest request = new RemittanceStatusUpdateRequest(true);

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
            given(roomMapper.findRoomByIdForUpdate(roomId)).willReturn(new Room());
            given(settlementMapper.updateRemittanceStatus(roomId, settlementId, memberId, true)).willReturn(1);
            given(settlementMapper.countRemainSettlements(roomId)).willReturn(0);
            given(roomMapper.updateIsClosedByRoomId(roomId, true)).willReturn(1);

            // when
            settlementService.updateRemittanceStatus(slug, settlementId, memberId, request);

            // then
            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomByIdForUpdate(roomId);
            verify(settlementMapper).updateRemittanceStatus(roomId, settlementId, memberId, true);
            verify(settlementMapper).countRemainSettlements(roomId);
            verify(roomMapper).updateIsClosedByRoomId(roomId, true);
        }

        @Test
        @DisplayName("예외: 존재하지 않거나 권한이 없는 정산 건 수정 시 updatedRows가 0이면 IllegalArgumentException 예외가 발생한다.")
        void updateRemittanceStatusThrowExceptionWhenUpdatedRowsIsZero() {
            // given
            RemittanceStatusUpdateRequest request = new RemittanceStatusUpdateRequest(true);

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
            given(roomMapper.findRoomByIdForUpdate(roomId)).willReturn(new Room());
            given(settlementMapper.updateRemittanceStatus(roomId, settlementId, memberId, true)).willReturn(0);

            // when & then
            assertThatThrownBy(() -> settlementService.updateRemittanceStatus(slug, settlementId, memberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않거나 수정 권한이 없는 정산 내역입니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomByIdForUpdate(roomId);
            verify(settlementMapper).updateRemittanceStatus(roomId, settlementId, memberId, true);
            verify(settlementMapper, never()).countRemainSettlements(anyLong());
            verify(roomMapper, never()).updateIsClosedByRoomId(anyLong(), anyBoolean());
        }
    }
}