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

    private static final String VERSION_PREFIX = "gcm:v1:";
    private static final int NONCE_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final AESUtil legacyAesUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthTokenCryptoService(
            @Value("${aes.secret}") String secret,
            AESUtil legacyAesUtil
    ) throws GeneralSecurityException {
        byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));
        this.key = new SecretKeySpec(keyBytes, "AES");
        this.legacyAesUtil = legacyAesUtil;
    }

    public String encrypt(String plainText) throws GeneralSecurityException {
        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        secureRandom.nextBytes(nonce);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, nonce));
        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        return VERSION_PREFIX
                + Base64.getEncoder().encodeToString(nonce)
                + ":"
                + Base64.getEncoder().encodeToString(cipherText);
    }

    public String decrypt(String encryptedText) throws GeneralSecurityException {
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
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new GeneralSecurityException("Invalid OAuth token ciphertext encoding", e);
        }
    }
}
