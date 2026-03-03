package com.whereyouad.WhereYouAd.domains.image.presentation.docs;

import com.whereyouad.WhereYouAd.domains.image.application.dto.response.ImageResponse;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

public interface ImageControllerDocs {

    @Operation(
            summary = "AWS S3 - 이미지 업로드",
            description = "이미지 URL과 함께 요청, 성공 시 AWS S3 이미지 업로드 및 URL 반환"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공적으로 이미지가 업로드됨"),
            @ApiResponse(responseCode = "400_1", description = "업로드할 파일이 없습니다. (Empty File)"),
            @ApiResponse(responseCode = "400_2", description = "허용되지 않는 확장자입니다. (jpg, jpeg, png, webp만 가능)"),
            @ApiResponse(responseCode = "400_3", description = "파일 용량이 10MB 제한을 초과했습니다."),
            @ApiResponse(responseCode = "500_1", description = "AWS S3 서버 업로드 도중 에러가 발생했습니다.")
    })
    ResponseEntity<DataResponse<ImageResponse.ImageUploadResponse>> uploadFile(@RequestPart("image") MultipartFile image);
}
