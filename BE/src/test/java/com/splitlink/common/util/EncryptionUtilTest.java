package com.splitlink.common.util;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class EncryptionUtilTest {

    private EncryptionUtil encryptionUtil;

    // 테스트용 32글자 비밀키 설정
    private final String testSecretKey = "12345678901234567890123456789012";

    @BeforeEach
    void setUp() {
        // Spring 환경 전체를 띄우지 않고 직접 객체를 생성하여 빠르게 테스트 수행
        encryptionUtil = new EncryptionUtil(testSecretKey);
    }

    @Test
    @DisplayName("계좌번호 정상 암호화 및 복호화 검증")
    void encryptAndDecryptSuccess() {
        // given
        String rawAccountNumber = "110-123-456789";

        // when (암호화 수행)
        String encryptedAccount = encryptionUtil.encrypt(rawAccountNumber);

        // then (암호화 검증)
        log.info("원본 계좌번호: {}", rawAccountNumber);
        log.info("암호화된 계좌번호: {}", encryptedAccount);

        assertThat(encryptedAccount).isNotNull();
        assertThat(encryptedAccount).isNotEqualTo(rawAccountNumber); // 원본과 달라야 함

        // when (복호화 수행)
        String decryptedAccount = encryptionUtil.decrypt(encryptedAccount);

        // then (복호화 검증)
        log.info("복호화된 계좌번호: {}", decryptedAccount);
        assertThat(decryptedAccount).isEqualTo(rawAccountNumber); // 원본과 같아야 함
    }

    @Test
    @DisplayName("Null 또는 빈 문자열 입력 시 예외 없이 그대로 반환하는지 검증")
    void handleNullOrBlank() {
        // given
        String nullAccount = null;
        String blankAccount = "   ";

        // when
        String encryptedNull = encryptionUtil.encrypt(nullAccount);
        String encryptedBlank = encryptionUtil.encrypt(blankAccount);

        // then
        assertThat(encryptedNull).isNull();
        assertThat(encryptedBlank).isEqualTo(blankAccount);
    }
}
