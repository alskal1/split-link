package com.splitlink.common.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 계좌번호 암호화 복호화 유틸리티
 */
@Component
public class EncryptionUtil {

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";

    private final SecretKeySpec keySpec;
    private final IvParameterSpec ivSpec;

    // yml에 정의된 secret-key 주입받음
    public EncryptionUtil(@Value("${encryption.secret-key}") String secretKey) {
        // 비밀키를 32바이트(256비트)로 맞춤
        byte[] keyBytes = new byte[32];
        byte[] b = secretKey.getBytes(StandardCharsets.UTF_8);
        int len = Math.min(b.length, keyBytes.length);
        System.arraycopy(b, 0, keyBytes, 0, len);

        this.keySpec = new SecretKeySpec(keyBytes, "AES");
        // IV(Initial Vector)는 키의 앞 16바이트 활용 (CBC 모드 필수)
        this.ivSpec = new IvParameterSpec(keyBytes, 0, 16);
    }

    /**
     * 계좌번호 평문 -> AES-256 암호화 (Base64 인코딩)
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            return plainText;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("계좌번호 암호화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 암호화된 계좌번호 -> AES-256 복호화 (평문)
     */
    public String decrypt(String cipherText) {
        if (cipherText == null || cipherText.isBlank()) {
            return cipherText;
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decodedBytes = Base64.getDecoder().decode(cipherText);
            byte[] decrypted = cipher.doFinal(decodedBytes);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("계좌번호 복호화 중 오류가 발생했습니다.");
        }
    }
}
