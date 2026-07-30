package com.whereyouad.WhereYouAd.domains.user.domain.service.oauth;

import com.whereyouad.WhereYouAd.global.utils.AESUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class OAuthTokenCryptoService {

    // 암호문 버전을 구분해 추후 키/알고리즘 변경과 기존 데이터 마이그레이션을 지원한다.
    private static final String VERSION_PREFIX = "gcm:v1:";
    // GCM 권장 크기인 96bit nonce와 128bit 인증 태그를 사용한다.
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final AESUtil legacyAesUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthTokenCryptoService(
            @Value("${aes.secret}") String secret,
            AESUtil legacyAesUtil
    ) throws GeneralSecurityException {
        // 환경변수의 길이와 무관하게 AES-256에서 사용할 32byte 키를 만든다.
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(keyBytes, "AES");
        this.legacyAesUtil = legacyAesUtil;
    }

    public String encrypt(String plainText) throws GeneralSecurityException {
        // 같은 토큰도 매번 다른 암호문이 생성되도록 암호화마다 새로운 nonce를 사용한다.
        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        secureRandom.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // nonce는 비밀값이 아니므로 버전 및 암호문과 함께 저장한다.
        return VERSION_PREFIX
                + Base64.getEncoder().encodeToString(nonce)
                + ":"
                + Base64.getEncoder().encodeToString(cipherText);
    }

    public String decrypt(String encryptedText) throws GeneralSecurityException {
        // GCM 도입 전에 AESUtil(CBC)로 저장된 데이터도 읽을 수 있도록 하위 호환한다.
        if (!encryptedText.startsWith(VERSION_PREFIX)) {
            return new String(legacyAesUtil.decryptAES(encryptedText), StandardCharsets.UTF_8);
        }

        String payload = encryptedText.substring(VERSION_PREFIX.length());
        String[] parts = payload.split(":", 2);
        if (parts.length != 2) {
            throw new GeneralSecurityException("Invalid OAuth token ciphertext format");
        }

        try {
            byte[] nonce = Base64.getDecoder().decode(parts[0]);
            byte[] cipherText = Base64.getDecoder().decode(parts[1]);
            if (nonce.length != NONCE_LENGTH_BYTES) {
                throw new GeneralSecurityException("Invalid OAuth token nonce length");
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
            // 인증 태그 검증에 실패하면 doFinal에서 예외가 발생해 변조된 토큰 사용을 차단한다.
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Invalid OAuth token ciphertext encoding", e);
        }
    }
}
