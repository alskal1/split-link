package com.splitlink.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateApiCacheClient exchangeRateApiCacheClient;

    public BigDecimal getHistoricalExchangeRate(String paymentCurrency, String roomBaseCurrency, LocalDate spentDate) {

        log.info(">>> [ExchangeRateService] 환율 조회 시작 -> Payment: {}, Base: {}, Date: {}", paymentCurrency, roomBaseCurrency, spentDate);

        // 1. 동일 통화 처리 (환율 1.0)
        if (paymentCurrency.equalsIgnoreCase(roomBaseCurrency)) {
            log.info(">>> [ExchangeRateService] 동일 통화이므로 환율 1.0 즉시 반환");
            return BigDecimal.ONE;
        }

        LocalDate europeToday = LocalDate.now(ZoneId.of("Europe/Berlin"));
        String datePath = spentDate.isBefore(europeToday) ? spentDate.toString() : "latest";
        String cacheKeyDate = spentDate.toString(); // 캐시 키는 항상 실제 요청 날짜(YYYY-MM-DD)로 사용
        log.info("europeToday: {}, datePath: {}, cacheKeyDate: {}", europeToday.toString(), datePath, cacheKeyDate);

        // 1차 시도: 요청 날짜 캐시 확인 및 특정 날짜 환율 조회 (성공 시 YYYY-MM-DD 키로 캐시 저장)
        BigDecimal rate = exchangeRateApiCacheClient.fetchHistoricalRate(datePath, cacheKeyDate, paymentCurrency, roomBaseCurrency);
        if (rate != null) {
            log.info(">>> [1차 시도 성공 (Cache Hit 또는 API 성공)] 요청일: {}, 환율: {}", spentDate, rate);
            return rate;
        }

        // 2차 시도: 1차 실패 시 /latest 로 자동 우회 (캐시 저장 안 됨)
        log.warn(">>> [1차 시도 실패 -> 최신 환율(/latest) 우회 시도] 요청일: {}", spentDate);
        BigDecimal fallbackRate = exchangeRateApiCacheClient.fetchRateFromApi("latest", paymentCurrency, roomBaseCurrency);
        if (fallbackRate != null) {
            log.info(">>> [2차 우회 성공 (캐시 저장 안 됨)] 요청일: {}, 환율: {}", spentDate, fallbackRate);
            return fallbackRate;
        }

        throw new IllegalArgumentException("해당 날짜(" + spentDate + ")의 환율 정보를 가져올 수 없습니다.");
    }
}