package com.splitlink.service;

import com.splitlink.dto.response.FrankfurterApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        void getHistoricalExchangeRate_ForeignCurrency_Success() {
            // given
            String fromCurrency = "USD";
            String toCurrency = "KRW";
            LocalDate date = LocalDate.of(2026, 9, 17);

            FrankfurterApiResponse mockResponse = FrankfurterApiResponse.builder()
                    .amount(BigDecimal.ONE)
                    .base("USD")
                    .date("2026-09-17")
                    .rates(Map.of("KRW", new BigDecimal("1350.25")))
                    .build();

            given(restClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.body(FrankfurterApiResponse.class)).willReturn(mockResponse);

            // when
            BigDecimal rate = exchangeRateService.getHistoricalExchangeRate(fromCurrency, toCurrency, date);

            // then
            assertThat(rate).isEqualByComparingTo("1350.25");
            verify(restClient, times(1)).get();
        }

        @Test
        @DisplayName("성공: 주말/공휴일 등으로 1차 날짜 조회가 실패할 경우 /latest 로 2차 우회(Fallback)하여 환율을 반환한다.")
        void getHistoricalExchangeRate_FallbackToLatest_Success() {
            // given
            String fromCurrency = "USD";
            String toCurrency = "KRW";
            LocalDate weekendDate = LocalDate.of(2026, 10, 10); // 주말 토요일

            FrankfurterApiResponse emptyResponse = FrankfurterApiResponse.builder()
                    .rates(Map.of()) // 1차 실패 (공시 데이터 없음)
                    .build();

            FrankfurterApiResponse fallbackResponse = FrankfurterApiResponse.builder()
                    .rates(Map.of("KRW", new BigDecimal("1340.00"))) // 2차 성공
                    .build();

            given(restClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.body(FrankfurterApiResponse.class))
                    .willReturn(emptyResponse)
                    .willReturn(fallbackResponse);

            // when
            BigDecimal rate = exchangeRateService.getHistoricalExchangeRate(fromCurrency, toCurrency, weekendDate);

            // then
            assertThat(rate).isEqualByComparingTo("1340.00");
            verify(restClient, times(2)).get(); // 1차 시도 + 2차 fallback 총 2회 호출 검증
        }

        @Test
        @DisplayName("성공: 유럽 현지 기준 오늘 날짜 요청 시 /latest Endpoint로 바로 우회하여 환율을 조회한다.")
        void getHistoricalExchangeRate_EuropeToday_UsesLatestEndpoint() {
            // given
            String fromCurrency = "USD";
            String toCurrency = "KRW";
            LocalDate europeToday = LocalDate.now(ZoneId.of("Europe/Berlin"));

            FrankfurterApiResponse mockResponse = FrankfurterApiResponse.builder()
                    .rates(Map.of("KRW", new BigDecimal("1355.00")))
                    .build();

            given(restClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.body(FrankfurterApiResponse.class)).willReturn(mockResponse);

            // when
            BigDecimal rate = exchangeRateService.getHistoricalExchangeRate(fromCurrency, toCurrency, europeToday);

            // then
            assertThat(rate).isEqualByComparingTo("1355.00");
            verify(requestHeadersUriSpec).uri(contains("latest"), any(Object[].class));
        }

        @Test
        @DisplayName("예외: 1차/2차 시도 모두 실패 시 IllegalArgumentException 예외가 발생한다.")
        void getHistoricalExchangeRate_AllAttemptsFail_ThrowsException() {
            // given
            String fromCurrency = "USD";
            String toCurrency = "KRW";
            LocalDate date = LocalDate.of(2026, 9, 17);

            given(restClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.body(FrankfurterApiResponse.class)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> exchangeRateService.getHistoricalExchangeRate(fromCurrency, toCurrency, date))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("환율 정보를 가져올 수 없습니다.");
        }
    }
}