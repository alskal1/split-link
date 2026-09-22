package com.splitlink.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitlink.common.jwt.JwtProvider;
import com.splitlink.common.resolver.AuthMemberArgumentResolver;
import com.splitlink.dto.request.RemittanceStatusUpdateRequest;
import com.splitlink.service.SettlementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SettlementController.class)
@Import(AuthMemberArgumentResolver.class)
public class SettlementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SettlementService settlementService;

    @MockBean
    private JwtProvider jwtProvider;

    @Nested
    @DisplayName("POST /api/rooms/{slug}/settlements - 정산 실행")
    class ExecuteSettlement {

        @Test
        @DisplayName("성공: 올바른 토큰과 slug로 정산 실행 요청 시 200 OK를 반환한다")
        void executeSettlementSuccess() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willDoNothing().given(settlementService).executeSettlement(eq(slug), eq(expectedMemberId));

            // when & then
            mockMvc.perform(post("/api/rooms/{slug}/settlements", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("SUCCESS"))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 이미 마감되거나 정산 실행된 방 접근 시 400 Bad Request를 반환한다")
        void executeSettlementBadRequestError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willThrow(new IllegalArgumentException("이미 정산이 실행되었거나 마감된 방입니다."))
                    .given(settlementService).executeSettlement(eq(slug), eq(expectedMemberId));

            // when & then
            mockMvc.perform(post("/api/rooms/{slug}/settlements", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("이미 정산이 실행되었거나 마감된 방입니다."))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 권한이 없는 사용자가 정산 실행 시 400 Bad Request를 반환한다")
        void executeSettlementUnauthorizedError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long expectedMemberId = 999L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willThrow(new IllegalArgumentException("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다."))
                    .given(settlementService).executeSettlement(eq(slug), eq(expectedMemberId));

            // when & then
            mockMvc.perform(post("/api/rooms/{slug}/settlements", slug)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("해당 방이 존재하지 않거나, 해당 방에 접근 권한이 없습니다."))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("PATCH /api/rooms/{slug}/settlements/{settlementId} - 개별 송금 완료 상태 변경")
    class UpdateRemittanceStatus {

        @Test
        @DisplayName("성공: 올바른 요청 시 송금 상태가 변경되고 200 OK를 반환한다")
        void updateRemittanceStatusSuccess() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long settlementId = 10L;
            Long expectedMemberId = 100L;

            RemittanceStatusUpdateRequest request = new RemittanceStatusUpdateRequest(true);

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willDoNothing().given(settlementService)
                    .updateRemittanceStatus(eq(slug), eq(settlementId), eq(expectedMemberId), any(RemittanceStatusUpdateRequest.class));

            // when & then
            mockMvc.perform(patch("/api/rooms/{slug}/settlements/{settlementId}", slug, settlementId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value(200))
                    .andExpect(jsonPath("$.message").value("SUCCESS"))
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 필수 파라미터(isDone)가 null로 요청되면 400 Bad Request를 반환한다")
        void updateRemittanceStatusNullRequestError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long settlementId = 10L;
            Long expectedMemberId = 100L;

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            // request body에 {"isDone": null} 전송
            String invalidJson = "{\"isDone\": null}";

            // when & then
            mockMvc.perform(patch("/api/rooms/{slug}/settlements/{settlementId}", slug, settlementId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(invalidJson))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        @DisplayName("예외: 권한이 없거나 정산 내역이 존재하지 않는 경우 400 Bad Request를 반환한다")
        void updateRemittanceStatusNotFoundError() throws Exception {
            // given
            String mockToken = "mock.jwt.token";
            String slug = "test-slug";
            Long settlementId = 999L;
            Long expectedMemberId = 100L;

            RemittanceStatusUpdateRequest request = new RemittanceStatusUpdateRequest(true);

            given(jwtProvider.validateToken(mockToken)).willReturn(true);
            given(jwtProvider.getMemberId(mockToken)).willReturn(expectedMemberId);

            willThrow(new IllegalArgumentException("존재하지 않거나 수정 권한이 없는 정산 내역입니다."))
                    .given(settlementService)
                    .updateRemittanceStatus(eq(slug), eq(settlementId), eq(expectedMemberId), any(RemittanceStatusUpdateRequest.class));

            // when & then
            mockMvc.perform(patch("/api/rooms/{slug}/settlements/{settlementId}", slug, settlementId)
                            .header("Authorization", "Bearer " + mockToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.message").value("존재하지 않거나 수정 권한이 없는 정산 내역입니다."))
                    .andDo(print());
        }
    }
}