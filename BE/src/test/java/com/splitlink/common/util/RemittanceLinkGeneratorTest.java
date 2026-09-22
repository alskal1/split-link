package com.splitlink.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class RemittanceLinkGeneratorTest {

    private RemittanceLinkGenerator remittanceLinkGenerator;

    @BeforeEach
    void setUp() {
        remittanceLinkGenerator = new RemittanceLinkGenerator();
    }

    @Test
    @DisplayName("성공: 올바른 파라미터 전달 시 하이픈이 제거되고 은행명이 URL 인코딩된 토스 딥링크를 생성한다.")
    void generateTossLink_Success() {
        // given
        String bankName = "카카오뱅크";
        String accountNumber = "3333-12-345678";
        BigDecimal amount = new BigDecimal("15000");

        // 동적으로 인코딩 기대값 생성
        String expectedEncodedBank = URLEncoder.encode(bankName, StandardCharsets.UTF_8);

        // when
        String result = remittanceLinkGenerator.generateTossLink(bankName, accountNumber, amount);

        // then
        assertThat(result).isNotNull();
        // "카카오뱅크" URL 인코딩 결과: %EC%B9%B4%EC%B9%B4%EC%96%B4%EB%B1%8D%ED%81%AC
        assertThat(result).startsWith("supertoss://send?");
        assertThat(result).contains("bank=" + expectedEncodedBank);
        assertThat(result).contains("accountNo=333312345678"); // 하이픈 제거 확인
        assertThat(result).contains("amount=15000");
    }

    @Test
    @DisplayName("성공: 계좌번호에 공백이나 특수문자가 포함되어 있어도 숫자만 추출하여 딥링크를 생성한다.")
    void generateTossLink_CleanAccountNumber_Success() {
        // given
        String bankName = "KB국민은행";
        String accountNumber = "123 - 456 - 789 012";
        BigDecimal amount = new BigDecimal("50000");

        // when
        String result = remittanceLinkGenerator.generateTossLink(bankName, accountNumber, amount);

        // then
        assertThat(result).contains("accountNo=123456789012");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("예외/경계: 은행명이 null이거나 공백이면 null을 반환한다.")
    void generateTossLink_BlankBankName_ReturnsNull(String bankName) {
        // when
        String result = remittanceLinkGenerator.generateTossLink(bankName, "123456789", new BigDecimal("10000"));

        // then
        assertThat(result).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("예외/경계: 계좌번호가 null이거나 공백이면 null을 반환한다.")
    void generateTossLink_BlankAccountNumber_ReturnsNull(String accountNumber) {
        // when
        String result = remittanceLinkGenerator.generateTossLink("신한은행", accountNumber, new BigDecimal("10000"));

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("예외/경계: 송금 금액이 null이거나 0원 이하(0, 음수)이면 null을 반환한다.")
    void generateTossLink_InvalidAmount_ReturnsNull() {
        // null 금액
        assertThat(remittanceLinkGenerator.generateTossLink("신한은행", "123456", null)).isNull();

        // 0원
        assertThat(remittanceLinkGenerator.generateTossLink("신한은행", "123456", BigDecimal.ZERO)).isNull();

        // 음수 금액
        assertThat(remittanceLinkGenerator.generateTossLink("신한은행", "123456", new BigDecimal("-1000"))).isNull();
    }
}