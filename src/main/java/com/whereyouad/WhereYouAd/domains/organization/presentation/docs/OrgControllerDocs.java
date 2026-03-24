package com.whereyouad.WhereYouAd.domains.organization.presentation.docs;

import com.whereyouad.WhereYouAd.domains.organization.application.dto.request.OrgRequest;
import com.whereyouad.WhereYouAd.domains.organization.application.dto.response.OrgResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

public interface OrgControllerDocs {
    @Operation(
            summary = "조직 생성 API",
            description = "조직 이름, 설명, 로고 이미지 파일을 받아 저장(로그인이 진행된 회원만 가능)\n\n"
                    + "🚨 **[프론트엔드 연동 주의사항]** 🚨\n"
                    + "- 요청 시 반드시 `multipart/form-data` 형식으로 전송해야 합니다.\n"
                    + "- `request` 파트는 단순 문자열이나 객체가 아닌, **`application/json` 타입의 Blob 객체**로 변환하여 append 해야 합니다.\n"
                    + "- `image` 파트는 파일 객체를 그대로 append 합니다. (변경하지 않을 경우 생략 가능)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400_1", description = "조직 이름 중복")
    })
    public ResponseEntity<DataResponse<OrgResponse.Create>> createOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @RequestPart(value = "request") @Valid OrgRequest.Create request,
            @RequestPart(value = "image", required = false) MultipartFile image
    );

    @Operation(
            summary = "내가 속한 조직 전체 조회 API",
            description = "로그인한 회원이 속한 조직들의 DB id, 이름, 설명, 로고URL, 내 역할(ADMIN/MEMBER) 을 반환"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401_3", description = "토큰 없이 접근 시 실패")
    })
    public ResponseEntity<DataResponse<OrgResponse.MyOrganizations>> getMyOrganizations(
            @AuthenticationPrincipal(expression = "userId") Long userId);

    @Operation(
            summary = "조직 하나의 세부정보 조회 API",
            description = "조직 id 를 param 으로 받아 해당 조직의 id, 이름, 설명, 로고URL, 조직 생성 시각 반환"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 값 조직 존재 X"),
            @ApiResponse(responseCode = "410_1", description = "해당 조직은 삭제되었습니다 (Soft Delete)")
    })
    public ResponseEntity<DataResponse<OrgResponse.OrgDetail>> getOrganizationDetail(@PathVariable Long orgId);

    @Operation(
            summary = "회원이 만든 조직 중 Soft Deleted 된 조직 목록 조회",
            description = "해당 회원이 만든 조직 중 status 가 DELETED 인 조직만을 조회합니다. Soft Delete 된 조직 복구를 위해 사용 가능한 API 입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "404_1", description = "회원 존재 X")
    })
    public ResponseEntity<DataResponse<OrgResponse.MyOrganizations>> getSoftDeletedOrganizations(
            @AuthenticationPrincipal(expression = "userId") Long userId
    );

    @Operation(
            summary = "조직 정보 수정 API",
            description = "새로운 조직 이름, 설명, 로고 이미지 파일을 받아 저장(해당 조직을 생성한 회원만 정보 변경 가능)\n\n"
                    + "request 에서 boolean 값인 isImageDeleted 를 true 로 하고 image 파일에 null 값을 담아 전송하면 조직 로고 이미지를 null 값으로 지정하고, isImageDeleted 를 true 로 하고 image 파일에 false 값을 담아 전송하면 기존 조직 로고 이미지를 유지합니다.\n\n"
                    + "🚨 **[프론트엔드 연동 주의사항]** 🚨\n"
                    + "- 요청 시 반드시 `multipart/form-data` 형식으로 전송해야 합니다.\n"
                    + "- `request` 파트는 단순 문자열이나 객체가 아닌, **`application/json` 타입의 Blob 객체**로 변환하여 append 해야 합니다.\n"
                    + "- `image` 파트는 파일 객체를 그대로 append 합니다. (변경하지 않을 경우 생략 가능)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공(변경된 필드 값들과 조직Id, 변경 시각 반환)"),
            @ApiResponse(responseCode = "403_1", description = "허가되지 않은 회원의 요청(조직 생성 회원 X)"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 조직 존재 X")
    })
    public ResponseEntity<DataResponse<OrgResponse.Update>> modifyOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestPart(value = "request") @Valid OrgRequest.Update request,
            @RequestPart(value = "image", required = false) MultipartFile imageFile
    );

    @Operation(
            summary = "조직 복구 API",
            description = "Soft Delete 로 임시삭제한 조직을 다시 활성화 합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403_1", description = "허가되지 않은 회원의 요청(조직 생성 회원 X)"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 조직 존재 X"),
            @ApiResponse(responseCode = "409_1", description = "이미 활성화 상태인 조직")
    })
    public ResponseEntity<DataResponse<OrgResponse.Delete>> restoreOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId
    );


    @Operation(
            summary = "조직 삭제 API",
            description = "조직 Id 를 PathVariable 로 받아 해당 조직 삭제(해당 조직을 생성한 회원만 삭제 가능) \n\n" +
                    "param 인 isHard = true 이면 Hard Delete (DB에서 삭제), isHard = false 이면 Soft Delete (status 만 DELETED 로 변경)\n\n"
                    + "*추가* Hard Delete 시 조직 로고 이미지가 S3 에서 자동 삭제됩니다. Soft Delete 시에는 삭제되지 않습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403_1", description = "허가되지 않은 회원의 요청(조직 생성 회원 X)"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 조직 존재 X")
    })
    public ResponseEntity<DataResponse<String>> removeOrganization(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestParam(defaultValue = "false") boolean isHard
    );

    @Operation(
            summary = "조직 멤버 조회 API (무한 스크롤 - Slice 기반)",
            description = "조직에 속한 멤버를 조회합니다. cursor와 size 파라미터를 통해 무한 스크롤을 지원합니다. cursor는 Base64로 인코딩된 문자열입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 (hasNext: 다음 페이지 존재 여부, nextCursor: 다음 페이지 커서, members: 멤버 리스트)"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 조직 존재 X"),
            @ApiResponse(responseCode = "400", description = "잘못된 커서 형식")
    })
    ResponseEntity<DataResponse<OrgResponse.OrgMemberSliceDTO>> getOrgMembers(
            @PathVariable Long orgId,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size
    );

    @Operation(
            summary = "조직 전체 멤버 수 조회 API",
            description = "조직의 전체 멤버 수를 조회합니다. 무한 스크롤 초기 로딩 시 1회만 호출하는 것을 권장합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 (totalCount: 전체 멤버 수)"),
            @ApiResponse(responseCode = "404_1", description = "해당 id 조직 존재 X")
    })
    ResponseEntity<DataResponse<OrgResponse.OrgMemberCountDTO>> getOrgMembersCount(
            @PathVariable Long orgId
    );

    @Operation(
            summary = "조직 맴버 삭제 API",
            description = "맴버 삭제를 요청한 유저의 권한이 ADMIN인 경우 실행이 가능합니다. memberId에 해당하는 맴버를 조직에서 제외시킵니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "403", description = "권한이 부족한 경우(요청을 보낸 유저의 권한이 ADMIN이 아닌 경우)"),
            @ApiResponse(responseCode = "404", description = "해당 id의 데이터 존재 X")
    })
    public ResponseEntity<DataResponse<String>> removeMember(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long memberId
    );

    @Operation(
            summary = "조직 맴버 권한 변경 API",
            description = "맴버 권한 변경을 요청한 유저의 권한이 ADMIN인 경우 실행이 가능합니다. memberId에 해당하는 맴버의 권한을 변경시킵니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공 (totalCount: 전체 멤버 수)"),
            @ApiResponse(responseCode = "401", description = "잘못된 요청을 보낸 경우(ADMIN의 권한 변경)"),
            @ApiResponse(responseCode = "403", description = "권한이 부족한 경우(요청을 보낸 유저의 권한이 ADMIN이 아닌 경우)"),
            @ApiResponse(responseCode = "404", description = "해당 id의 데이터 존재 X")
    })
    ResponseEntity<DataResponse<OrgResponse.OrgMemberDTO>> updateOrgMembersRole(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @PathVariable Long memberId,
            @RequestBody OrgRequest.UpdateRole dto
    );

    @Operation(summary = "조직 초대 이메일 발송 API", description = "조직 관리자가 이메일을 입력하여 새로운 멤버를 초대합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "403", description = "조직 멤버가 아닌 사용자의 요청"),
            @ApiResponse(responseCode = "404", description = "조직을 찾을 수 없음"),
            @ApiResponse(responseCode = "409", description = "이미 조직에 가입된 사용자")
    })
    public ResponseEntity<DataResponse<OrgResponse.OrgInvitationResponse>> sendOrgInvitation(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable Long orgId,
            @RequestBody @Valid OrgRequest.Invite request
    );

    @Operation(summary = "조직 초대 수락 API", description = "이메일로 받은 초대 토큰을 통해 조직 가입을 수락합니다. (로그인 필수, 본인 확인)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 토큰"),
            @ApiResponse(responseCode = "401", description = "로그인 필요"),
            @ApiResponse(responseCode = "403", description = "초대된 이메일과 로그인한 사용자가 불일치")
    })
    public ResponseEntity<DataResponse<OrgResponse.OrgInvitationResponse>> acceptOrgInvitation(
            @AuthenticationPrincipal(expression = "userId") Long userId,
            @PathVariable String token
    );
}