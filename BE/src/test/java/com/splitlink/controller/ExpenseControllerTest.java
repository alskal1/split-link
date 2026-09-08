package com.splitlink.controller;

import com.splitlink.common.jwt.JwtProvider;
import com.splitlink.common.resolver.AuthMemberArgumentResolver;
import com.splitlink.dto.response.ExpenseFormInitResponse;
import com.splitlink.service.ExpenseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ExpenseController.class)
@Import(AuthMemberArgumentResolver.class)
public class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseService expenseService;

    @MockBean
    private JwtProvider jwtProvider;

    @Test
    @DisplayName("JWT 토큰이 헤더에 포함되면 @AuthMember를 통해 memberId를 추출하고 지출 폼 초기 데이터를 반환한다")
    void getExpenseFormInit_auth_success() throws Exception {
        // given
        String mockToken = "mock.jwt.token";
        String slug = "test-slug";
        Long expectedMemberId = 100L;

        // 1. JwtProvider Mocking (인증 처리 모킹)
        given(jwtProvider.validateToken(mockToken)).willReturn(true);
        given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

        // 2. 컨트롤러가 반환할 가짜 ExpenseFormInitResponse DTO 응답 생성
        ExpenseFormInitResponse.AccountInfo accountInfo = ExpenseFormInitResponse.AccountInfo.builder()
                .bankName("카카오뱅크")
                .accountNumber("3333-12-345678")
                .build();

        ExpenseFormInitResponse response = ExpenseFormInitResponse.builder()
                .currentMemberId(expectedMemberId)
                .defaultAccount(accountInfo)
                .roomMembers(List.of())
                .build();

        // 3. ExpenseService Mocking (서비스 호출 시 생성한 response 객체를 반환하도록 설정)
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