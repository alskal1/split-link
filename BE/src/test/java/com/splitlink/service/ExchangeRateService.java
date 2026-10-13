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
            verifyNoInteractions(restClient); // RestClient 호출이 전혀 없어야 함
        }

        @Test
        @DisplayName("성공: 외화(USD -> KRW) 조회 시 Frankfurter API를 호출하여 환율을 정상 반환한다.")
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

            // RestClient Chaining Mocking
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
        @DisplayName("예외: Frankfurter API 응답에 해당 통화 환율 정보가 없는 경우 IllegalArgumentException이 발생한다.")
        void getHistoricalExchangeRate_RateNotFound_ThrowsException() {
            // given
            String fromCurrency = "USD";
            String toCurrency = "ABC"; // 존재하지 않는 통화
            LocalDate date = LocalDate.of(2026, 9, 17);

            FrankfurterApiResponse mockResponse = FrankfurterApiResponse.builder()
                    .amount(BigDecimal.ONE)
                    .base("USD")
                    .date("2026-09-17")
                    .rates(Map.of("EUR", new BigDecimal("0.92")))
                    .build();

            given(restClient.get()).willReturn(requestHeadersUriSpec);
            given(requestHeadersUriSpec.uri(anyString(), any(Object[].class))).willReturn(requestHeadersSpec);
            given(requestHeadersSpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.body(FrankfurterApiResponse.class)).willReturn(mockResponse);

            // when & then
            assertThatThrownBy(() -> exchangeRateService.getHistoricalExchangeRate(fromCurrency, toCurrency, date))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("환율(fx_rate) 정보를 가져올 수 없습니다.");
        }

        @Test
        @DisplayName("예외: 외부 API 호출 실패로 body가 null인 경우 IllegalArgumentException이 발생한다.")
        void getHistoricalExchangeRate_ResponseBodyNull_ThrowsException() {
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
                    .hasMessageContaining("환율(fx_rate) 정보를 가져올 수 없습니다.");
        }
    }
}