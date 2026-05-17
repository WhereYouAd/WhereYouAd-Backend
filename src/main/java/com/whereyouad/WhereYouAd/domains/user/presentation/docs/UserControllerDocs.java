package com.whereyouad.WhereYouAd.domains.user.presentation.docs;

import com.whereyouad.WhereYouAd.domains.user.application.dto.request.*;
import com.whereyouad.WhereYouAd.domains.user.application.dto.response.*;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.global.security.jwt.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

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
                    "테스트용 이메일은 'test' 로 시작하거나 'example.com' 으로 끝나야합니다. 테스트용 이메일의 인증코드는 서버 로그로 확인 가능합니다.\n\n" +
                    "이미 소셜 로그인으로 가입된 이메일 값이 요청으로 들어올 경우, isProviderLinked = true, providerType = [KAKAO, NAVER...] 와 같이 값이 나오며 이메일은 전송되지 않습니다."
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
    public ResponseEntity<DataResponse<PasswordResetResponse>> sendEmailForPwdReset(@RequestBody @Valid EmailRequest.Send request);

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

    @Operation(
            summary = "마이페이지 API",
            description = "Authorization : Bearer \\<AccessToken\\> 을 헤더로 받아 현재 로그인한 회원의 정보를 조회합니다.\n\n" +
                    "회원 DB id,이메일, 이름, 프로필 이미지 URL, 전화번호, 이메일 인증 여부(true / false), 로그인 Provider(EMAIL, KAKAO, NAVER, GOOGLE) 와,\n\n "
                    + "사용자가 속한 조직 정보(각 조직의 Id, 이름, 조직에서의 역할(ADMIN/MEMBER))를 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "해당 사용자 존재하지 않음")
    })
    public ResponseEntity<DataResponse<MyPageResponse>> getMyPage(@AuthenticationPrincipal CustomUserDetails userDetails);

    @Operation(
            summary = "회원 정보 수정 API",
            description = "회원이 수정하려는 이름, 프로필 이미지 파일, 기존 비밀번호와 새로운 비밀번호 값을 입력받아 정보 수정을 진행합니다.\n\n" +
                    "request 에서 boolean 값인 isImageDeleted 를 true 로 하고 image 파일에 null 값을 담아 전송하면 회원 프로필 이미지를 null 값으로 지정하고, isImageDeleted 를 false 로 하고 image 파일에 null 값을 담아 전송하면 기존 프로필 이미지를 유지합니다.\n\n"
                    + "🚨 **[프론트엔드 연동 주의사항]** 🚨\n"
                    + "- 요청 시 반드시 `multipart/form-data` 형식으로 전송해야 합니다.\n"
                    + "- `request` 파트는 단순 문자열이나 객체가 아닌, **`application/json` 타입의 Blob 객체**로 변환하여 append 해야 합니다.\n"
                    + "- `image` 파트는 파일 객체를 그대로 append 합니다. (변경하지 않을 경우 생략 가능)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "해당 사용자 존재하지 않음"),
            @ApiResponse(responseCode = "400", description = " USER_400_5 : 이전 비밀번호와 동일한 비밀번호로 바꿀 수 없습니다.\n\n" +
                    "USER_400_6 : 비밀번호가 일치하지 않습니다.\n\n USER_400_7 : 소셜 로그인 회원은 비밀번호를 변경할 수 없습니다.\n\n" +
                    "USER_400_8 : 비밀번호 변경을 위해선 이전 비밀번호 입력이 필요합니다."),
            @ApiResponse(responseCode = "500", description = "IMAGE_500_1 : S3 서버로의 이미지 업로드에 실패했습니다. \n\n" +
                    "IMAGE_500_2 : S3 서버에서 이미지 삭제를 실패했습니다.")
    })
    public ResponseEntity<DataResponse<UserInfoModifiedResponse>> modifyUserInfo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestPart(value = "request") UserInfoModifyRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    );

    @Operation(
            summary = "회원 탈퇴 API",
            description = "AccessToken 을 헤더로 받아 현재 로그인한 회원을 탈퇴 처리합니다.\n\n" +
                    "### 1. 사전 검증 (통과해야 탈퇴 가능)\n" +
                    "- **광고 플랫폼 연동(PlatformConnection) 존재 여부**: 연동된 광고 플랫폼이 하나라도 남아 있으면 탈퇴가 차단됩니다. 먼저 모든 광고 플랫폼 연동을 해제한 뒤 다시 시도해야 합니다. -> 추후 삭제 API 추가 예정\n\n" +
                    "- **본인이 소유자인 조직에 다른 멤버 존재**: 본인이 생성자(owner)인 조직에 본인 외 다른 멤버가 남아 있으면 탈퇴가 차단됩니다. `PATCH /api/org/{orgId}/changeOwner` API 로 소유권을 위임한 뒤 재시도해야 합니다.\n\n" +
                    "### 2. 회원이 속한 워크스페이스(Organization) 처리\n" +
                    "- **단순 ADMIN / MEMBER 로만 속해있는 조직** → 해당 가입 정보(OrgMember)만 제거됩니다. 조직 자체는 유지됩니다.\n\n" +
                    "- **회원이 생성자(owner)이고 본인만 속한 조직** → 조직이 Soft Delete 처리되며, 해당 조직의 부수 데이터(보낸 초대장 / 활동 타임라인 / AI 인사이트 리포트)도 함께 정리됩니다. 조직 자체는 복구 가능한 상태(status = DELETED) 로 남습니다.\n\n" +
                    "### 3. 회원 본인 데이터 정리\n" +
                    "- 소유자가 아닌채로 속한 조직에서 탈퇴하는 회원 정보가 모두 제거됩니다.\n\n" +
                    "- 소셜 로그인 사용자의 경우 연결된 소셜계정이 함께 제거됩니다.\n\n" +
                    "- 회원 본인이 삭제된 후, 프로필 이미지가 S3 에서 삭제됩니다. S3 이미지 삭제 실패는 서버 로그로만 기록됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "USER_404_1 : 해당 사용자 존재하지 않음"),
            @ApiResponse(responseCode = "400_9", description = "USER_400_9 : 다른 멤버가 속한 조직의 소유자는 탈퇴할 수 없음 (소유권 위임 후 재시도 필요)"),
            @ApiResponse(responseCode = "400_10", description = "USER_400_10 : 연동된 광고 플랫폼이 존재하여 탈퇴할 수 없음 (모든 연동 해제 후 재시도 필요)")
    })
    public ResponseEntity<DataResponse<String>> deleteUser(
            @AuthenticationPrincipal(expression = "userId") Long userId
    );

}
