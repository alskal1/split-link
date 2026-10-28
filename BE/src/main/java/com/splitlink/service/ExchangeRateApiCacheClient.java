package com.splitlink.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.splitlink.dto.response.FrankfurterApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateApiCacheClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * 특정 일자의 정상적인 환율 응답만 캐시에 저장 (null 결과는 캐싱 제외)
     */
    @Cacheable(
            value = "historicalExchangeRates",
            key = "#from + '_' + #to + '_' + #cacheKeyDate",
            unless = "#result == null"  // null 결과는 캐시에 저장하지 않음 (캐시 오염 방지)
    )
    public BigDecimal fetchHistoricalRate(String datePath, String cacheKeyDate, String from, String to) {
        log.info("========== [CACHE MISS] 캐시에 데이터가 없어 실제 API 호출 진행 -> DatePath: {}, KeyDate: {}, From: {}, To: {} ==========",
                datePath, cacheKeyDate, from, to);
        return fetchRateFromApi(datePath, from, to);
    }

    /**
     * Frankfurter API 실제 HTTP 호출 및 JSON 파싱 (캐시 어노테이션 없음)
     */
    public BigDecimal fetchRateFromApi(String datePath, String from, String to) {
        String url = String.format("https://api.frankfurter.dev/v1/%s?from=%s&to=%s",
                datePath,
                from.trim().toUpperCase(),
                to.trim().toUpperCase());

        try {
            log.info("[Frankfurter API 호출 시도] URL: {}", url);

            ResponseEntity<String> response = restClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .header("User-Agent", "Mozilla/5.0")
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        log.warn("[Frankfurter API HTTP 에러 응답] Status: {}, URL: {}", res.getStatusCode(), url);
                    })
                    .toEntity(String.class);

            // 2xx 성공 응답 처리
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                FrankfurterApiResponse apiResponse = objectMapper.readValue(response.getBody(), FrankfurterApiResponse.class);
                if (apiResponse != null && apiResponse.getRates() != null && apiResponse.getRates().containsKey(to.toUpperCase())) {
                    return apiResponse.getRates().get(to.toUpperCase());
                } else {
                    log.warn("[Frankfurter API Rates 데이터 없음] Response Body: {}", response.getBody());
                }
            }

            // 404 (Not Found)인 경우: 데이터 없음으로 간주하여 null 반환
            if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                log.warn("[Frankfurter API 데이터 없음 (404)] URL: {}", url);
                return null;
            }

            // 그 외 4xx / 5xx 에러: 일시적 외부 시스템 장애로 판단하여 예외 발생
            throw new IllegalStateException("Frankfurter API 호출 실패 (HTTP Status: " + response.getStatusCode() + ")");

        } catch (IllegalStateException e) {
            // 위에서 던진 4xx/5xx IllegalStateException은 그대로 상위로 던짐
            throw e;
        } catch (Exception e) {
            log.error("[Frankfurter API 호출 중 Exception 발생] URL: {}, Error: {}", url, e.getMessage());
            // 일시적 네트워크 타임아웃 / 외부 API 오류 시 예외를 던져 상위로 전달 (null 반환 금지)
            throw new IllegalStateException("외부 환율 API 연동 중 오류가 발생했습니다.", e);
        }
    }
}
