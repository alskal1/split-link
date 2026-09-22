package com.splitlink.common.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 간편송금 딥링크 URL 생성 유틸리티
 */
@Component
public class RemittanceLinkGenerator {

    /**
     * 토스 송금 딥링크 URL 생성
     * 규격: supertoss://send?bank={은행명}&accountNo={계좌번호}&amount={금액}
     *
     * @param bankName      은행명 (예: "카카오뱅크", "KB국민은행")
     * @param accountNumber 계좌번호 (하이픈 포함 여부 상관없이 숫자만 추출)
     * @param amount        송금 금액
     * @return 토스 딥링크 URL (필수값 누락 시 null 반환)
     */
    public String generateTossLink(String bankName, String accountNumber, BigDecimal amount) {
        // 필수 값 검증 (하나라도 없거나 0 이하 금액이면 딥링크 생성 안 함)
        if (!StringUtils.hasText(bankName) || !StringUtils.hasText(accountNumber) || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        // 은행명 전처리: "국민은행" -> "국민" 등 토스 매핑용 키워드로 정제
        String normalizedBank = normalizeBankName(bankName);

        // 계좌번호에서 하이픈(-) 및 공백 제거 (숫자만 남김)
        String cleanAccount = accountNumber.replaceAll("[^0-9]", "");

        try {
            // 정제된 은행명 URL 인코딩 처리
            String encodedBank = URLEncoder.encode(normalizedBank, StandardCharsets.UTF_8);

            return String.format("supertoss://send?bank=%s&accountNo=%s&amount=%s",
                    encodedBank,
                    cleanAccount,
                    amount.toPlainString());
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 토스 딥링크 인식률 증대를 위한 은행명 보정 로직
     */
    private String normalizeBankName(String bankName) {
        String trimmed = bankName.trim();

        // "국민은행", "신한은행", "농협은행" 등 "~은행"으로 끝나는 경우 "은행" 제거
        // 예: "국민은행" -> "국민", "신한은행" -> "신한"
        // "카카오뱅크", "토스뱅크" 등은 "은행"으로 끝나지 않으므로 그대로 유지됨
        if (trimmed.endsWith("은행") && trimmed.length() > 2) {
            return trimmed.substring(0, trimmed.length() - 2);
        }

        return trimmed;
    }
}
