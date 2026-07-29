package com.whereyouad.WhereYouAd.domains.user.presentation.docs;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.LoginRequest;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.security.jwt.dto.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@Tag(name = "Auth API", description = "로그인, 로그아웃 API")
public interface AuthControllerDocs {
    @Operation(
            summary = "로그인 API",
            description = "이메일, 비밀번호를 입력받아 로그인 진행, AccessToken 을 body 로 반환 & RefreshToken 은 쿠키로 반환"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401_1", description = "실패")
    })
    public ResponseEntity<DataResponse<TokenResponse>> login(@RequestBody LoginRequest request);

    @Operation(
            summary = "AccessToken 재발급 API",
            description = "AccessToken 만료 시 쿠키에 있는 RefreshToken 을 사용해 AccessToken & RefreshToken 을 재발급"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401_2", description = "실패(RefreshToken 만료, 재로그인 필요)"),
            @ApiResponse(responseCode = "401_3", description = "실패(RefreshToken 옳지 않은 값)")
    })
    public ResponseEntity<DataResponse<TokenResponse>> reIssue(@CookieValue(name = "refresh_token") String refreshToken);

    @Operation(
            summary = "로그아웃 API",
            description = "AccessToken 을 Redis 블랙리스트에 등록(남은 만료시간 TTL)하고 RefreshToken을 DB에서 삭제한 뒤, access_token/refresh_token 쿠키를 만료시킴. 이메일/소셜 로그인 공통.\n\n" +
                    "***프론트 유의점*** 해당 API 는 헤더에 Authorization: Bearer <token> 값이 반드시 필요합니다. 소셜 로그인의 경우 AccessToken 값이 쿠키에 저장되어있어 해당 값을 명시적으로 Authorization 헤더에 첨부해야합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "성공 — 이후 동일 AccessToken 으로는 인증 불가"),
            @ApiResponse(responseCode = "401_2", description = "AccessToken 만료 — reissue 후 재호출 필요"),
            @ApiResponse(responseCode = "401_3", description = "AccessToken 형식이 잘못되었거나 없음 / 이미 로그아웃된 토큰")
    })
    public ResponseEntity<Void> logout(@RequestHeader(name = "Authorization") String authorizationHeader);
}
