package com.whereyouad.WhereYouAd.global.utils.cursor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereyouad.WhereYouAd.global.utils.cursor.dto.CursorData;
import com.whereyouad.WhereYouAd.global.utils.cursor.exception.CursorException;
import com.whereyouad.WhereYouAd.global.utils.cursor.exception.code.CursorErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/*
CursorData 객체 생성
JSON 직렬화 (예: {"id":12345})
Base64 인코딩 (예: eyJpZCI6MTIzNDV9)

디코딩은 위 과정의 역순으로 진행
 */
@Slf4j
public class CursorUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    // 유틸리티 클래스이므로 인스턴스화 방지
    private CursorUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }


    // ID를 Base64 인코딩된 커서 문자열로 변환
    public static String encode(Long id) {

        // 커서 아이디가 없거나 양수가 아닌 경우
        if (id == null || id <= 0) {
            throw new CursorException(CursorErrorCode.ID_NOT_POSITIVE_NUMBER);
        }

        try {
            CursorData cursorData = new CursorData(id);

            // 1. JSON 직렬화
            String json = objectMapper.writeValueAsString(cursorData);

            // 2. Base64 인코딩 (URL-safe, padding 제거)
            String encoded = Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(json.getBytes(StandardCharsets.UTF_8));

            return encoded;

        } catch (JsonProcessingException e) {
            // 커서 인코딩에 실패한 경우
            log.error("Failed to encode cursor for ID: {}", id, e);
            throw new CursorException(CursorErrorCode.ENCODE_ERROR);
        }
    }


    // 인코딩된 커서 문자열을 CursorData 객체로 디코딩
    public static CursorData decode(String encodedCursor) {

        // 커서 값이 없는 경우
        if (encodedCursor == null || encodedCursor.isBlank()) {
            throw new CursorException(CursorErrorCode.EMPTY_CURSOR);
        }

        try {
            // 1. Base64 디코딩
            byte[] decodedBytes = Base64.getUrlDecoder()
                    .decode(encodedCursor.getBytes(StandardCharsets.UTF_8));
            String json = new String(decodedBytes, StandardCharsets.UTF_8);

            // 2. JSON 역직렬화
            CursorData cursorData = objectMapper.readValue(json, CursorData.class);

            return cursorData;

        } catch (IllegalArgumentException | JsonProcessingException e) {
            // Base64 형식이 아니거나 JSON 파싱에 실패한 경우
            log.error("Invalid cursor format: {}", encodedCursor, e);
            throw new CursorException(CursorErrorCode.INVALID_CURSOR_FORMAT);

        } catch (Exception e) {
            // 그 외 디코딩에 실패한 경우
            log.error("Unexpected error while decoding cursor: {}", encodedCursor, e);
            throw new CursorException(CursorErrorCode.DECODE_ERROR);
        }
    }


    // 인코딩된 커서 문자열에서 ID 값을 반환
    public static Long decodeToId(String encodedCursor) {
        return decode(encodedCursor).id();
    }
}
