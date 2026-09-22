package com.splitlink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitlink.common.jwt.JwtProvider;
import com.splitlink.common.resolver.AuthMemberArgumentResolver;
import com.splitlink.dto.request.ExpenseBatchCreateRequest;
import com.splitlink.dto.request.ExpenseUpdateRequest;
import com.splitlink.dto.response.ExpenseDetailResponse;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.dto.response.ExpenseListResponse;
import com.splitlink.dto.response.ExpenseUpdateFormResponse;
import com.splitlink.service.ExpenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@Import(AuthMemberArgumentResolver.class)
public class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private JwtProvider jwtProvider;

    @Nested
    @DisplayName("GET /api/rooms/{slug}/expenses/new - 지출 입력 폼 초기화")
    class GetExpenseFormInit {

        @Test
        @DisplayName("JWT 토큰이 헤더에 포함되면 @AuthMember를 통해 memberId를 추출하고 지출 폼 초기 데이터를 반환한다")
        void getExpenseFormInitAuthSuccess() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            // JwtProvider Mocking (인증 처리 모킹)
            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            // 컨트롤러가 반환할 가짜 ExpenseFormInitResponse DTO 응답 생성
            ExpenseFormInitResponse.AccountInfo accountInfo = ExpenseFormInitResponse.AccountInfo.builder()
                    .bankName("카카오뱅크")
                    .accountNumber("3333-12-345678")
                    .build();

            ExpenseFormInitResponse response = ExpenseFormInitResponse.builder()
                    .currentMemberId(expectedMemberId)
                    .defaultAccount(accountInfo)
                    .roomMembers(List.of())
                    .build();

            // ExpenseService Mocking
            given(expenseService.getExpenseFormInit(slug, expectedMemberId)).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/rooms/{slug}/expenses/new", slug)
                            .header("Authorization", "Bearer " + mockToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.currentMemberId").value(expectedMemberId))
                    .andExpect(jsonPath("$.data.defaultAccount.bankName").value("카카오뱅크"))
                    .andExpect(jsonPath("$.data.defaultAccount.accountNumber").value("3333-12-345678"))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("POST /api/rooms/{slug}/expenses - 지출 일괄 등록")
    class CreateExpenses {

        @Test
        @DisplayName("성공: 올바른 지출 일괄 등록 요청 시 200 OK를 반환한다")
        void createExpensesSuccess() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("저녁 식사")
                    .amount(new BigDecimal("10000"))
                    .targetMemberIds(List.of(100L, 101L))
                    .build();

            ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                    .payerId(100L)
                    .spentAt(LocalDateTime.now())
                    .currency("KRW")
                    .bankName("카카오뱅크")
                    .accountNumber("3333-12-345678")
                    .items(List.of(item))
                    .build();

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of(group))
                    .build();

            willDoNothing().given(expenseService).createExpenses(eq(slug), eq(expectedMemberId), any(ExpenseBatchCreateRequest.class));

            // when & then
            mockMvc.perform(post("/api/rooms/{slug}/expenses", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(201))
                    .andExpect(jsonPath("$.message").value("SUCCESS"))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 요청 바디 Validation 검증 실패(필수값 누락) 시 400 Bad Request를 반환한다")
        void createExpensesValidationError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            // 필수 목록(expenseGroups)이 비어있는 유효하지 않은 요청 객체
            ExpenseBatchCreateRequest invalidRequest = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of())
                    .build();

            // when & then
            mockMvc.perform(post("/api/rooms/{slug}/expenses", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 지출 등록 시 targetMemberIds에 중복된 멤버 ID가 포함되어 있으면 400 Bad Request를 반환한다")
        void createExpenses_ThrowExceptionWhenDuplicateTargetMemberIds() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseBatchCreateRequest.ExpenseItemRequest duplicateItem = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("저녁 식사")
                    .amount(new BigDecimal("10000"))
                    .targetMemberIds(List.of(100L, 100L, 101L)) // 중복 ID 포함
                    .build();

            ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                    .payerId(100L)
                    .spentAt(LocalDateTime.now())
                    .currency("KRW")
                    .bankName("카카오뱅크")
                    .accountNumber("3333-12-345678")
                    .items(List.of(duplicateItem))
                    .build();

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of(group))
                    .build();

            // when & then
            mockMvc.perform(post("/api/rooms/{slug}/expenses", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.data[0].reason").value("참여자 목록에 중복된 멤버가 존재합니다."))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 방 소속이 아닌 참여자 ID(IDOR)로 요청 시 400 Bad Request를 반환한다")
        void createExpensesMemberNotInRoomError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;
            Long outsideMemberId = 999L; // 타 방 멤버 ID

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("저녁 식사")
                    .amount(new BigDecimal("10000"))
                    .targetMemberIds(List.of(100L, outsideMemberId))
                    .build();

            ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                    .payerId(100L)
                    .spentAt(LocalDateTime.now())
                    .currency("KRW")
                    .bankName("카카오뱅크")
                    .accountNumber("3333-12-345678")
                    .items(List.of(item))
                    .build();

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of(group))
                    .build();

            // 서비스에서 방 소속 검증 실패 예외를 던지도록 설정
            willThrow(new IllegalArgumentException("해당 방에 속하지 않은 참여자가 포함되어 있습니다."))
                    .given(expenseService).createExpenses(eq(slug), eq(expectedMemberId), any(ExpenseBatchCreateRequest.class));

            // when & then
            mockMvc.perform(post("/api/rooms/{slug}/expenses", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 은행명에 특수문자나 숫자가 포함되는 등 유효하지 않은 포맷이면 400 Bad Request를 반환한다")
        void createExpenses_InvalidBankName_Returns400() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("저녁 식사")
                    .amount(new BigDecimal("10000"))
                    .targetMemberIds(List.of(100L, 101L))
                    .build();

            ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                    .payerId(100L)
                    .spentAt(LocalDateTime.now())
                    .currency("KRW")
                    .bankName("카카오123!") // 잘못된 은행명 포맷
                    .accountNumber("3333-12-345678")
                    .items(List.of(item))
                    .build();

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of(group))
                    .build();

            // when & then
            mockMvc.perform(post("/api/rooms/{slug}/expenses", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(400))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 계좌번호에 숫자가 전혀 없으면 400 Bad Request를 반환한다")
        void createExpenses_NoDigitsInAccountNumber_Returns400() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseBatchCreateRequest.ExpenseItemRequest item = ExpenseBatchCreateRequest.ExpenseItemRequest.builder()
                    .title("저녁 식사")
                    .amount(new BigDecimal("10000"))
                    .targetMemberIds(List.of(100L, 101L))
                    .build();

            ExpenseBatchCreateRequest.ExpenseGroupRequest group = ExpenseBatchCreateRequest.ExpenseGroupRequest.builder()
                    .payerId(100L)
                    .spentAt(LocalDateTime.now())
                    .currency("KRW")
                    .bankName("카카오뱅크")
                    .accountNumber("문의요망") // 숫자가 없는 계좌번호
                    .items(List.of(item))
                    .build();

            ExpenseBatchCreateRequest request = ExpenseBatchCreateRequest.builder()
                    .expenseGroups(List.of(group))
                    .build();

            // when & then
            mockMvc.perform(post("/api/rooms/{slug}/expenses", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(400))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("GET /api/rooms/{slug}/expenses - 지출 목록 조회")
    class GetExpenseList {

        @Test
        @DisplayName("성공: 올바른 slug 및 토큰으로 지출 목록 조회 요청 시 200 OK를 반환한다")
        void getExpenseListSuccess() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseListResponse.ExpenseItemResponse expenseItem = ExpenseListResponse.ExpenseItemResponse.builder()
                    .expenseId(1L)
                    .title("점심 돈까스")
                    .amount(new BigDecimal("30000"))
                    .payerName("스펀지밥")
                    .isMyPayment(true)
                    .targetMemberCount(2)
                    .build();

            ExpenseListResponse response = ExpenseListResponse.builder()
                    .roomTitle("제주도 여행 방")
                    .currentMemberName("스펀지밥")
                    .isLocked(false)
                    .totalExpenseAmount(new BigDecimal("30000"))
                    .settlementStatus(ExpenseListResponse.SettlementStatus.PENDING)
                    .mySettlementAmount(null)
                    .expenses(List.of(expenseItem))
                    .build();

            given(expenseService.getExpenseList(eq(slug), eq(expectedMemberId))).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/rooms/{slug}/expenses", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.roomTitle").value("제주도 여행 방"))
                    .andExpect(jsonPath("$.data.currentMemberName").value("스펀지밥"))
                    .andExpect(jsonPath("$.data.isLocked").value(false))
                    .andExpect(jsonPath("$.data.totalExpenseAmount").value(30000))
                    .andExpect(jsonPath("$.data.settlementStatus").value("PENDING"))
                    .andExpect(jsonPath("$.data.expenses[0].title").value("점심 돈까스"))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 방 소속이 아닌 참여자 ID(IDOR)로 지출 목록 조회 시 400 Bad Request를 반환한다")
        void getExpenseListMemberNotInRoomError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willThrow(new IllegalArgumentException("해당 방에 속하지 않은 참여자입니다."))
                    .given(expenseService).getExpenseList(eq(slug), eq(expectedMemberId));

            // when & then
            mockMvc.perform(get("/api/rooms/{slug}/expenses", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("GET /api/rooms/{slug}/expenses/{expenseId} - 지출 단건 상세 조회")
    class GetExpenseDetail {

        @Test
        @DisplayName("성공: 올바른 slug 및 expenseId 요청 시 200 OK와 상세 데이터를 반환한다")
        void getExpenseDetailSuccess() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expenseId = 1L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseDetailResponse.TargetMemberDetail targetMember = ExpenseDetailResponse.TargetMemberDetail.builder()
                    .memberId(expectedMemberId)
                    .name("스펀지밥")
                    .shareAmount(new BigDecimal("15000"))
                    .isSelf(true)
                    .build();

            ExpenseDetailResponse response = ExpenseDetailResponse.builder()
                    .expenseId(expenseId)
                    .title("점심 돈까스")
                    .amount(new BigDecimal("30000"))
                    .currency("KRW")
                    .fxRate(BigDecimal.ONE)
                    .spentAt(LocalDateTime.of(2026, 9, 9, 18, 30))
                    .payerId(expectedMemberId)
                    .payerName("스펀지밥")
                    .bankName("카카오뱅크")
                    .accountNumber("3333-12-3456789")
                    .isMyPayment(true)
                    .targetMembers(List.of(targetMember))
                    .build();

            given(expenseService.getExpenseDetail(eq(slug), eq(expenseId), eq(expectedMemberId)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/rooms/{slug}/expenses/{expenseId}", slug, expenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.expenseId").value(expenseId))
                    .andExpect(jsonPath("$.data.title").value("점심 돈까스"))
                    .andExpect(jsonPath("$.data.isMyPayment").value(true))
                    .andExpect(jsonPath("$.data.targetMembers[0].name").value("스펀지밥"))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 존재하지 않는 지출 ID이거나 타 방 지출(IDOR) 접근 시 400 Bad Request를 반환한다")
        void getExpenseDetailNotFoundError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long invalidExpenseId = 999L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willThrow(new IllegalArgumentException("해당 지출 내역이 존재하지 않습니다."))
                    .given(expenseService).getExpenseDetail(eq(slug), eq(invalidExpenseId), eq(expectedMemberId));

            // when & then
            mockMvc.perform(get("/api/rooms/{slug}/expenses/{expenseId}", slug, invalidExpenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("해당 지출 내역이 존재하지 않습니다."))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("GET /api/rooms/{slug}/expenses/{expenseId}/edit - 지출 수정 폼 데이터 조회")
    class GetExpenseUpdateForm {

        @Test
        @DisplayName("성공: 올바른 slug 및 expenseId 요청 시 200 OK와 수정 폼 데이터를 반환한다")
        void getExpenseUpdateFormSuccess() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expenseId = 1L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseUpdateFormResponse.MemberInfo roomMember1 = ExpenseUpdateFormResponse.MemberInfo.builder()
                    .memberId(100L)
                    .name("스펀지밥")
                    .isActive(true)
                    .build();

            ExpenseUpdateFormResponse.MemberInfo roomMember2 = ExpenseUpdateFormResponse.MemberInfo.builder()
                    .memberId(101L)
                    .name("뚱이")
                    .isActive(false)
                    .build();

            ExpenseUpdateFormResponse response = ExpenseUpdateFormResponse.builder()
                    .expenseId(expenseId)
                    .title("점심 돈까스")
                    .amount(new BigDecimal("30000"))
                    .currency("KRW")
                    .spentAt(LocalDateTime.of(2026, 9, 9, 18, 30))
                    .payerId(expectedMemberId)
                    .targetMemberIds(List.of(100L, 101L))
                    .roomMembers(List.of(roomMember1, roomMember2))
                    .build();

            given(expenseService.getExpenseUpdateForm(eq(slug), eq(expenseId), eq(expectedMemberId)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(get("/api/rooms/{slug}/expenses/{expenseId}/edit", slug, expenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.expenseId").value(expenseId))
                    .andExpect(jsonPath("$.data.title").value("점심 돈까스"))
                    .andExpect(jsonPath("$.data.amount").value(30000))
                    .andExpect(jsonPath("$.data.payerId").value(expectedMemberId))
                    .andExpect(jsonPath("$.data.targetMemberIds[0]").value(100))
                    .andExpect(jsonPath("$.data.targetMemberIds[1]").value(101))
                    .andExpect(jsonPath("$.data.roomMembers[0].name").value("스펀지밥"))
                    .andExpect(jsonPath("$.data.roomMembers[0].isActive").value(true))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 존재하지 않는 지출 ID이거나 마감/잠금 처리된 방 접근 시 400 Bad Request를 반환한다")
        void getExpenseUpdateFormBadRequestError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long invalidExpenseId = 999L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willThrow(new IllegalArgumentException("해당 지출 내역이 존재하지 않습니다."))
                    .given(expenseService).getExpenseUpdateForm(eq(slug), eq(invalidExpenseId), eq(expectedMemberId));

            // when & then
            mockMvc.perform(get("/api/rooms/{slug}/expenses/{expenseId}/edit", slug, invalidExpenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("해당 지출 내역이 존재하지 않습니다."))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("PUT /api/rooms/{slug}/expenses/{expenseId} - 지출 내역 수정")
    class UpdateExpense {

        @Test
        @DisplayName("성공: 올바른 요청 바디와 토큰으로 요청 시 200 OK를 반환한다")
        void updateExpenseSuccess() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expenseId = 1L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseUpdateRequest request = ExpenseUpdateRequest.builder()
                    .payerId(expectedMemberId)
                    .title("수정된 저녁 식사")
                    .amount(new BigDecimal("50000"))
                    .spentAt(LocalDateTime.of(2026, 9, 17, 19, 0))
                    .targetMemberIds(List.of(100L, 101L))
                    .build();

            willDoNothing().given(expenseService).updateExpense(eq(slug), eq(expenseId), eq(expectedMemberId), any(ExpenseUpdateRequest.class));

            // when & then
            mockMvc.perform(put("/api/rooms/{slug}/expenses/{expenseId}", slug, expenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("SUCCESS"))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 요청 바디 Validation 실패 (필수 항목 누락) 시 400 Bad Request를 반환한다")
        void updateExpenseValidationError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expenseId = 1L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            // 필수값(title, amount 등)이 누락된 유효하지 않은 DTO
            ExpenseUpdateRequest invalidRequest = ExpenseUpdateRequest.builder()
                    .payerId(expectedMemberId)
                    .title("")
                    .amount(new BigDecimal("0"))
                    .spentAt(null)
                    .targetMemberIds(List.of())
                    .build();

            // when & then
            mockMvc.perform(put("/api/rooms/{slug}/expenses/{expenseId}", slug, expenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 이미 정산 마감되었거나 존재하지 않는 지출 수정 시 400 Bad Request를 반환한다")
        void updateExpenseBadRequestError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long invalidExpenseId = 999L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseUpdateRequest request = ExpenseUpdateRequest.builder()
                    .payerId(expectedMemberId)
                    .title("수정된 저녁 식사")
                    .amount(new BigDecimal("50000"))
                    .spentAt(LocalDateTime.of(2026, 9, 17, 19, 0))
                    .targetMemberIds(List.of(100L, 101L))
                    .build();

            willThrow(new IllegalArgumentException("이미 정산이 완료된 방의 지출은 수정할 수 없습니다."))
                    .given(expenseService).updateExpense(eq(slug), eq(invalidExpenseId), eq(expectedMemberId), any(ExpenseUpdateRequest.class));

            // when & then
            mockMvc.perform(put("/api/rooms/{slug}/expenses/{expenseId}", slug, invalidExpenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("이미 정산이 완료된 방의 지출은 수정할 수 없습니다."))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 지출 수정 시 targetMemberIds에 중복된 멤버 ID가 포함되어 있으면 400 Bad Request를 반환한다")
        void updateExpense_ThrowExceptionWhenDuplicateTargetMemberIds() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expenseId = 1L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            ExpenseUpdateRequest request = ExpenseUpdateRequest.builder()
                    .payerId(expectedMemberId)
                    .title("저녁 식사")
                    .amount(new BigDecimal("30000"))
                    .spentAt(LocalDateTime.now())
                    .targetMemberIds(List.of(100L, 100L, 101L)) // 중복 ID 포함
                    .build();

            // when & then
            mockMvc.perform(put("/api/rooms/{slug}/expenses/{expenseId}", slug, expenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.data[0].field").value("validTargetMemberIds"))
                    .andExpect(jsonPath("$.data[0].reason").value("참여자 목록에 중복된 멤버가 존재합니다."))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("DELETE /api/rooms/{slug}/expenses/{expenseId} - 지출 내역 삭제")
    class DeleteExpense {

        @Test
        @DisplayName("성공: 정상적인 지출 삭제 요청 시 200 OK를 반환한다")
        void deleteExpenseSuccess() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expenseId = 1L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willDoNothing().given(expenseService).deleteExpense(eq(slug), eq(expenseId), eq(expectedMemberId));

            // when & then
            mockMvc.perform(delete("/api/rooms/{slug}/expenses/{expenseId}", slug, expenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("SUCCESS"))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 마감된 방이거나 존재하지 않는 지출 삭제 시 400 Bad Request를 반환한다")
        void deleteExpenseBadRequestError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long invalidExpenseId = 999L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willThrow(new IllegalArgumentException("해당 방에 존재하지 않는 지출이거나 이미 삭제된 지출입니다."))
                    .given(expenseService).deleteExpense(eq(slug), eq(invalidExpenseId), eq(expectedMemberId));

            // when & then
            mockMvc.perform(delete("/api/rooms/{slug}/expenses/{expenseId}", slug, invalidExpenseId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("해당 방에 존재하지 않는 지출이거나 이미 삭제된 지출입니다."))
                    .andDo(print());
        }
    }
}