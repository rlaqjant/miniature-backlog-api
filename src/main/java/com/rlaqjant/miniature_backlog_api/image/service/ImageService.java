package com.rlaqjant.miniature_backlog_api.image.service;

import com.rlaqjant.miniature_backlog_api.common.exception.BusinessException;
import com.rlaqjant.miniature_backlog_api.common.exception.ErrorCode;
import com.rlaqjant.miniature_backlog_api.image.domain.Image;
import com.rlaqjant.miniature_backlog_api.image.dto.*;
import com.rlaqjant.miniature_backlog_api.image.repository.ImageRepository;
import com.rlaqjant.miniature_backlog_api.progresslog.domain.ProgressLog;
import com.rlaqjant.miniature_backlog_api.progresslog.repository.ProgressLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 이미지 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ImageService {

    // ObjectKey 형식 패턴: users/{userId}/{UUID}.{ext}
    private static final Pattern VALID_OBJECT_KEY_PATTERN =
            Pattern.compile("^users/\\d+/[a-f0-9-]{36}\\.(png|jpg|jpeg|gif|webp)$");

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String r2BucketName;
    private final ImageRepository imageRepository;
    private final ProgressLogRepository progressLogRepository;

    @Value("${cloudflare.r2.presign-expiration-minutes}")
    private int presignExpirationMinutes;

    @Value("${cloudflare.r2.public-url-base:}")
    private String publicUrlBase;

    /**
     * Presigned URL 발급
     * 클라이언트가 직접 R2에 업로드할 수 있는 URL 생성
     */
    public PresignResponse generatePresignedUrl(PresignRequest request, Long userId) {
        // ContentType에서 확장자 추출
        String extension = getExtensionFromContentType(request.getContentType());

        // 고유한 object key 생성: users/{userId}/{UUID}.{ext}
        String objectKey = generateObjectKey(userId, extension);

        // Presigned PUT URL 생성
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignExpirationMinutes))
                .putObjectRequest(builder -> builder
                        .bucket(r2BucketName)
                        .key(objectKey)
                        .contentType(request.getContentType()))
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String uploadUrl = presignedRequest.url().toString();

        log.info("Presigned URL 발급 완료: objectKey={}", objectKey);

        return PresignResponse.builder()
                .uploadUrl(uploadUrl)
                .objectKey(objectKey)
                .build();
    }

    /**
     * 이미지 메타데이터 저장
     * 클라이언트가 R2 업로드 완료 후 호출
     */
    @Transactional
    public ImageResponse saveImage(ImageCreateRequest request, Long userId) {
        // ObjectKey 검증: 인증된 사용자 ID와 일치하는지 확인
        validateObjectKey(request.getObjectKey(), userId);

        // 진행 로그 존재 확인 및 소유권 검증
        ProgressLog progressLog = progressLogRepository.findById(request.getProgressLogId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PROGRESS_LOG_NOT_FOUND));

        if (!progressLog.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        // ObjectKey에서 파일명 추출 (형식: users/{userId}/{UUID}.{ext})
        String fileName = extractFileNameFromObjectKey(request.getObjectKey());
        String contentType = inferContentType(fileName);

        // 이미지 메타데이터 저장
        Image image = Image.builder()
                .progressLogId(request.getProgressLogId())
                .objectKey(request.getObjectKey())
                .fileName(fileName)
                .contentType(contentType)
                .build();

        Image savedImage = imageRepository.save(image);
        log.info("이미지 메타데이터 저장 완료: imageId={}, progressLogId={}", savedImage.getId(), request.getProgressLogId());

        // 읽기용 presigned URL 생성
        String imageUrl = generateReadPresignedUrl(savedImage.getObjectKey());
        return ImageResponse.from(savedImage, imageUrl);
    }

    /**
     * 진행 로그별 이미지 목록 조회 (비공개용 - presigned URL)
     */
    public List<ImageResponse> getImagesByProgressLogId(Long progressLogId) {
        return getImagesByProgressLogId(progressLogId, false);
    }

    /**
     * 진행 로그별 이미지 목록 조회
     * @param progressLogId 진행 로그 ID
     * @param isPublic true: 공개 URL, false: presigned URL
     */
    public List<ImageResponse> getImagesByProgressLogId(Long progressLogId, boolean isPublic) {
        return imageRepository.findByProgressLogIdOrderByCreatedAtAsc(progressLogId).stream()
                .map(image -> {
                    String imageUrl = isPublic
                            ? generatePublicUrl(image.getObjectKey())
                            : generateReadPresignedUrl(image.getObjectKey());
                    return ImageResponse.from(image, imageUrl);
                })
                .collect(Collectors.toList());
    }

    /**
     * 비공개 이미지용 Presigned URL 생성 (15분 유효)
     */
    public String generateReadPresignedUrl(String objectKey) {
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignExpirationMinutes))
                .getObjectRequest(builder -> builder
                        .bucket(r2BucketName)
                        .key(objectKey))
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        return presignedRequest.url().toString();
    }

    /**
     * 공개 이미지용 공개 URL 생성
     * R2_PUBLIC_URL_BASE 미설정 시 presigned URL로 폴백
     */
    public String generatePublicUrl(String objectKey) {
        if (publicUrlBase == null || publicUrlBase.isBlank()) {
            // 공개 URL 미설정 시 presigned URL로 폴백
            return generateReadPresignedUrl(objectKey);
        }
        return publicUrlBase + "/" + objectKey;
    }

    /**
     * 고유한 Object Key 생성
     * 형식: users/{userId}/{UUID}.{ext}
     */
    private String generateObjectKey(Long userId, String extension) {
        String uuid = UUID.randomUUID().toString();
        return String.format("users/%d/%s.%s", userId, uuid, extension);
    }

    /**
     * ContentType에서 확장자 추출
     */
    private String getExtensionFromContentType(String contentType) {
        return switch (contentType) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            default -> "bin";
        };
    }

    /**
     * ObjectKey 검증
     * - 형식 검증: users/{userId}/{UUID}.{ext}
     * - 소유권 검증: ObjectKey의 userId가 인증된 사용자와 일치하는지 확인
     */
    private void validateObjectKey(String objectKey, Long userId) {
        if (!VALID_OBJECT_KEY_PATTERN.matcher(objectKey).matches()) {
            log.warn("유효하지 않은 ObjectKey 형식: {}", objectKey);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 Object Key 형식입니다.");
        }

        // ObjectKey에서 userId 추출하여 소유권 검증
        // 형식: users/{userId}/{UUID}.{ext}
        String[] parts = objectKey.split("/");
        Long objectKeyUserId = Long.parseLong(parts[1]);

        if (!objectKeyUserId.equals(userId)) {
            log.warn("ObjectKey 소유권 불일치: objectKeyUserId={}, authenticatedUserId={}", objectKeyUserId, userId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 이미지에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * ObjectKey에서 파일명 추출
     * 형식: users/{userId}/{UUID}.{ext} -> {UUID}.{ext}
     */
    private String extractFileNameFromObjectKey(String objectKey) {
        return objectKey.substring(objectKey.lastIndexOf("/") + 1);
    }

    /**
     * R2 오브젝트 단건 삭제 (best-effort)
     * 삭제 실패 시 warn 로그만 남기고 예외를 전파하지 않음
     */
    public void deleteR2Object(String objectKey) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(r2BucketName)
                    .key(objectKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
        } catch (Exception e) {
            log.warn("R2 오브젝트 삭제 실패 (고아 파일 발생): objectKey={}, error={}",
                    objectKey, e.getMessage());
        }
    }

    /**
     * R2 오브젝트 일괄 삭제 (best-effort)
     * 각 오브젝트를 개별 삭제하며 실패 시 로그만 남김
     */
    public void deleteR2Objects(List<String> objectKeys) {
        for (String objectKey : objectKeys) {
            deleteR2Object(objectKey);
        }
        log.info("R2 오브젝트 일괄 삭제 시도 완료: 총 {}건", objectKeys.size());
    }

    /**
     * 파일 확장자에서 Content-Type 추론
     */
    private String inferContentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.endsWith(".png")) {
            return "image/png";
        } else if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerName.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerName.endsWith(".webp")) {
            return "image/webp";
        }
        return "application/octet-stream";
    }
}
