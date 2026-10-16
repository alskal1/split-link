package com.splitlink.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitlink.dto.response.FrankfurterApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExchangeRateServiceTest {

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    @Mock
    private RestClient restClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    @SuppressWarnings("rawtypes")
    private RestClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Nested
    @DisplayName("과거 환율 조회 (getHistoricalExchangeRate)")
    class GetHistoricalExchangeRateTest {

        @Test
        @DisplayName("성공: 동일한 통화 간(예: KRW -> KRW) 조회 시 외부 API 호출 없이 1.0을 반환한다.")
        void getHistoricalExchangeRate_SameCurrency_ReturnsOne() {
            // given
            String currency = "KRW";
            String baseCurrency = "KRW";
            LocalDate date = LocalDate.of(2026, 9, 17);

            // when
            BigDecimal rate = exchangeRateService.getHistoricalExchangeRate(currency, baseCurrency, date);

            // then
            assertThat(rate).isEqualByComparingTo(BigDecimal.ONE);
            verifyNoInteractions(restClient);
        }

        @Test
        @DisplayName("성공: 과거 외화(USD -> KRW) 조회 시 Frankfurter API를 호출하여 환율을 정상 반환한다.")
        void getHistoricalExchangeRate_ForeignCurrency_Success() throws JsonProcessingException {
            // given
            String fromCurrency = "USD";
            String toCurrency = "KRW";
            LocalDate date = LocalDate.of(2026, 10, 12);

            FrankfurterApiResponse mockResponseDto = FrankfurterApiResponse.builder()
                    .amount(BigDecimal.ONE)
                    .base("USD")
                    .date("2026-10-12")
                    .rates(Map.of("KRW", new BigDecimal("1333.09")))
                    .build();

            String jsonBody = objectMapper.writeValueAsString(mockResponseDto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonBody, headers, HttpStatus.OK);

            given(restClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.accept(any())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
            given(responseSpec.toEntity(String.class)).willReturn(responseEntity);

            // when
            BigDecimal rate = exchangeRateService.getHistoricalExchangeRate(fromCurrency, toCurrency, date);

            // then
            assertThat(rate).isEqualByComparingTo("1333.09");
            verify(restClient, times(1)).get();
        }

        @Test
        @DisplayName("성공: 유럽 현지 기준 오늘 날짜 요청 시 latest Endpoint로 환율을 정상 조회한다.")
        void getHistoricalExchangeRate_EuropeToday_UsesLatestEndpoint() throws JsonProcessingException {
            // given
            String fromCurrency = "USD";
            String toCurrency = "KRW";
            LocalDate europeToday = LocalDate.now(ZoneId.of("Europe/Berlin"));

            FrankfurterApiResponse mockResponseDto = FrankfurterApiResponse.builder()
                    .rates(Map.of("KRW", new BigDecimal("1333.09")))
                    .build();

            String jsonBody = objectMapper.writeValueAsString(mockResponseDto);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonBody, headers, HttpStatus.OK);

            given(restClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(contains("latest"))).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.accept(any())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
            given(responseSpec.toEntity(String.class)).willReturn(responseEntity);

            // when
            BigDecimal rate = exchangeRateService.getHistoricalExchangeRate(fromCurrency, toCurrency, europeToday);

            // then
            assertThat(rate).isEqualByComparingTo("1333.09");
        }

        @Test
        @DisplayName("예외: 1차/2차 시도 모두 실패 시 IllegalArgumentException 예외가 발생한다.")
        void getHistoricalExchangeRate_AllAttemptsFail_ThrowsException() {
            // given
            String fromCurrency = "USD";
            String toCurrency = "KRW";
            LocalDate date = LocalDate.of(2026, 10, 12);

            ResponseEntity<String> errorEntity = new ResponseEntity<>("<html>404 Not Found</html>", HttpStatus.NOT_FOUND);

            given(restClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.accept(any())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.header(anyString(), anyString())).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
            given(responseSpec.toEntity(String.class)).willReturn(errorEntity);

            // when & then
            assertThatThrownBy(() -> exchangeRateService.getHistoricalExchangeRate(fromCurrency, toCurrency, date))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("환율 정보를 가져올 수 없습니다.");
        }
    }
}