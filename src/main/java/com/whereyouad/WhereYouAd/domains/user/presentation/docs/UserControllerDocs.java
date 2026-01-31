package com.whereyouad.WhereYouAd.domains.user.presentation.docs;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.EmailRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.SmsRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.PwdResetRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.request.SignUpRequest;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.EmailSentResponse;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SmsResponse;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.SignUpResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface UserControllerDocs {
    @Operation(
            summary = "단순 회원가입 API",
            description = "이메일, 비밀번호, 이름, 전화번호를 받아 회원가입을 진행합니다(먼저 이메일 인증이 진행되어야 회원가입 가능)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_1", description = "이메일 중복 회원 존재")
    })
    public ResponseEntity<DataResponse<SignUpResponse>> signUp(@RequestBody @Valid SignUpRequest request);

    @Operation(
            summary = "이메일 인증코드 전송 API",
            description = "입력받은 이메일로 인증코드를 전송합니다. 인증코드 재전송도 해당 API 를 호출합니다.\n\n" +
                    "테스트용 이메일은 'test' 로 시작하거나 'example.com' 으로 끝나야합니다. 테스트용 이메일의 인증코드는 서버 로그로 확인 가능합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_2", description = "이메일 전송실패(이메일 오타 등)")
    })
    public ResponseEntity<DataResponse<EmailSentResponse>> sendEmail(@RequestBody @Valid EmailRequest.Send request);

    @Operation(
            summary = "이메일 인증코드 인증 API",
            description = "이메일과 인증코드를 받아 인증코드가 맞는지 검증합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_3", description = "실패(인증코드 불일치)")
    })
    public ResponseEntity<DataResponse<String>> verifyEmail(@RequestBody @Valid EmailRequest.Verify request);

    @Operation(
            summary = "사용자 비밀번호 재설정을 위한 이메일 인증코드 전송 API",
            description = "회원의 이메일을 입력받아 해당 이메일로 인증코드를 전송합니다(인증코드 검증은 기존 /email-verify 로 진행)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_2", description = "이메일 전송실패(이메일 오타 등)"),
            @ApiResponse(responseCode = "404_1", description = "해당 이메일로 가입한 회원 존재하지 않음")
    })
    public ResponseEntity<DataResponse<EmailSentResponse>> sendEmailForPwdReset(@RequestBody @Valid EmailRequest.Send request);

    @Operation(
            summary = "사용자 비밀번호 재설정 API",
            description = "회원의 이메일과 재설정할 비밀번호를 입력받아 비밀번호를 재설정(이전과 같은 비밀번호 일 경우 예외 발생)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_5", description = "이전 비밀번호와 동일 비밀번호로 변경 불가"),
            @ApiResponse(responseCode = "401_1", description = "이메일 인증 진행되지 않음"),
            @ApiResponse(responseCode = "404_1", description = "해당 이메일로 가입한 회원 존재하지 않음")
    })
    public ResponseEntity<DataResponse<String>> resetPassword(@RequestBody @Valid PwdResetRequest request);

    @Operation(summary = "이메일 찾기 - SMS 인증 번호 전송 API", description = "입력받은 전화번호로 인증 번호를 전송합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "실패")
    })
    public ResponseEntity<DataResponse<SmsResponse.SmsSentResponse>> sendSms(
            @RequestBody @Valid SmsRequest.SmsSendRequest request);

    @Operation(summary = "이메일 찾기 - SMS 인증 번호 확인 API", description = "전화번호와 인증 코드를 받아 맞는지 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "실패")
    })
    public ResponseEntity<DataResponse<SmsResponse.SmsVerifiedResponse>> verifySms(
            @RequestBody @Valid SmsRequest.SmsVerifyRequest request);
}
