package com.whereyouad.WhereYouAd.domains.image.presentation;

import com.whereyouad.WhereYouAd.domains.dashboard.application.dto.response.DashboardResponse;
import com.whereyouad.WhereYouAd.domains.image.application.dto.response.ImageResponse;
import com.whereyouad.WhereYouAd.domains.image.presentation.docs.ImageControllerDocs;
import com.whereyouad.WhereYouAd.global.response.DataResponse;
import com.whereyouad.WhereYouAd.infrastructure.client.aws.s3.S3UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


@RestController
@RequestMapping("/api/images")
@RequiredArgsConstructor
public class ImageController implements ImageControllerDocs {

    private final S3UploadService s3UploadService;

    @PostMapping("/upload")
    public ResponseEntity<DataResponse<ImageResponse.ImageUploadResponse>> uploadFile(@RequestPart("image") MultipartFile image) {
        String imageUrl = s3UploadService.uploadImage(image);
        return ResponseEntity.ok(DataResponse.from(new ImageResponse.ImageUploadResponse(imageUrl)));
    }
}
