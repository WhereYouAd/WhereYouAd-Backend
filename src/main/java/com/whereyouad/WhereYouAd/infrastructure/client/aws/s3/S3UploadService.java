package com.whereyouad.WhereYouAd.infrastructure.client.aws.s3;

import com.whereyouad.WhereYouAd.domains.image.exception.ImageException;
import com.whereyouad.WhereYouAd.domains.image.exception.code.ImageErrorCode;
import io.awspring.cloud.s3.ObjectMetadata;
import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3UploadService {

    private final S3Template s3Template;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile file) {
        // 파일 존재 여부 검증
        if (file == null || file.isEmpty()) {
            throw new ImageException(ImageErrorCode.EMPTY_FILE);
        }

        // 확장자 검증 (이미지만 허용)
        validateExtension(file.getOriginalFilename());

        String originalFilename = file.getOriginalFilename();
        String uniqueFileName = UUID.randomUUID().toString() + "_" + originalFilename;

        ObjectMetadata metadata = ObjectMetadata.builder()
                .contentType(file.getContentType())
                .build();

        try (InputStream inputStream = file.getInputStream()) {
            s3Template.upload(bucket, uniqueFileName, inputStream, metadata);
        } catch (IOException e) {
            // S3 업로드 과정에서 발생하는 에러 처리
            throw new ImageException(ImageErrorCode.IMAGE_UPLOAD_FAILED);
        }

        // S3 객체 URL 반환
        return String.format("https://%s.s3.ap-northeast-2.amazonaws.com/%s", bucket, uniqueFileName);
    }

    private void validateExtension(String filename) {
        String lastDot = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
        List<String> allowedExtensions = Arrays.asList("jpg", "jpeg", "png", "webp");

        if (!allowedExtensions.contains(lastDot)) {
            throw new ImageException(ImageErrorCode.INVALID_FILE_EXTENSION);
        }
    }
}
