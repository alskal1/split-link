package com.splitlink.service;

import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.request.ExpenseBatchCreateRequest;
import com.splitlink.dto.request.ExpenseUpdateRequest;
import com.splitlink.dto.response.ExpenseDetailResponse;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.dto.response.ExpenseListResponse;
import com.splitlink.dto.response.ExpenseUpdateFormResponse;
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
import static org.mockito.Mockito.*;

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

    @Nested
    @DisplayName("지출 폼 초기화 데이터 조회 (getExpenseFormInit)")
    class GetExpenseFormInitTest {

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

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
            given(memberMapper.findAccountInfoByMemberId(memberId)).willReturn(Optional.empty());
            given(memberMapper.findRoomMembersBySlug(slug, memberId)).willReturn(List.of());

            // when
            ExpenseFormInitResponse response = expenseService.getExpenseFormInit(slug, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getDefaultAccount()).isNull();

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(memberMapper).findAccountInfoByMemberId(memberId);
        }

        @Test
        @DisplayName("예외: 존재하지 않는 회원 ID로 요청 시 IllegalArgumentException 예외가 발생한다.")
        void getExpenseFormInitThrowExceptionWhenMemberNotFound() {
            // given
            String slug = "test-room-slug";
            Long invalidMemberId = 999L;

            given(roomAccessValidator.validateAndGetRoomId(slug, invalidMemberId))
                    .willThrow(new IllegalArgumentException("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다."));

            // when & then
            assertThatThrownBy(() -> expenseService.getExpenseFormInit(slug, invalidMemberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다.");
        }
    }

    @Nested
    @DisplayName("지출 내역 일괄 등록 (createExpenses)")
    class CreateExpensesTest {

        @Test
        @DisplayName("성공: 1/N 정산 시 소수점 버림 후 남은 1원 오차가 첫 번째 참여자에게 정상 가산된다.")
        void createExpensesRemainderAddedToFirstMember() {
            // given
            String slug = "test-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;

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

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            // when
            expenseService.createExpenses(slug, currentMemberId, request);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ExpenseMapper.ExpenseShareParam>> captor = ArgumentCaptor.forClass(List.class);
            verify(expenseMapper).insertExpenseShares(captor.capture());

            List<ExpenseMapper.ExpenseShareParam> shares = captor.getValue();

            assertThat(shares).hasSize(3);
            assertThat(shares.get(0).getAmount()).isEqualTo(new BigDecimal("3334"));
            assertThat(shares.get(1).getAmount()).isEqualTo(new BigDecimal("3333"));
            assertThat(shares.get(2).getAmount()).isEqualTo(new BigDecimal("3333"));

            verify(memberMapper).updateAccountInfo(1L, "카카오뱅크", "3333-12-345678");
        }

        @Test
        @DisplayName("예외: 요청 바디의 payerId 또는 targetMemberIds 중 방 소속이 아닌 멤버가 있으면 예외가 발생한다.")
        void createExpensesThrowExceptionWhenMemberNotInRoom() {
            // given
            String slug = "test-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;
            Long outsideMemberId = 999L;

            ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("저녁 식사")
                    .amount(new BigDecimal("10000"))
                    .targetMemberIds(List.of(1L, outsideMemberId))
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

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            doThrow(new IllegalArgumentException("해당 방에 속하지 않은 참여자가 포함되어 있습니다."))
                    .when(roomAccessValidator).validateMembersInRoom(eq(roomId), anyList());

            // when & then
            assertThatThrownBy(() -> expenseService.createExpenses(slug, currentMemberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 방에 속하지 않은 참여자가 포함되어 있습니다.");
        }

        @Test
        @DisplayName("예외: 지출 입력이 잠긴(isLocked=true) 방에는 지출을 추가할 수 없다.")
        void createExpensesThrowExceptionWhenRoomIsLocked() {
            // given
            String slug = "test-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of())
                    .build();

            given(roomAccessValidator.validateAndGetRoomId(slug, currentMemberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(true)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            // when & then
            assertThatThrownBy(() -> expenseService.createExpenses(slug, currentMemberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 지출 입력이 잠긴 방에는 지출을 추가할 수 없습니다.");
        }

        @Test
        @DisplayName("예외: 정산이 완료(isClosed=true)된 방에는 지출을 추가할 수 없다.")
        void createExpensesThrowExceptionWhenRoomIsClosed() {
            // given
            String slug = "test-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of())
                    .build();

            given(roomAccessValidator.validateAndGetRoomId(slug, currentMemberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(true)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            // when & then
            assertThatThrownBy(() -> expenseService.createExpenses(slug, currentMemberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 정산이 완료된 방에는 지출을 추가할 수 없습니다.");
        }
    }

    @Nested
    @DisplayName("지출 목록 및 정산 요약 조회 (getExpenseList)")
    class GetExpenseListTest {

        @Test
        @DisplayName("성공: 지출 목록 조회 시 방 정보, 총액 및 isMyPayment, targetMemberCount가 포함된 목록을 반환한다.")
        void getExpenseListSuccess() {
            // given
            String slug = "test-room-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;

            RoomMapper.ExpenseListHeaderData headerData = RoomMapper.ExpenseListHeaderData.builder()
                    .roomTitle("일본 여행 정산방")
                    .memberName("스펀지밥")
                    .isLocked(false)
                    .build();

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

            assertThat(response.getExpenses()).hasSize(2);
            assertThat(response.getExpenses().get(0).getExpenseId()).isEqualTo(3L);
            assertThat(response.getExpenses().get(0).getTitle()).isEqualTo("후식 메론");
            assertThat(response.getExpenses().get(0).isMyPayment()).isTrue();
            assertThat(response.getExpenses().get(0).getTargetMemberCount()).isEqualTo(2);

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

            RoomMapper.ExpenseListHeaderData headerData = new RoomMapper.ExpenseListHeaderData("테스트방", "기영", true);
            given(roomMapper.getExpenseListHeaderData(roomId, memberId)).willReturn(headerData);
            given(expenseMapper.findTotalExpenseAmountByRoomId(roomId)).willReturn(new BigDecimal("30000"));

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

            SettlementMapper.SettlementSummary summary = new SettlementMapper.SettlementSummary(BigDecimal.ZERO, BigDecimal.ZERO);
            given(settlementMapper.findSettlementSummary(roomId, memberId)).willReturn(summary);
            given(expenseMapper.findExpenseItems(roomId, memberId)).willReturn(List.of());

            // when
            ExpenseListResponse response = expenseService.getExpenseList(slug, memberId);

            // then
            assertThat(response.getSettlementStatus()).isEqualTo(ExpenseListResponse.SettlementStatus.ZERO);
            assertThat(response.getMySettlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("지출 상세 조회 (getExpenseDetail)")
    class GetExpenseDetailTest {

        private final String slug = "82f31815-3763-4648-8245-d7c5dcd90a24";
        private final Long roomId = 1L;
        private final Long expenseId = 2L;
        private final Long memberId = 38L;

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

            assertThat(result.getTargetMembers()).hasSize(2);
            assertThat(result.getTargetMembers().get(0).getName()).isEqualTo("스펀지밥");
            assertThat(result.getTargetMembers().get(0).isSelf()).isTrue();
            assertThat(result.getTargetMembers().get(1).getName()).isEqualTo("다람이");
            assertThat(result.getTargetMembers().get(1).isSelf()).isFalse();

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

    @Nested
    @DisplayName("지출 수정 폼 데이터 조회 (getExpenseUpdateForm)")
    class GetExpenseUpdateFormTest {

        private final String slug = "test-room-slug";
        private final Long roomId = 10L;
        private final Long expenseId = 100L;
        private final Long memberId = 1L;

        @Test
        @DisplayName("성공: 지출 기본 정보, 참여자 ID 목록, 방 멤버 목록을 정상 조립하여 반환한다.")
        void getExpenseUpdateForm_Success() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            ExpenseUpdateFormResponse mockForm = ExpenseUpdateFormResponse.builder()
                    .expenseId(expenseId)
                    .title("돈까스 정식")
                    .amount(new BigDecimal("25000"))
                    .currency("KRW")
                    .spentAt(LocalDateTime.of(2026, 9, 17, 12, 0))
                    .payerId(1L)
                    .build();

            List<Long> mockTargetMemberIds = List.of(1L, 2L);

            List<ExpenseFormInitResponse.MemberInfo> mockRoomMembers = List.of(
                    ExpenseFormInitResponse.MemberInfo.builder()
                            .memberId(1L)
                            .name("스펀지밥")
                            .isActive(true)
                            .isSelf(true)
                            .build(),
                    ExpenseFormInitResponse.MemberInfo.builder()
                            .memberId(2L)
                            .name("뚱이")
                            .isActive(false)
                            .isSelf(false)
                            .build()
            );

            given(expenseMapper.findExpenseUpdateFormById(expenseId, roomId))
                    .willReturn(Optional.of(mockForm));
            given(expenseMapper.findTargetMemberIdsByExpenseId(expenseId))
                    .willReturn(mockTargetMemberIds);
            given(memberMapper.findRoomMembersBySlug(slug, memberId))
                    .willReturn(mockRoomMembers);

            // when
            ExpenseUpdateFormResponse result = expenseService.getExpenseUpdateForm(slug, expenseId, memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getExpenseId()).isEqualTo(expenseId);
            assertThat(result.getTitle()).isEqualTo("돈까스 정식");
            assertThat(result.getAmount()).isEqualByComparingTo("25000");
            assertThat(result.getPayerId()).isEqualTo(1L);
            assertThat(result.getTargetMemberIds()).containsExactly(1L, 2L);

            assertThat(result.getRoomMembers()).hasSize(2);
            assertThat(result.getRoomMembers().get(0).getMemberId()).isEqualTo(1L);
            assertThat(result.getRoomMembers().get(0).getName()).isEqualTo("스펀지밥");
            assertThat(result.getRoomMembers().get(0).isActive()).isTrue();

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
            verify(expenseMapper).findExpenseUpdateFormById(expenseId, roomId);
            verify(expenseMapper).findTargetMemberIdsByExpenseId(expenseId);
            verify(memberMapper).findRoomMembersBySlug(slug, memberId);
        }

        @Test
        @DisplayName("예외: 존재하지 않는 방인 경우 IllegalArgumentException 예외가 발생한다.")
        void getExpenseUpdateForm_ThrowExceptionWhenRoomNotFound() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> expenseService.getExpenseUpdateForm(slug, expenseId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("존재하지 않는 방입니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
        }

        @Test
        @DisplayName("예외: 정산이 완료(isClosed=true)된 방인 경우 IllegalArgumentException 예외가 발생한다.")
        void getExpenseUpdateForm_ThrowExceptionWhenRoomIsClosed() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(true)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            // when & then
            assertThatThrownBy(() -> expenseService.getExpenseUpdateForm(slug, expenseId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 정산이 완료된 방의 지출은 수정할 수 없습니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
        }

        @Test
        @DisplayName("예외: 지출 입력이 잠긴(isLocked=true) 방인 경우 IllegalArgumentException 예외가 발생한다.")
        void getExpenseUpdateForm_ThrowExceptionWhenRoomIsLocked() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(true)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            // when & then
            assertThatThrownBy(() -> expenseService.getExpenseUpdateForm(slug, expenseId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 지출 입력이 잠긴 방의 지출은 수정할 수 없습니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
        }

        @Test
        @DisplayName("예외: 해당 방에 지출 내역이 존재하지 않을 경우 IllegalArgumentException 예외가 발생한다.")
        void getExpenseUpdateForm_ThrowExceptionWhenExpenseNotFound() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);
            given(expenseMapper.findExpenseUpdateFormById(expenseId, roomId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> expenseService.getExpenseUpdateForm(slug, expenseId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 지출 내역이 존재하지 않습니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
            verify(expenseMapper).findExpenseUpdateFormById(expenseId, roomId);
        }
    }

    @Nested
    @DisplayName("지출 내역 수정 (updateExpense)")
    class UpdateExpenseTest {

        private final String slug = "test-room-slug";
        private final Long roomId = 10L;
        private final Long expenseId = 1L;
        private final Long memberId = 100L;

        private ExpenseUpdateRequest createRequest(Long payerId, List<Long> targetMemberIds) {
            return ExpenseUpdateRequest.builder()
                    .payerId(payerId)
                    .title("수정된 점심 식사")
                    .amount(new BigDecimal("30000"))
                    .spentAt(LocalDateTime.of(2026, 9, 17, 12, 0))
                    .targetMemberIds(targetMemberIds)
                    .build();
        }

        @Test
        @DisplayName("성공: 올바른 요청 시 기존 지출 및 부담금을 수정하고 새 부담금을 계산하여 재등록한다.")
        void updateExpense_Success() {
            // given
            ExpenseUpdateRequest request = createRequest(100L, List.of(100L, 101L));

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            given(expenseMapper.updateExpense(
                    eq(expenseId), eq(roomId), eq(request.getPayerId()),
                    eq(request.getTitle()), eq(request.getAmount()), eq(request.getSpentAt())
            )).willReturn(1);

            given(expenseMapper.deleteExpenseSharesByExpenseId(expenseId)).willReturn(2);

            // when
            expenseService.updateExpense(slug, expenseId, memberId, request);

            // then
            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
            verify(roomAccessValidator).validateMembersInRoom(eq(roomId), anyList());
            verify(expenseMapper).updateExpense(
                    eq(expenseId), eq(roomId), eq(request.getPayerId()),
                    eq(request.getTitle()), eq(request.getAmount()), eq(request.getSpentAt())
            );
            verify(expenseMapper).deleteExpenseSharesByExpenseId(expenseId);
            verify(expenseMapper).insertExpenseShares(anyList());
        }

        @Test
        @DisplayName("예외: 정산 완료(isClosed=true) 상태인 경우 예외가 발생한다.")
        void updateExpense_ThrowExceptionWhenRoomIsClosed() {
            // given
            ExpenseUpdateRequest request = createRequest(100L, List.of(100L, 101L));

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(true)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            // when & then
            assertThatThrownBy(() -> expenseService.updateExpense(slug, expenseId, memberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 정산이 완료된 방의 지출은 수정할 수 없습니다.");
        }

        @Test
        @DisplayName("예외: 지출 입력 완료(isLocked=true) 상태인 경우 예외가 발생한다.")
        void updateExpense_ThrowExceptionWhenRoomIsLocked() {
            // given
            ExpenseUpdateRequest request = createRequest(100L, List.of(100L, 101L));

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(true)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            // when & then
            assertThatThrownBy(() -> expenseService.updateExpense(slug, expenseId, memberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 지출 입력이 완료된 방의 지출은 수정할 수 없습니다.");
        }

        @Test
        @DisplayName("예외: 존재하지 않는 지출이거나 업데이트된 행이 0개인 경우 예외가 발생한다.")
        void updateExpense_ThrowExceptionWhenExpenseNotFound() {
            // given
            ExpenseUpdateRequest request = createRequest(100L, List.of(100L, 101L));

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            given(expenseMapper.updateExpense(
                    eq(expenseId), eq(roomId), eq(request.getPayerId()),
                    eq(request.getTitle()), eq(request.getAmount()), eq(request.getSpentAt())
            )).willReturn(0);

            // when & then
            assertThatThrownBy(() -> expenseService.updateExpense(slug, expenseId, memberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방에 존재하지 않는 지출이거나 이미 삭제된 지출입니다.");
        }
    }

    @Nested
    @DisplayName("지출 내역 삭제 (deleteExpense)")
    class DeleteExpenseTest {

        private final String slug = "test-room-slug";
        private final Long roomId = 10L;
        private final Long expenseId = 1L;
        private final Long memberId = 100L;

        @Test
        @DisplayName("성공: 올바른 방 식별자, 지출 ID 및 회원 ID로 요청 시 지출 내역을 성공적으로 삭제한다.")
        void deleteExpenseSuccess() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);
            given(expenseMapper.deleteExpenseById(expenseId, roomId)).willReturn(1);

            // when & then
            expenseService.deleteExpense(slug, expenseId, memberId);

            // verify
            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
            verify(expenseMapper).deleteExpenseById(expenseId, roomId);
        }

        @Test
        @DisplayName("예외: 정산이 이미 완료(isClosed=true)된 방인 경우 IllegalArgumentException 예외가 발생한다.")
        void deleteExpenseThrowExceptionWhenRoomIsClosed() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(true)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            // when & then
            assertThatThrownBy(() -> expenseService.deleteExpense(slug, expenseId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 정산이 완료된 방의 지출은 삭제할 수 없습니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
        }

        @Test
        @DisplayName("예외: 지출 입력이 잠긴(isLocked=true) 방인 경우 IllegalArgumentException 예외가 발생한다.")
        void deleteExpenseThrowExceptionWhenRoomIsLocked() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(true)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            // when & then
            assertThatThrownBy(() -> expenseService.deleteExpense(slug, expenseId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 지출 입력이 잠긴 방의 지출은 삭제할 수 없습니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
        }

        @Test
        @DisplayName("예외: 존재하지 않는 지출 ID이거나 영향받은 행이 0개인 경우 IllegalArgumentException 예외가 발생한다.")
        void deleteExpenseThrowExceptionWhenExpenseNotFound() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);
            given(expenseMapper.deleteExpenseById(expenseId, roomId)).willReturn(0);

            // when & then
            assertThatThrownBy(() -> expenseService.deleteExpense(slug, expenseId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방에 존재하지 않는 지출이거나 이미 삭제된 지출입니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
            verify(expenseMapper).deleteExpenseById(expenseId, roomId);
        }

        @Test
        @DisplayName("예외: 방 소속이 아닌 사용자(비인가)가 삭제 시도 시 예외가 발생한다.")
        void deleteExpenseThrowExceptionWhenUnauthorizedUser() {
            // given
            given(roomAccessValidator.validateAndGetRoomId(slug, memberId))
                    .willThrow(new IllegalArgumentException("해당 방에 접근 권한이 없습니다."));

            // when & then
            assertThatThrownBy(() -> expenseService.deleteExpense(slug, expenseId, memberId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("해당 방에 접근 권한이 없습니다.");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verifyNoInteractions(roomMapper);
            verifyNoInteractions(expenseMapper);
        }
    }
}