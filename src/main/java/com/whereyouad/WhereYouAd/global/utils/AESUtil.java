package com.whereyouad.WhereYouAd.global.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Base64;

@Component
@RequiredArgsConstructor
public class AESUtil {

    private static final String ALGORITHM = "AES";
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    @Value("${aes.secret}")
    private String key;

    /**
     * AES로 암호화되어있는 문자열을 복호화합니다.
     * @param encryptString 사용자의 시크릿 키 (AES 암호화된 상태)
     * @return 복호화된 시크릿 키를 반환합니다.
     */
    public byte[] decryptAES(
            String encryptString
    ) throws GeneralSecurityException {
        // 시크릿키 키 세팅 (복호화)
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        SecretKey aesKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(key.getBytes());
        cipher.init(Cipher.DECRYPT_MODE, aesKey, iv);

        // Base64 디코딩 -> AES 복호화
        return cipher.doFinal(Base64.getDecoder().decode(encryptString));
    }

    /**
     * 평문을 AES로 암호화합니다.
     * @param plainText 암호화할 평문
     * @return AES로 암호화한 뒤 Base64로 인코딩한 문자열
     */
    public byte[] encryptAES(
            String plainText
    ) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
        SecretKey aesKey = new SecretKeySpec(key.getBytes(), ALGORITHM);
        IvParameterSpec iv = new IvParameterSpec(key.getBytes());
        cipher.init(Cipher.ENCRYPT_MODE, aesKey, iv);

        // AES 암호화 -> Base64 인코딩
        return Base64.getEncoder().encode(cipher.doFinal(plainText.getBytes()));
    }
}