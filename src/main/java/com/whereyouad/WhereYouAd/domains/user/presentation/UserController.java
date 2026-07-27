package com.whereyouad.WhereYouAd.domains.user.presentation;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.*;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.*;
import com.whereyouad.WhereYouAd.domains.user.domain.constant.Provider;
import com.whereyouad.WhereYouAd.domains.user.domain.service.EmailService;
import com.whereyouad.WhereYouAd.domains.user.domain.service.SmsService;
import com.whereyouad.WhereYouAd.domains.user.domain.service.UserService;
import com.whereyouad.WhereYouAd.domains.user.presentation.docs.UserControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController implements UserControllerDocs {

    private final UserService userService;
    private final EmailService emailService;
    private final SmsService smsService;

    @Value("${cookie.secure}")
    private boolean cookieSecure;

    @Value("${cookie.domain:}")
    private String cookieDomain;

    @Value("${cookie.same-site}")
    private String cookieSameSite;

    @PostMapping("/signup")
    public ResponseEntity<DataResponse<SignUpResponse>> signUp(@RequestBody @Valid SignUpRequest request) {
        SignUpResponse signUpResponse = userService.signUpUser(request);
        return ResponseEntity.ok(
                DataResponse.created(signUpResponse)
        );
    }

    @PostMapping("/email-send")
    public ResponseEntity<DataResponse<EmailSentResponse>> sendEmail(@RequestBody @Valid EmailRequest.Send request) {
        EmailSentResponse emailSentResponse = emailService.sendEmail(request.email());
        return ResponseEntity.ok(
                DataResponse.from(emailSentResponse)
        );
    }

    @PostMapping("/email-verify")
    public ResponseEntity<DataResponse<String>> verifyEmail(@RequestBody @Valid EmailRequest.Verify request) {
        emailService.verifyEmailCode(request.email(), request.authCode());

        return ResponseEntity.ok(
                DataResponse.from("이메일 인증이 성공적으로 완료되었습니다."));
    }

    @PostMapping("/sms-send")
    public ResponseEntity<DataResponse<SmsResponse.SmsSentResponse>> sendSms(
            @RequestBody @Valid SmsRequest.SmsSendRequest request) {
        SmsResponse.SmsSentResponse smsSentResponse = smsService.sendSms(request.phoneNumber());
        return ResponseEntity.ok(DataResponse.from(smsSentResponse));
    }

    @PostMapping("/sms-verify")
    public ResponseEntity<DataResponse<SmsResponse.SmsVerifiedResponse>> verifySms(
            @RequestBody @Valid SmsRequest.SmsVerifyRequest request) {
        String email = smsService.isPhoneVerified(request.phoneNumber(), request.verificationCode());
        return ResponseEntity.ok(DataResponse.from(new SmsResponse.SmsVerifiedResponse(true, "이메일 찾기 성공", email)));
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<DataResponse<PasswordResetResponse>> sendEmailForPwdReset(@RequestBody @Valid EmailRequest.Send request) {
        PasswordResetResponse response = emailService.sendEmailForPwd(request.email());

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<DataResponse<String>> resetPassword(@RequestBody @Valid PwdResetRequest request) {
        userService.passwordReset(request.email(), request.password());

        return ResponseEntity.ok(
                DataResponse.from("비밀번호 변경이 완료되었습니다.")
        );
    }

    @GetMapping("/my")
    public ResponseEntity<DataResponse<MyPageResponse>> getMyPage(@AuthenticationPrincipal CustomUserDetails userDetails) {

        MyPageResponse response = userService.getMyPage(
                userDetails.getUserId(),
                userDetails.getProvider().name()
        );

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @PatchMapping("/my")
    public ResponseEntity<DataResponse<UserInfoModifiedResponse>> modifyUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart(value = "request") UserInfoModifyRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    )
    {
        Long userId = userDetails.getUserId();
        Provider provider = userDetails.getProvider();
        UserInfoModifiedResponse response = userService.modifyUserInfo(userId, provider, request, image);

        return ResponseEntity.ok(
                DataResponse.from(response)
        );
    }

    @DeleteMapping("/my")
    public ResponseEntity<DataResponse<String>> deleteUser(
            @AuthenticationPrincipal(expression = "userId") Long userId
    )
    {
        userService.deleteUser(userId);

        // 로그인 때 사용한 경로·도메인 설정과 동일한 만료 쿠키를 내려 브라우저의 인증 정보를 제거한다.
        ResponseCookie expiredRefresh = baseCookie("refresh_token", "")
                .httpOnly(true)
                .maxAge(0)
                .build();
        ResponseCookie expiredAccess = baseCookie("access_token", "")
                .httpOnly(false)
                .maxAge(0)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredRefresh.toString())
                .header(HttpHeaders.SET_COOKIE, expiredAccess.toString())
                .body(DataResponse.from("탈퇴가 정상적으로 처리되었습니다"));
    }

    // 로그인 쿠키와 같은 보안 설정을 적용해야 기존 쿠키를 정확히 만료시킬 수 있다.
    private ResponseCookie.ResponseCookieBuilder baseCookie(String name, String value) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .secure(cookieSecure)
                .path("/")
                .sameSite(cookieSameSite);
        if (StringUtils.hasText(cookieDomain)) {
            builder.domain(cookieDomain);
        }
        return builder;
    }
}
