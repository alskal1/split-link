package com.splitlink.service;

import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.request.ExpenseBatchCreateRequest;
import com.splitlink.dto.response.ExpenseDetailResponse;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.dto.response.ExpenseListResponse;
import com.splitlink.mapper.ExpenseMapper;
import com.splitlink.mapper.MemberMapper;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class ExpenseServiceTest {

    @InjectMocks
    private ExpenseService expenseService;

    @Mock
    private MemberMapper memberMapper;

    @Mock
    private ExpenseMapper expenseMapper;

    @Mock
    private RoomMapper roomMapper;

    @Mock
    private SettlementMapper settlementMapper;

    @Mock
    private RoomAccessValidator roomAccessValidator;

    @Test
    @DisplayName("성공: 등록된 계좌가 있는 회원인 경우 계좌 정보와 방 멤버 목록을 정상 반환한다.")
    void getExpenseFormInitSuccessWithAccount() {
        // given
        String slug = "test-room-slug";
        Long memberId = 1L;
        Long roomId = 10L;

        ExpenseFormInitResponse.AccountInfo accountInfo = ExpenseFormInitResponse.AccountInfo.builder()
                .bankName("카카오뱅크")
                .accountNumber("3333-12-345678")
                .build();

        ExpenseFormInitResponse.MemberInfo member1 = ExpenseFormInitResponse.MemberInfo.builder()
                .memberId(1L).name("스폰지밥").isActive(true).isSelf(true).build();
        ExpenseFormInitResponse.MemberInfo member2 = ExpenseFormInitResponse.MemberInfo.builder()
                .memberId(2L).name("뚱이").isActive(false).isSelf(false).build();

        given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
        given(memberMapper.findAccountInfoByMemberId(memberId)).willReturn(Optional.of(accountInfo));
        given(memberMapper.findRoomMembersBySlug(slug, memberId)).willReturn(List.of(member1, member2));

        // when
        ExpenseFormInitResponse response = expenseService.getExpenseFormInit(slug, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getCurrentMemberId()).isEqualTo(memberId);

        // 계좌 검증
        assertThat(response.getDefaultAccount()).isNotNull();
        assertThat(response.getDefaultAccount().getBankName()).isEqualTo("카카오뱅크");

        // 멤버 목록 검증
        assertThat(response.getRoomMembers()).hasSize(2);
        assertThat(response.getRoomMembers().get(0).isSelf()).isTrue();
        assertThat(response.getRoomMembers().get(0).isActive()).isTrue();
        assertThat(response.getRoomMembers().get(1).isSelf()).isFalse();
        assertThat(response.getRoomMembers().get(1).isActive()).isFalse();

        verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
        verify(memberMapper).findAccountInfoByMemberId(memberId);
        verify(memberMapper).findRoomMembersBySlug(slug, memberId);
    }

    @Test
    @DisplayName("성공: DB에 계좌 정보(Member)가 아예 없는 경우(Optional.empty)에도 defaultAccount는 null을 반환한다.")
    void getExpenseFormInitSuccessNoAccount() {
        // given
        String slug = "test-room-slug";
        Long memberId = 1L;
        Long roomId = 10L;

        // Validator는 정상 통과하지만, 계좌/회원 조회 결과가 Optional.empty인 상황
        given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
        given(memberMapper.findAccountInfoByMemberId(memberId)).willReturn(Optional.empty());
        given(memberMapper.findRoomMembersBySlug(slug, memberId)).willReturn(List.of());

        // when
        ExpenseFormInitResponse response = expenseService.getExpenseFormInit(slug, memberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getDefaultAccount()).isNull(); // 예외가 발생하는 대신 null이어야 함!

        verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
        verify(memberMapper).findAccountInfoByMemberId(memberId);
    }

    @Test
    @DisplayName("예외: 존재하지 않는 회원 ID로 요청 시 IllegalArgumentException 예외가 발생한다.")
    void getExpenseFormInitThrowExceptionWhenMemberNotFound() {
        // given
        String slug = "test-room-slug";
        Long invalidMemberId = 999L;

        // RoomAccessValidator에서 예외가 발생하는 상황 모킹
        given(roomAccessValidator.validateAndGetRoomId(slug, invalidMemberId))
                .willThrow(new IllegalArgumentException("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다."));

        // when & then
        assertThatThrownBy(() -> expenseService.getExpenseFormInit(slug, invalidMemberId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다.");
    }

    @Test
    @DisplayName("성공: 1/N 정산 시 소수점 버림 후 남은 1원 오차가 첫 번째 참여자에게 정상 가산된다.")
    void createExpensesRemainderAddedToFirstMember() {
        // given
        String slug = "test-slug";
        Long currentMemberId = 1L;
        Long roomId = 10L;

        // 10,000원을 3명이 분할 (3,333원 * 3 = 9,999원 -> 오차 1원 발생)
        ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                .title("저녁 식사")
                .amount(new BigDecimal("10000"))
                .targetMemberIds(List.of(1L, 2L, 3L))
                .build();

        ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                .payerId(1L)
                .spentAt(LocalDateTime.now())
                .currency("KRW")
                .bankName("카카오뱅크")
                .accountNumber("3333-12-345678")
                .items(List.of(item))
                .build();

        ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                .expenseGroups(List.of(group))
                .build();

        given(roomAccessValidator.validateAndGetRoomId(slug, currentMemberId)).willReturn(roomId);

        // when
        expenseService.createExpenses(slug, currentMemberId, request);

        // then
        // insertExpenseShares 메서드로 넘어간 파라미터 캡처
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ExpenseMapper.ExpenseShareParam>> captor = ArgumentCaptor.forClass(List.class);
        verify(expenseMapper).insertExpenseShares(captor.capture());

        List<ExpenseMapper.ExpenseShareParam> shares = captor.getValue();

        // 3명에게 분할된 금액 검증
        assertThat(shares).hasSize(3);
        assertThat(shares.get(0).getAmount()).isEqualTo(new BigDecimal("3334")); // 오차 1원 추가됨 (3,333 + 1)
        assertThat(shares.get(1).getAmount()).isEqualTo(new BigDecimal("3333"));
        assertThat(shares.get(2).getAmount()).isEqualTo(new BigDecimal("3333"));
    }

    @Test
    @DisplayName("예외: 요청 바디의 payerId 또는 targetMemberIds 중 방 소속이 아닌 멤버가 있으면 예외가 발생한다.")
    void createExpensesThrowExceptionWhenMemberNotInRoom() {
        // given
        String slug = "test-slug";
        Long currentMemberId = 1L;
        Long roomId = 10L;
        Long outsideMemberId = 999L; // 다른 방 멤버 (공격 시도)

        ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                .title("저녁 식사")
                .amount(new BigDecimal("10000"))
                .targetMemberIds(List.of(1L, outsideMemberId)) // 방 밖 멤버 포함
                .build();

        ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                .payerId(1L)
                .spentAt(LocalDateTime.now())
                .currency("KRW")
                .bankName("카카오뱅크")
                .accountNumber("3333-12-345678")
                .items(List.of(item))
                .build();

        ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                .expenseGroups(List.of(group))
                .build();

        given(roomAccessValidator.validateAndGetRoomId(slug, currentMemberId)).willReturn(roomId);

        // RoomAccessValidator에서 검증 실패 예외를 던지도록 모킹
        doThrow(new IllegalArgumentException("해당 방에 속하지 않은 참여자가 포함되어 있습니다."))
                .when(roomAccessValidator).validateMembersInRoom(eq(roomId), anyList());

        // when & then
        assertThatThrownBy(() -> expenseService.createExpenses(slug, currentMemberId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 방에 속하지 않은 참여자가 포함되어 있습니다.");
    }

    @Test
    @DisplayName("성공: 지출 목록 조회 시 방 정보, 총액 및 isMyPayment, targetMemberCount가 포함된 목록을 반환한다.")
    void getExpenseListSuccess() {
        // given
        String slug = "test-room-slug";
        Long currentMemberId = 1L;
        Long roomId = 10L;

        // 1. roomMapper가 반환할 HeaderData 모킹 준비
        RoomMapper.ExpenseListHeaderData headerData = RoomMapper.ExpenseListHeaderData.builder()
                .roomTitle("일본 여행 정산방")
                .memberName("스펀지밥")
                .isLocked(false)
                .build();

        // 2. 지출 아이템 모킹 데이터 준비
        ExpenseListResponse.ExpenseItemResponse item1 = ExpenseListResponse.ExpenseItemResponse.builder()
                .expenseId(3L)
                .title("후식 메론")
                .amount(new BigDecimal("15000"))
                .payerName("스펀지밥")
                .targetMemberCount(2)
                .isMyPayment(true)
                .build();

        ExpenseListResponse.ExpenseItemResponse item2 = ExpenseListResponse.ExpenseItemResponse.builder()
                .expenseId(2L)
                .title("점심 돈까스")
                .amount(new BigDecimal("30000"))
                .payerName("스펀지밥")
                .targetMemberCount(2)
                .isMyPayment(true)
                .build();

        given(roomAccessValidator.validateAndGetRoomId(slug, currentMemberId)).willReturn(roomId);

        given(roomMapper.getExpenseListHeaderData(roomId, currentMemberId)).willReturn(headerData);

        given(expenseMapper.findTotalExpenseAmountByRoomId(roomId)).willReturn(new BigDecimal("45000"));
        given(expenseMapper.findExpenseItems(roomId, currentMemberId)).willReturn(List.of(item1, item2));

        // when
        ExpenseListResponse response = expenseService.getExpenseList(slug, currentMemberId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getRoomTitle()).isEqualTo("일본 여행 정산방");
        assertThat(response.getCurrentMemberName()).isEqualTo("스펀지밥");
        assertThat(response.getTotalExpenseAmount()).isEqualByComparingTo(new BigDecimal("45000"));
        assertThat(response.isLocked()).isFalse();

        // 지출 목록 검증
        assertThat(response.getExpenses()).hasSize(2);
        assertThat(response.getExpenses().get(0).getExpenseId()).isEqualTo(3L);
        assertThat(response.getExpenses().get(0).getTitle()).isEqualTo("후식 메론");
        assertThat(response.getExpenses().get(0).isMyPayment()).isTrue();
        assertThat(response.getExpenses().get(0).getTargetMemberCount()).isEqualTo(2);

        // verify
        verify(roomAccessValidator).validateAndGetRoomId(slug, currentMemberId);
        verify(roomMapper).getExpenseListHeaderData(roomId, currentMemberId);
        verify(expenseMapper).findTotalExpenseAmountByRoomId(roomId);
        verify(expenseMapper).findExpenseItems(roomId, currentMemberId);
    }

    @Test
    @DisplayName("성공: isLocked=true일 때 보낼 금액이 존재하면 SEND 상태와 금액을 반환한다")
    void getExpenseListSettlementSendStatusTest() {
        // given
        String slug = "test-slug";
        Long roomId = 1L;
        Long memberId = 10L;

        given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

        // isLocked = true 상태 모킹
        RoomMapper.ExpenseListHeaderData headerData = new RoomMapper.ExpenseListHeaderData("테스트방", "기영", true);
        given(roomMapper.getExpenseListHeaderData(roomId, memberId)).willReturn(headerData);

        given(expenseMapper.findTotalExpenseAmountByRoomId(roomId)).willReturn(new BigDecimal("30000"));

        // 보낼 금액(Send) 5,000원, 받을 금액(Receive) 0원 모킹
        SettlementMapper.SettlementSummary summary = new SettlementMapper.SettlementSummary(new BigDecimal("5000"), BigDecimal.ZERO);
        given(settlementMapper.findSettlementSummary(roomId, memberId)).willReturn(summary);

        given(expenseMapper.findExpenseItems(roomId, memberId)).willReturn(List.of());

        // when
        ExpenseListResponse response = expenseService.getExpenseList(slug, memberId);

        // then
        assertThat(response.getSettlementStatus()).isEqualTo(ExpenseListResponse.SettlementStatus.SEND);
        assertThat(response.getMySettlementAmount()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("성공: isLocked=true일 때 받을 금액이 존재하면 RECEIVE 상태와 금액을 반환한다")
    void getExpenseListSettlementReceiveStatusTest() {
        // given
        String slug = "test-slug";
        Long roomId = 1L;
        Long memberId = 20L;

        given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

        RoomMapper.ExpenseListHeaderData headerData = new RoomMapper.ExpenseListHeaderData("테스트방", "기철", true);
        given(roomMapper.getExpenseListHeaderData(roomId, memberId)).willReturn(headerData);

        given(expenseMapper.findTotalExpenseAmountByRoomId(roomId)).willReturn(new BigDecimal("30000"));

        // 보낼 금액 0원, 받을 금액 5,000원 모킹
        SettlementMapper.SettlementSummary summary = new SettlementMapper.SettlementSummary(BigDecimal.ZERO, new BigDecimal("5000"));
        given(settlementMapper.findSettlementSummary(roomId, memberId)).willReturn(summary);

        given(expenseMapper.findExpenseItems(roomId, memberId)).willReturn(List.of());

        // when
        ExpenseListResponse response = expenseService.getExpenseList(slug, memberId);

        // then
        assertThat(response.getSettlementStatus()).isEqualTo(ExpenseListResponse.SettlementStatus.RECEIVE);
        assertThat(response.getMySettlementAmount()).isEqualByComparingTo(new BigDecimal("5000"));
    }

    @Test
    @DisplayName("성공: isLocked=true일 때 보낼 금액과 받을 금액이 모두 0원이면 ZERO 상태를 반환한다")
    void getExpenseListSettlementZeroStatusTest() {
        // given
        String slug = "test-slug";
        Long roomId = 1L;
        Long memberId = 30L;

        given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

        RoomMapper.ExpenseListHeaderData headerData = new RoomMapper.ExpenseListHeaderData("테스트방", "오덕", true);
        given(roomMapper.getExpenseListHeaderData(roomId, memberId)).willReturn(headerData);

        given(expenseMapper.findTotalExpenseAmountByRoomId(roomId)).willReturn(new BigDecimal("30000"));

        // 보낼 금액 0원, 받을 금액 0원 모킹
        SettlementMapper.SettlementSummary summary = new SettlementMapper.SettlementSummary(BigDecimal.ZERO, BigDecimal.ZERO);
        given(settlementMapper.findSettlementSummary(roomId, memberId)).willReturn(summary);

        given(expenseMapper.findExpenseItems(roomId, memberId)).willReturn(List.of());

        // when
        ExpenseListResponse response = expenseService.getExpenseList(slug, memberId);

        // then
        assertThat(response.getSettlementStatus()).isEqualTo(ExpenseListResponse.SettlementStatus.ZERO);
        assertThat(response.getMySettlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Nested
    @DisplayName("지출 상세 조회 (getExpenseDetail)")
    class GetExpenseDetailTest {

        private final String slug = "82f31815-3763-4648-8245-d7c5dcd90a24";
        private final Long roomId = 1L;
        private final Long expenseId = 2L;
        private final Long memberId = 38L; // 스펀지밥 (현재 접속자)

        @Test
        @DisplayName("성공: 지출 상세 정보 및 참여자별 부담금 정보를 정상 조회한다.")
        void getExpenseDetail_Success() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            ExpenseDetailResponse mockDetail = ExpenseDetailResponse.builder()
                    .expenseId(expenseId)
                    .title("점심 돈까스")
                    .amount(new BigDecimal("30000.00"))
                    .currency("KRW")
                    .fxRate(BigDecimal.ONE)
                    .spentAt(LocalDateTime.of(2026, 9, 9, 18, 30))
                    .payerId(38L)
                    .payerName("스펀지밥")
                    .bankName("카카오뱅크")
                    .accountNumber("3333-12-3456789")
                    .isMyPayment(true)
                    .build();

            List<ExpenseDetailResponse.TargetMemberDetail> mockTargetMembers = List.of(
                    ExpenseDetailResponse.TargetMemberDetail.builder()
                            .memberId(38L)
                            .name("스펀지밥")
                            .shareAmount(new BigDecimal("15000.00"))
                            .isSelf(true)
                            .build(),
                    ExpenseDetailResponse.TargetMemberDetail.builder()
                            .memberId(39L)
                            .name("다람이")
                            .shareAmount(new BigDecimal("15000.00"))
                            .isSelf(false)
                            .build()
            );

            given(expenseMapper.findExpenseDetailById(expenseId, roomId, memberId))
                    .willReturn(Optional.of(mockDetail));
            given(expenseMapper.findExpenseSharesByExpenseId(expenseId, roomId, memberId))
                    .willReturn(mockTargetMembers);

            // when
            ExpenseDetailResponse result = expenseService.getExpenseDetail(slug, expenseId, memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getExpenseId()).isEqualTo(expenseId);
            assertThat(result.getTitle()).isEqualTo("점심 돈까스");
            assertThat(result.getAmount()).isEqualByComparingTo("30000.00");
            assertThat(result.getPayerName()).isEqualTo("스펀지밥");
            assertThat(result.getBankName()).isEqualTo("카카오뱅크");
            assertThat(result.isMyPayment()).isTrue();

            // 참여자 목록 검증
            assertThat(result.getTargetMembers()).hasSize(2);
            assertThat(result.getTargetMembers().get(0).getName()).isEqualTo("스펀지밥");
            assertThat(result.getTargetMembers().get(0).isSelf()).isTrue();
            assertThat(result.getTargetMembers().get(1).getName()).isEqualTo("다람이");
            assertThat(result.getTargetMembers().get(1).isSelf()).isFalse();

            // 호출 검증
            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(expenseMapper).findExpenseDetailById(expenseId, roomId, memberId);
            verify(expenseMapper).findExpenseSharesByExpenseId(expenseId, roomId, memberId);
        }

        @Test
        @DisplayName("실패: 해당 방에 존재하지 않는 지출이거나 IDOR 접근 시 IllegalArgumentException이 발생한다.")
        void getExpenseDetail_NotFound_ThrowsException() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
            given(expenseMapper.findExpenseDetailById(expenseId, roomId, memberId))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> expenseService.getExpenseDetail(slug, expenseId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 지출 내역이 존재하지 않습니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(expenseMapper).findExpenseDetailById(expenseId, roomId, memberId);
        }
    }
}
