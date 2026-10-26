package com.splitlink.service;

import com.splitlink.common.util.EncryptionUtil;
import com.splitlink.common.validator.RoomAccessValidator;
import com.splitlink.dto.request.ExpenseBatchCreateRequest;
import com.splitlink.dto.request.ExpenseUpdateRequest;
import com.splitlink.dto.response.ExpenseDetailResponse;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.dto.response.ExpenseListResponse;
import com.splitlink.dto.response.ExpenseUpdateFormResponse;
import com.splitlink.entity.Room;
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

    @Mock
    private EncryptionUtil encryptionUtil;

    @Mock
    private ExchangeRateService exchangeRateService;

    @Nested
    @DisplayName("지출 폼 초기화 데이터 조회 (getExpenseFormInit)")
    class GetExpenseFormInitTest {

        @Test
        @DisplayName("성공: 등록된 계좌가 있는 회원인 경우 계좌 정보를 복호화하여 방 멤버 목록과 함께 반환한다.")
        void getExpenseFormInitSuccessWithAccount() {
            // given
            String slug = "test-room-slug";
            Long memberId = 1L;
            Long roomId = 10L;
            String encryptedAccount = "EncryptedAccountText123";
            String decryptedAccount = "3333-12-345678";

            ExpenseFormInitResponse.AccountInfo accountInfo = ExpenseFormInitResponse.AccountInfo.builder()
                    .bankName("카카오뱅크")
                    .accountNumber(encryptedAccount)
                    .build();

            ExpenseFormInitResponse.MemberInfo member1 = ExpenseFormInitResponse.MemberInfo.builder()
                    .memberId(1L).name("스펀지밥").isActive(true).isSelf(true).build();
            ExpenseFormInitResponse.MemberInfo member2 = ExpenseFormInitResponse.MemberInfo.builder()
                    .memberId(2L).name("뚱이").isActive(false).isSelf(false).build();

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);
            given(memberMapper.findAccountInfoByMemberId(memberId)).willReturn(Optional.of(accountInfo));
            given(encryptionUtil.decrypt(encryptedAccount)).willReturn(decryptedAccount);
            given(memberMapper.findRoomMembersBySlug(slug, memberId)).willReturn(List.of(member1, member2));

            // when
            ExpenseFormInitResponse response = expenseService.getExpenseFormInit(slug, memberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getCurrentMemberId()).isEqualTo(memberId);

            assertThat(response.getDefaultAccount()).isNotNull();
            assertThat(response.getDefaultAccount().getBankName()).isEqualTo("카카오뱅크");
            assertThat(response.getDefaultAccount().getAccountNumber()).isEqualTo(decryptedAccount);

            assertThat(response.getRoomMembers()).hasSize(2);

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(memberMapper).findAccountInfoByMemberId(memberId);
            verify(encryptionUtil).decrypt(encryptedAccount);
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
        @DisplayName("성공: 계좌번호를 암호화하여 저장하며, 환율(1.0) 기반 1/N 정산 시 오차가 첫 번째 참여자에게 가산된다.")
        void createExpensesRemainderAddedToFirstMember() {
            // given
            String slug = "test-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;
            String rawAccount = "3333-12-345678";
            String encryptedAccount = "EncryptedAccountText123";
            LocalDateTime spentAt = LocalDateTime.of(2026, 9, 17, 12, 0);

            ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("저녁 식사")
                    .amount(new BigDecimal("10000"))
                    .targetMemberIds(List.of(1L, 2L, 3L))
                    .build();

            ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                    .payerId(1L)
                    .spentAt(spentAt)
                    .currency("KRW")
                    .bankName("카카오뱅크")
                    .accountNumber(rawAccount)
                    .items(List.of(item))
                    .build();

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of(group))
                    .build();

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, currentMemberId)).willReturn(mockRoom);
            given(encryptionUtil.encrypt(rawAccount)).willReturn(encryptedAccount);
            given(exchangeRateService.getHistoricalExchangeRate("KRW", "KRW", spentAt.toLocalDate()))
                    .willReturn(BigDecimal.ONE);

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

            verify(encryptionUtil).encrypt(rawAccount);
            verify(memberMapper).updateAccountInfo(1L, "카카오뱅크", encryptedAccount);
            verify(exchangeRateService).getHistoricalExchangeRate("KRW", "KRW", spentAt.toLocalDate());
        }

        @Test
        @DisplayName("성공: 외화(USD) 결제 시 환율을 적용하여 1/N 부담금을 방 기준 통화로 환산해 저장한다.")
        void createExpensesWithForeignCurrency() {
            // given
            String slug = "test-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;
            String rawAccount = "3333-12-345678";
            String encryptedAccount = "EncryptedAccountText123";
            LocalDateTime spentAt = LocalDateTime.of(2026, 9, 17, 12, 0);

            ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("미국 스테이크 식사")
                    .amount(new BigDecimal("100")) // 100 USD
                    .targetMemberIds(List.of(1L, 2L))
                    .build();

            ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                    .payerId(1L)
                    .spentAt(spentAt)
                    .currency("USD")
                    .bankName("카카오뱅크")
                    .accountNumber(rawAccount)
                    .items(List.of(item))
                    .build();

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of(group))
                    .build();

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, currentMemberId)).willReturn(mockRoom);
            given(encryptionUtil.encrypt(rawAccount)).willReturn(encryptedAccount);
            given(exchangeRateService.getHistoricalExchangeRate("USD", "KRW", spentAt.toLocalDate()))
                    .willReturn(new BigDecimal("1350.00"));

            // when
            expenseService.createExpenses(slug, currentMemberId, request);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ExpenseMapper.ExpenseShareParam>> captor = ArgumentCaptor.forClass(List.class);
            verify(expenseMapper).insertExpenseShares(captor.capture());

            List<ExpenseMapper.ExpenseShareParam> shares = captor.getValue();

            assertThat(shares).hasSize(2);
            assertThat(shares.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("67500"));
            assertThat(shares.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("67500"));
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

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, currentMemberId)).willReturn(mockRoom);

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

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(true)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, currentMemberId)).willReturn(mockRoom);

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

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .isClosed(true)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, currentMemberId)).willReturn(mockRoom);

            // when & then
            assertThatThrownBy(() -> expenseService.createExpenses(slug, currentMemberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 정산이 완료된 방에는 지출을 추가할 수 없습니다.");
        }

        @Test
        @DisplayName("성공: 소수 4자리 환율(1382.4567) 적용 시 반올림 환산액과 3명 분할 부담금 합계가 정확히 일치한다.")
        void createExpenses_WithDecimalRate_PreservesTotalSum() {
            // given
            String slug = "test-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;
            LocalDateTime spentAt = LocalDateTime.of(2026, 9, 17, 12, 0);

            // 100 USD 결제, 소수 4자리 환율 1382.4567
            ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("뉴욕 저녁 식사")
                    .amount(new BigDecimal("100"))
                    .targetMemberIds(List.of(1L, 2L, 3L))
                    .build();

            ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                    .payerId(1L)
                    .spentAt(spentAt)
                    .currency("USD")
                    .bankName("카카오뱅크")
                    .accountNumber("3333-12-345678")
                    .items(List.of(item))
                    .build();

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of(group))
                    .build();

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, currentMemberId)).willReturn(mockRoom);
            given(encryptionUtil.encrypt(anyString())).willReturn("EncryptedAccountText");
            given(exchangeRateService.getHistoricalExchangeRate("USD", "KRW", spentAt.toLocalDate()))
                    .willReturn(new BigDecimal("1382.4567"));

            // when
            expenseService.createExpenses(slug, currentMemberId, request);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ExpenseMapper.ExpenseShareParam>> captor = ArgumentCaptor.forClass(List.class);
            verify(expenseMapper).insertExpenseShares(captor.capture());

            List<ExpenseMapper.ExpenseShareParam> shares = captor.getValue();
            assertThat(shares).hasSize(3);

            // 100 USD * 1382.4567 = 138245.67 KRW -> HALF_UP 반올림 적용 시 138,246 KRW
            // 138,246 / 3 = 46,082 KRW (나머지 0원)
            BigDecimal totalSum = shares.stream()
                    .map(ExpenseMapper.ExpenseShareParam::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(totalSum).isEqualByComparingTo(new BigDecimal("138246"));
            assertThat(shares.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("46082"));
            assertThat(shares.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("46082"));
            assertThat(shares.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("46082"));
        }

        @Test
        @DisplayName("성공: 기준 통화가 EUR인 방에서 USD 결제 시 소수 2자리 오차가 첫 번째 참여자에게 보정된다.")
        void createExpenses_WithEurBaseCurrency_RoundsScaleAndPreservesSum() {
            // given
            String slug = "test-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;
            LocalDateTime spentAt = LocalDateTime.of(2026, 9, 17, 12, 0);

            // 100.55 USD 결제, USD -> EUR 환율 0.9200
            ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("유럽 박물관 입장료")
                    .amount(new BigDecimal("100.55"))
                    .targetMemberIds(List.of(1L, 2L, 3L))
                    .build();

            ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                    .payerId(1L)
                    .spentAt(spentAt)
                    .currency("USD")
                    .bankName("카카오뱅크")
                    .accountNumber("3333-12-345678")
                    .items(List.of(item))
                    .build();

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of(group))
                    .build();

            // 방 기준 통화: EUR (소수점 2자리)
            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("EUR")
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, currentMemberId)).willReturn(mockRoom);
            given(encryptionUtil.encrypt(anyString())).willReturn("EncryptedAccountText");
            given(exchangeRateService.getHistoricalExchangeRate("USD", "EUR", spentAt.toLocalDate()))
                    .willReturn(new BigDecimal("0.9200"));

            // when
            expenseService.createExpenses(slug, currentMemberId, request);

            // then
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ExpenseMapper.ExpenseShareParam>> captor = ArgumentCaptor.forClass(List.class);
            verify(expenseMapper).insertExpenseShares(captor.capture());

            List<ExpenseMapper.ExpenseShareParam> shares = captor.getValue();
            assertThat(shares).hasSize(3);

            // 100.55 USD * 0.9200 = 92.5060 EUR -> HALF_UP(scale=2) 반올림 시 92.51 EUR
            // 92.51 / 3 = 30.83 EUR (30.83 * 3 = 92.49 EUR, 차액 0.02 EUR가 첫 번째 사람에게 모임)
            BigDecimal totalSum = shares.stream()
                    .map(ExpenseMapper.ExpenseShareParam::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(totalSum).isEqualByComparingTo(new BigDecimal("92.51"));
            assertThat(shares.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("30.85"));
            assertThat(shares.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("30.83"));
            assertThat(shares.get(2).getAmount()).isEqualByComparingTo(new BigDecimal("30.83"));
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
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .build();

            ExpenseListResponse.ExpenseItemResponse item1 = ExpenseListResponse.ExpenseItemResponse.builder()
                    .expenseId(3L)
                    .title("후식 메론")
                    .amount(new BigDecimal("15000"))
                    .currency("KRW")
                    .fxRate(BigDecimal.ONE)
                    .amountInBaseCurrency(new BigDecimal("15000"))
                    .payerName("스펀지밥")
                    .targetMemberCount(2)
                    .isMyPayment(true)
                    .build();

            ExpenseListResponse.ExpenseItemResponse item2 = ExpenseListResponse.ExpenseItemResponse.builder()
                    .expenseId(2L)
                    .title("점심 돈까스")
                    .amount(new BigDecimal("30000"))
                    .currency("KRW")
                    .fxRate(BigDecimal.ONE)
                    .amountInBaseCurrency(new BigDecimal("30000"))
                    .payerName("스펀지밥")
                    .targetMemberCount(2)
                    .isMyPayment(true)
                    .build();

            given(roomAccessValidator.validateAndGetRoomId(slug, currentMemberId)).willReturn(roomId);
            given(roomMapper.getExpenseListHeaderData(roomId, currentMemberId)).willReturn(headerData);
            given(expenseMapper.findExpenseAmountsByRoomId(roomId))
                    .willReturn(List.of(
                            new ExpenseMapper.ExpenseConversionData(
                                    new BigDecimal("30000"),
                                    new BigDecimal("1.0")
                            ),
                            new ExpenseMapper.ExpenseConversionData(
                                    new BigDecimal("15000"),
                                    new BigDecimal("1.0")
                            )
                    ));
            given(expenseMapper.findExpenseItems(roomId, currentMemberId)).willReturn(List.of(item1, item2));

            // when
            ExpenseListResponse response = expenseService.getExpenseList(slug, currentMemberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getRoomTitle()).isEqualTo("일본 여행 정산방");
            assertThat(response.getCurrentMemberName()).isEqualTo("스펀지밥");
            assertThat(response.getRoomBaseCurrency()).isEqualTo("KRW");
            assertThat(response.getTotalExpenseAmount()).isEqualByComparingTo(new BigDecimal("45000"));
            assertThat(response.isLocked()).isFalse();

            assertThat(response.getExpenses()).hasSize(2);
            assertThat(response.getExpenses().get(0).getExpenseId()).isEqualTo(3L);
            assertThat(response.getExpenses().get(0).getAmountInBaseCurrency()).isEqualByComparingTo(new BigDecimal("15000"));

            verify(roomAccessValidator).validateAndGetRoomId(slug, currentMemberId);
            verify(roomMapper).getExpenseListHeaderData(roomId, currentMemberId);
            verify(expenseMapper).findExpenseAmountsByRoomId(roomId);
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

            RoomMapper.ExpenseListHeaderData headerData = RoomMapper.ExpenseListHeaderData.builder()
                    .roomTitle("테스트방")
                    .memberName("기영")
                    .baseCurrency("KRW")
                    .isLocked(true)
                    .build();

            given(roomMapper.getExpenseListHeaderData(roomId, memberId)).willReturn(headerData);
            given(expenseMapper.findExpenseAmountsByRoomId(roomId))
                    .willReturn(List.of(new ExpenseMapper.ExpenseConversionData(new BigDecimal("30000"), BigDecimal.ONE)));

            SettlementMapper.SettlementSummary summary = new SettlementMapper.SettlementSummary(new BigDecimal("5000"), BigDecimal.ZERO);
            given(settlementMapper.findSettlementSummary(roomId, memberId)).willReturn(summary);
            given(expenseMapper.findExpenseItems(roomId, memberId)).willReturn(List.of());

            // when
            ExpenseListResponse response = expenseService.getExpenseList(slug, memberId);

            // then
            assertThat(response.getSettlementStatus()).isEqualTo(ExpenseListResponse.SettlementStatus.SEND);
            assertThat(response.getMySettlementAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(response.getRoomBaseCurrency()).isEqualTo("KRW");
        }

        @Test
        @DisplayName("성공: isLocked=true일 때 받을 금액이 존재하면 RECEIVE 상태와 금액을 반환한다")
        void getExpenseListSettlementReceiveStatusTest() {
            // given
            String slug = "test-slug";
            Long roomId = 1L;
            Long memberId = 20L;

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.ExpenseListHeaderData headerData = RoomMapper.ExpenseListHeaderData.builder()
                    .roomTitle("테스트방")
                    .memberName("기철")
                    .baseCurrency("KRW")
                    .isLocked(true)
                    .build();

            given(roomMapper.getExpenseListHeaderData(roomId, memberId)).willReturn(headerData);
            given(expenseMapper.findExpenseAmountsByRoomId(roomId))
                    .willReturn(List.of(new ExpenseMapper.ExpenseConversionData(new BigDecimal("30000"), BigDecimal.ONE)));

            SettlementMapper.SettlementSummary summary = new SettlementMapper.SettlementSummary(BigDecimal.ZERO, new BigDecimal("5000"));
            given(settlementMapper.findSettlementSummary(roomId, memberId)).willReturn(summary);
            given(expenseMapper.findExpenseItems(roomId, memberId)).willReturn(List.of());

            // when
            ExpenseListResponse response = expenseService.getExpenseList(slug, memberId);

            // then
            assertThat(response.getSettlementStatus()).isEqualTo(ExpenseListResponse.SettlementStatus.RECEIVE);
            assertThat(response.getMySettlementAmount()).isEqualByComparingTo(new BigDecimal("5000"));
            assertThat(response.getRoomBaseCurrency()).isEqualTo("KRW");
        }

        @Test
        @DisplayName("성공: isLocked=true일 때 보낼 금액과 받을 금액이 모두 0원이면 ZERO 상태를 반환한다")
        void getExpenseListSettlementZeroStatusTest() {
            // given
            String slug = "test-slug";
            Long roomId = 1L;
            Long memberId = 30L;

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.ExpenseListHeaderData headerData = RoomMapper.ExpenseListHeaderData.builder()
                    .roomTitle("테스트방")
                    .memberName("오덕")
                    .baseCurrency("KRW")
                    .isLocked(true)
                    .build();

            given(roomMapper.getExpenseListHeaderData(roomId, memberId)).willReturn(headerData);
            given(expenseMapper.findExpenseAmountsByRoomId(roomId))
                    .willReturn(List.of(new ExpenseMapper.ExpenseConversionData(new BigDecimal("30000"), BigDecimal.ONE)));

            SettlementMapper.SettlementSummary summary = new SettlementMapper.SettlementSummary(BigDecimal.ZERO, BigDecimal.ZERO);
            given(settlementMapper.findSettlementSummary(roomId, memberId)).willReturn(summary);
            given(expenseMapper.findExpenseItems(roomId, memberId)).willReturn(List.of());

            // when
            ExpenseListResponse response = expenseService.getExpenseList(slug, memberId);

            // then
            assertThat(response.getSettlementStatus()).isEqualTo(ExpenseListResponse.SettlementStatus.ZERO);
            assertThat(response.getMySettlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getRoomBaseCurrency()).isEqualTo("KRW");
        }

        @Test
        @DisplayName("성공: 외화(USD) 지출이 포함된 경우 방 기준 통화(KRW) 및 환산액(amountInBaseCurrency)이 올바르게 매핑되어 반환된다.")
        void getExpenseListWithForeignCurrencySuccess() {
            // given
            String slug = "test-room-slug";
            Long currentMemberId = 1L;
            Long roomId = 10L;

            RoomMapper.ExpenseListHeaderData headerData = RoomMapper.ExpenseListHeaderData.builder()
                    .roomTitle("미국 여행 정산방")
                    .memberName("스펀지밥")
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .build();

            ExpenseListResponse.ExpenseItemResponse foreignItem = ExpenseListResponse.ExpenseItemResponse.builder()
                    .expenseId(1L)
                    .title("뉴욕 버거")
                    .amount(new BigDecimal("100.00"))
                    .currency("USD")
                    .fxRate(new BigDecimal("1350.00"))
                    .amountInBaseCurrency(new BigDecimal("135000"))
                    .payerName("스펀지밥")
                    .targetMemberCount(2)
                    .isMyPayment(true)
                    .build();

            given(roomAccessValidator.validateAndGetRoomId(slug, currentMemberId)).willReturn(roomId);
            given(roomMapper.getExpenseListHeaderData(roomId, currentMemberId)).willReturn(headerData);
            given(expenseMapper.findExpenseAmountsByRoomId(roomId))
                    .willReturn(List.of(new ExpenseMapper.ExpenseConversionData(new BigDecimal("100.00"), new BigDecimal("1350.00"))));
            given(expenseMapper.findExpenseItems(roomId, currentMemberId)).willReturn(List.of(foreignItem));

            // when
            ExpenseListResponse response = expenseService.getExpenseList(slug, currentMemberId);

            // then
            assertThat(response).isNotNull();
            assertThat(response.getRoomBaseCurrency()).isEqualTo("KRW");
            assertThat(response.getTotalExpenseAmount()).isEqualByComparingTo(new BigDecimal("135000"));
            assertThat(response.getExpenses()).hasSize(1);
            assertThat(response.getExpenses().get(0).getCurrency()).isEqualTo("USD");
            assertThat(response.getExpenses().get(0).getFxRate()).isEqualByComparingTo(new BigDecimal("1350.00"));
            assertThat(response.getExpenses().get(0).getAmountInBaseCurrency()).isEqualByComparingTo(new BigDecimal("135000"));
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
        @DisplayName("성공: 지출 상세 정보 조회 시 암호화된 계좌번호를 복호화하여 반환한다.")
        void getExpenseDetail_Success() {
            // given
            String encryptedAccount = "EncryptedAccountText123";
            String decryptedAccount = "3333-12-3456789";

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            ExpenseDetailResponse mockDetail = ExpenseDetailResponse.builder()
                    .expenseId(expenseId)
                    .title("점심 돈까스")
                    .amount(new BigDecimal("30000.00"))
                    .currency("KRW")
                    .fxRate(BigDecimal.ONE)
                    .amountInBaseCurrency(new BigDecimal("30000.00"))
                    .roomBaseCurrency("KRW")
                    .spentAt(LocalDateTime.of(2026, 9, 9, 18, 30))
                    .payerId(38L)
                    .payerName("스펀지밥")
                    .bankName("카카오뱅크")
                    .accountNumber(encryptedAccount)
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
            given(encryptionUtil.decrypt(encryptedAccount)).willReturn(decryptedAccount);

            // when
            ExpenseDetailResponse result = expenseService.getExpenseDetail(slug, expenseId, memberId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getExpenseId()).isEqualTo(expenseId);
            assertThat(result.getTitle()).isEqualTo("점심 돈까스");
            assertThat(result.getPayerName()).isEqualTo("스펀지밥");
            assertThat(result.getBankName()).isEqualTo("카카오뱅크");
            assertThat(result.getAccountNumber()).isEqualTo(decryptedAccount);
            assertThat(result.getAmountInBaseCurrency()).isEqualByComparingTo(new BigDecimal("30000.00"));
            assertThat(result.getRoomBaseCurrency()).isEqualTo("KRW");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(expenseMapper).findExpenseDetailById(expenseId, roomId, memberId);
            verify(encryptionUtil).decrypt(encryptedAccount);
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

        @Test
        @DisplayName("성공: 외화 지출 상세 조회 시 방 기준 통화의 소수점 자릿수에 맞춰 환산 금액을 반올림한다.")
        void getExpenseDetail_RoundsConvertedAmountToBaseCurrencyScale() {
            // given
            String encryptedAccount = "EncryptedAccountText123";

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId))
                    .willReturn(roomId);

            ExpenseDetailResponse mockDetail = ExpenseDetailResponse.builder()
                    .expenseId(expenseId)
                    .title("미국 여행 식사")
                    .amount(new BigDecimal("10.25"))
                    .currency("USD")
                    .fxRate(new BigDecimal("1350.00"))
                    .amountInBaseCurrency(new BigDecimal("13837.50"))
                    .roomBaseCurrency("KRW")
                    .spentAt(LocalDateTime.of(2026, 9, 9, 18, 30))
                    .payerId(memberId)
                    .payerName("스펀지밥")
                    .bankName("카카오뱅크")
                    .accountNumber(encryptedAccount)
                    .isMyPayment(true)
                    .build();

            given(expenseMapper.findExpenseDetailById(expenseId, roomId, memberId))
                    .willReturn(Optional.of(mockDetail));

            given(expenseMapper.findExpenseSharesByExpenseId(expenseId, roomId, memberId))
                    .willReturn(List.of());

            given(encryptionUtil.decrypt(encryptedAccount))
                    .willReturn("3333-12-3456789");

            // when
            ExpenseDetailResponse result =
                    expenseService.getExpenseDetail(slug, expenseId, memberId);

            // then
            // 10.25 USD × 1350 KRW = 13837.50 KRW
            // KRW는 소수점 이하를 사용하지 않으므로 HALF_UP 반올림 결과는 13838
            assertThat(result.getAmountInBaseCurrency())
                    .isEqualByComparingTo(new BigDecimal("13838"));

            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10.25"));
            assertThat(result.getCurrency()).isEqualTo("USD");
            assertThat(result.getFxRate()).isEqualByComparingTo(new BigDecimal("1350.00"));
            assertThat(result.getRoomBaseCurrency()).isEqualTo("KRW");
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
        @DisplayName("성공: 지출 수정 폼 데이터 조회 시 암호화된 계좌번호 복호화, 통화, 환율, 방 기준 통화 및 멤버 목록을 포함하여 반환한다.")
        void getExpenseUpdateForm_Success() {
            // given
            String encryptedAccount = "EncryptedAccountText123";
            String decryptedAccount = "3333-12-345678";

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            ExpenseUpdateFormResponse mockForm = ExpenseUpdateFormResponse.builder()
                    .expenseId(expenseId)
                    .title("돈까스 정식")
                    .amount(new BigDecimal("25.00"))
                    .bankName("카카오뱅크")
                    .accountNumber(encryptedAccount)
                    .currency("USD")
                    .fxRate(new BigDecimal("1350.00"))
                    .amountInBaseCurrency(new BigDecimal("33750.00"))
                    .roomBaseCurrency("KRW")
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
            given(encryptionUtil.decrypt(encryptedAccount)).willReturn(decryptedAccount);
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
            assertThat(result.getBankName()).isEqualTo("카카오뱅크");
            assertThat(result.getAccountNumber()).isEqualTo(decryptedAccount);
            assertThat(result.getCurrency()).isEqualTo("USD");
            assertThat(result.getFxRate()).isEqualByComparingTo(new BigDecimal("1350.00"));
            assertThat(result.getAmountInBaseCurrency()).isEqualByComparingTo(new BigDecimal("33750.00"));
            assertThat(result.getRoomBaseCurrency()).isEqualTo("KRW");
            assertThat(result.getTargetMemberIds()).containsExactly(1L, 2L);

            assertThat(result.getRoomMembers()).hasSize(2);
            assertThat(result.getRoomMembers().get(0).getName()).isEqualTo("스펀지밥");

            verify(roomAccessValidator).validateAndGetRoomId(slug, memberId);
            verify(roomMapper).findRoomStatusBySlug(slug);
            verify(expenseMapper).findExpenseUpdateFormById(expenseId, roomId);
            verify(encryptionUtil).decrypt(encryptedAccount);
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

        @Test
        @DisplayName("성공: 외화 지출 수정 폼 조회 시 방 기준 통화의 소수점 자릿수에 맞춰 환산 금액을 반올림한다.")
        void getExpenseUpdateForm_RoundsConvertedAmountToBaseCurrencyScale() {
            // given
            String encryptedAccount = "EncryptedAccountText123";

            given(roomAccessValidator.validateAndGetRoomId(slug, memberId)).willReturn(roomId);

            RoomMapper.RoomStatus mockStatus = RoomMapper.RoomStatus.builder()
                    .isLocked(false)
                    .isClosed(false)
                    .build();
            given(roomMapper.findRoomStatusBySlug(slug)).willReturn(mockStatus);

            ExpenseUpdateFormResponse mockForm = ExpenseUpdateFormResponse.builder()
                    .expenseId(expenseId)
                    .title("미국 스테이크")
                    .amount(new BigDecimal("10.25"))
                    .currency("USD")
                    .fxRate(new BigDecimal("1350.00"))
                    .roomBaseCurrency("KRW")
                    .bankName("카카오뱅크")
                    .accountNumber(encryptedAccount)
                    .spentAt(LocalDateTime.of(2026, 9, 17, 12, 0))
                    .payerId(1L)
                    .build();

            given(expenseMapper.findExpenseUpdateFormById(expenseId, roomId)).willReturn(Optional.of(mockForm));
            given(encryptionUtil.decrypt(encryptedAccount)).willReturn("3333-12-345678");
            given(expenseMapper.findTargetMemberIdsByExpenseId(expenseId)).willReturn(List.of(1L));
            given(memberMapper.findRoomMembersBySlug(slug, memberId)).willReturn(List.of());

            // when
            ExpenseUpdateFormResponse result = expenseService.getExpenseUpdateForm(slug, expenseId, memberId);

            // then
            // 10.25 USD × 1350.00 KRW = 13837.50 KRW -> HALF_UP 적용 시 13838 KRW
            assertThat(result.getAmountInBaseCurrency()).isEqualByComparingTo(new BigDecimal("13838"));
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("10.25"));
            assertThat(result.getFxRate()).isEqualByComparingTo(new BigDecimal("1350.00"));
        }
    }

    @Nested
    @DisplayName("지출 내역 수정 (updateExpense)")
    class UpdateExpenseTest {

        private final String slug = "test-room-slug";
        private final Long roomId = 10L;
        private final Long expenseId = 1L;
        private final Long memberId = 100L;

        private ExpenseUpdateRequest createRequest(Long payerId, List<Long> targetMemberIds, String currency) {
            return ExpenseUpdateRequest.builder()
                    .payerId(payerId)
                    .title("수정된 점심 식사")
                    .amount(new BigDecimal("30"))
                    .currency(currency)
                    .bankName("카카오뱅크")
                    .accountNumber("3333-12-345678")
                    .spentAt(LocalDateTime.of(2026, 9, 17, 12, 0))
                    .targetMemberIds(targetMemberIds)
                    .build();
        }

        @Test
        @DisplayName("성공: 입력받은 계좌번호를 암호화하여 지출 및 결제자 계좌를 수정하고, 환율(1.0) 기반 부담금을 재등록한다.")
        void updateExpense_Success() {
            // given
            String rawAccount = "3333-12-345678";
            String encryptedAccount = "EncryptedAccountText123";
            LocalDateTime spentAt = LocalDateTime.of(2026, 9, 17, 12, 0);

            ExpenseUpdateRequest request = createRequest(100L, List.of(100L, 101L), "KRW");

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(mockRoom);
            given(encryptionUtil.encrypt(rawAccount)).willReturn(encryptedAccount);
            given(exchangeRateService.getHistoricalExchangeRate("KRW", "KRW", spentAt.toLocalDate()))
                    .willReturn(BigDecimal.ONE);

            given(expenseMapper.updateExpense(
                    eq(expenseId), eq(roomId), eq(request.getPayerId()),
                    eq(request.getTitle()), eq(request.getAmount()),
                    eq(new BigDecimal("30")),
                    eq("카카오뱅크"), eq(encryptedAccount), eq("KRW"), eq(BigDecimal.ONE), eq(spentAt)
            )).willReturn(1);

            given(expenseMapper.deleteExpenseSharesByExpenseId(expenseId)).willReturn(2);

            // when
            expenseService.updateExpense(slug, expenseId, memberId, request);

            // then
            verify(roomAccessValidator).validateAndGetRoom(slug, memberId);
            verify(roomAccessValidator).validateMembersInRoom(eq(roomId), anyList());
            verify(encryptionUtil).encrypt(rawAccount);
            verify(memberMapper).updateAccountInfo(100L, "카카오뱅크", encryptedAccount);
            verify(exchangeRateService).getHistoricalExchangeRate("KRW", "KRW", spentAt.toLocalDate());
            verify(expenseMapper).updateExpense(
                    eq(expenseId), eq(roomId), eq(request.getPayerId()),
                    eq(request.getTitle()), eq(request.getAmount()),
                    eq(new BigDecimal("30")),
                    eq("카카오뱅크"), eq(encryptedAccount), eq("KRW"), eq(BigDecimal.ONE), eq(spentAt)
            );
            verify(expenseMapper).deleteExpenseSharesByExpenseId(expenseId);
            verify(expenseMapper).insertExpenseShares(anyList());
        }

        @Test
        @DisplayName("성공: 외화(USD) 수정 시 새로운 환율을 적용하여 지출 정보 및 부담금을 재등록한다.")
        void updateExpense_WithForeignCurrency_Success() {
            // given
            String rawAccount = "3333-12-345678";
            String encryptedAccount = "EncryptedAccountText123";
            LocalDateTime spentAt = LocalDateTime.of(2026, 9, 17, 12, 0);
            BigDecimal usdRate = new BigDecimal("1350.00");

            ExpenseUpdateRequest request = createRequest(100L, List.of(100L, 101L), "USD");

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(mockRoom);
            given(encryptionUtil.encrypt(rawAccount)).willReturn(encryptedAccount);
            given(exchangeRateService.getHistoricalExchangeRate("USD", "KRW", spentAt.toLocalDate()))
                    .willReturn(usdRate);

            given(expenseMapper.updateExpense(
                    eq(expenseId), eq(roomId), eq(request.getPayerId()),
                    eq(request.getTitle()), eq(request.getAmount()),
                    eq(new BigDecimal("40500")),
                    eq("카카오뱅크"), eq(encryptedAccount), eq("USD"), eq(usdRate), eq(spentAt)
            )).willReturn(1);

            given(expenseMapper.deleteExpenseSharesByExpenseId(expenseId)).willReturn(2);

            // when
            expenseService.updateExpense(slug, expenseId, memberId, request);

            // then
            verify(roomAccessValidator).validateAndGetRoom(slug, memberId);
            verify(exchangeRateService).getHistoricalExchangeRate("USD", "KRW", spentAt.toLocalDate());
            verify(expenseMapper).updateExpense(
                    eq(expenseId), eq(roomId), eq(request.getPayerId()),
                    eq(request.getTitle()), eq(request.getAmount()),
                    eq(new BigDecimal("40500")),
                    eq("카카오뱅크"), eq(encryptedAccount), eq("USD"), eq(usdRate), eq(spentAt)
            );

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<ExpenseMapper.ExpenseShareParam>> captor = ArgumentCaptor.forClass(List.class);
            verify(expenseMapper).insertExpenseShares(captor.capture());

            List<ExpenseMapper.ExpenseShareParam> shares = captor.getValue();
            assertThat(shares).hasSize(2);
            // 30 USD * 1350 = 40,500 KRW -> 2명 분할 시 각 20,250 KRW
            assertThat(shares.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("20250"));
            assertThat(shares.get(1).getAmount()).isEqualByComparingTo(new BigDecimal("20250"));
        }

        @Test
        @DisplayName("예외: 정산 완료(isClosed=true) 상태인 경우 예외가 발생한다.")
        void updateExpense_ThrowExceptionWhenRoomIsClosed() {
            // given
            ExpenseUpdateRequest request = createRequest(100L, List.of(100L, 101L), "KRW");

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .isClosed(true)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(mockRoom);

            // when & then
            assertThatThrownBy(() -> expenseService.updateExpense(slug, expenseId, memberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 정산이 완료된 방의 지출은 수정할 수 없습니다.");
        }

        @Test
        @DisplayName("예외: 지출 입력 완료(isLocked=true) 상태인 경우 예외가 발생한다.")
        void updateExpense_ThrowExceptionWhenRoomIsLocked() {
            // given
            ExpenseUpdateRequest request = createRequest(100L, List.of(100L, 101L), "KRW");

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(true)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(mockRoom);

            // when & then
            assertThatThrownBy(() -> expenseService.updateExpense(slug, expenseId, memberId, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("이미 지출 입력이 완료된 방의 지출은 수정할 수 없습니다.");
        }

        @Test
        @DisplayName("예외: 존재하지 않는 지출이거나 업데이트된 행이 0개인 경우 예외가 발생한다.")
        void updateExpense_ThrowExceptionWhenExpenseNotFound() {
            // given
            String rawAccount = "3333-12-345678";
            String encryptedAccount = "EncryptedAccountText123";
            LocalDateTime spentAt = LocalDateTime.of(2026, 9, 17, 12, 0);

            ExpenseUpdateRequest request = createRequest(100L, List.of(100L, 101L), "KRW");

            Room mockRoom = Room.builder()
                    .roomId(roomId)
                    .baseCurrency("KRW")
                    .isLocked(false)
                    .isClosed(false)
                    .build();

            given(roomAccessValidator.validateAndGetRoom(slug, memberId)).willReturn(mockRoom);
            given(encryptionUtil.encrypt(rawAccount)).willReturn(encryptedAccount);
            given(exchangeRateService.getHistoricalExchangeRate("KRW", "KRW", spentAt.toLocalDate()))
                    .willReturn(BigDecimal.ONE);

            given(expenseMapper.updateExpense(
                    eq(expenseId), eq(roomId), eq(request.getPayerId()),
                    eq(request.getTitle()), eq(request.getAmount()),
                    eq(new BigDecimal("30")),
                    eq("카카오뱅크"), eq(encryptedAccount), eq("KRW"), eq(BigDecimal.ONE), eq(spentAt)
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