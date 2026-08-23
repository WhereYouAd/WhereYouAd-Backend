package com.whereyouad.WhereYouAd.infrastructure.client.webpush;

// Web Push 전송 중 네트워크/서명/암호화 오류. HTTP 상태 코드는 statusCode 로 노출
public class WebPushSendException extends RuntimeException {

    private final int statusCode;

    public WebPushSendException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public WebPushSendException(String message, int statusCode) {
        this(message, statusCode, null);
    }

    public int getStatusCode() {
        return statusCode;
    }
}
