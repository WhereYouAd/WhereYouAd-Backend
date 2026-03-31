package com.whereyouad.WhereYouAd.global.adapi.dto;

public record AdAuthRequest(
        String method,
        String path,
        String body
) {
    // method, path O, body X
    public static AdAuthRequest forMethodAndPath(String method, String path) {
        return new AdAuthRequest(method, path, null);
    }

    // method, path, body X
    public static AdAuthRequest empty() {
        return new AdAuthRequest(null, null, null);
    }

    // 필요시 추가
}
