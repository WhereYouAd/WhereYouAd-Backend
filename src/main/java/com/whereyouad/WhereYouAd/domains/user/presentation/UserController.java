package com.whereyouad.WhereYouAd.domains.user.presentation;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.EmailRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.PwdResetRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.EmailSentResponse;
import com.whereyouad.WhereYouAd.domains.user.domain.service.EmailService;
import com.whereyouad.WhereYouAd.domains.user.domain.service.UserService;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.SignUpRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.domains.user.presentation.docs.UserControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController implements UserControllerDocs {

    private final UserService userService;
    private final EmailService emailService;

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
                DataResponse.from("이메일 인증이 성공적으로 완료되었습니다.")
        );
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<DataResponse<EmailSentResponse>> sendEmailForPwdReset(@RequestBody @Valid EmailRequest.Send request) {
        EmailSentResponse emailSentResponse = emailService.sendEmailForPwd(request.email());

        return ResponseEntity.ok(
                DataResponse.created(emailSentResponse)
        );
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<DataResponse<String>> resetPassword(@RequestBody @Valid PwdResetRequest request) {
        userService.passwordReset(request.email(), request.password());

        return ResponseEntity.ok(
                DataResponse.from("비밀번호 변경이 완료되었습니다.")
        );
    }
}
