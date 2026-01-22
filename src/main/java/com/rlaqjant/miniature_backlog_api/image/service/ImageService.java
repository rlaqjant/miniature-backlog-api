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
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
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

    // 안전한 파일명 패턴 (Path Traversal 방지)
    private static final Pattern SAFE_FILENAME_PATTERN = Pattern.compile("^[a-zA-Z0-9가-힣._-]+$");

    // ObjectKey 형식 패턴: users/{userId}/{UUID}_{fileName}
    private static final Pattern VALID_OBJECT_KEY_PATTERN =
            Pattern.compile("^users/\\d+/[a-f0-9-]{36}_[a-zA-Z0-9가-힣._-]+$");

    private final S3Presigner s3Presigner;
    private final String r2BucketName;
    private final ImageRepository imageRepository;
    private final ProgressLogRepository progressLogRepository;

    @Value("${cloudflare.r2.presign-expiration-minutes}")
    private int presignExpirationMinutes;

    /**
     * Presigned URL 발급
     * 클라이언트가 직접 R2에 업로드할 수 있는 URL 생성
     */
    public PresignResponse generatePresignedUrl(PresignRequest request, Long userId) {
        // 파일명 2차 검증 (Defense in Depth)
        validateFileName(request.getFileName());

        // 고유한 object key 생성: users/{userId}/{UUID}_{원본파일명}
        String objectKey = generateObjectKey(userId, request.getFileName());

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

        // ObjectKey에서 파일명 추출 (형식: users/{userId}/{UUID}_{fileName})
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

        return ImageResponse.from(savedImage);
    }

    /**
     * 진행 로그별 이미지 목록 조회
     */
    public List<ImageResponse> getImagesByProgressLogId(Long progressLogId) {
        return imageRepository.findByProgressLogIdOrderByCreatedAtAsc(progressLogId).stream()
                .map(ImageResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 고유한 Object Key 생성
     * PRD 형식: users/{userId}/{UUID}_{원본파일명}
     */
    private String generateObjectKey(Long userId, String originalFileName) {
        String uuid = UUID.randomUUID().toString();
        return String.format("users/%d/%s_%s", userId, uuid, originalFileName);
    }

    /**
     * 파일명 검증 (2차 방어, Defense in Depth)
     * Path Traversal 및 허용되지 않은 문자 차단
     */
    private void validateFileName(String fileName) {
        if (!SAFE_FILENAME_PATTERN.matcher(fileName).matches()) {
            log.warn("유효하지 않은 파일명 감지: {}", fileName);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "파일명에 허용되지 않은 문자가 포함되어 있습니다.");
        }
    }

    /**
     * ObjectKey 검증
     * - 형식 검증: users/{userId}/{UUID}_{fileName}
     * - 소유권 검증: ObjectKey의 userId가 인증된 사용자와 일치하는지 확인
     */
    private void validateObjectKey(String objectKey, Long userId) {
        if (!VALID_OBJECT_KEY_PATTERN.matcher(objectKey).matches()) {
            log.warn("유효하지 않은 ObjectKey 형식: {}", objectKey);
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "유효하지 않은 Object Key 형식입니다.");
        }

        // ObjectKey에서 userId 추출하여 소유권 검증
        // 형식: users/{userId}/{UUID}_{fileName}
        String[] parts = objectKey.split("/");
        Long objectKeyUserId = Long.parseLong(parts[1]);

        if (!objectKeyUserId.equals(userId)) {
            log.warn("ObjectKey 소유권 불일치: objectKeyUserId={}, authenticatedUserId={}", objectKeyUserId, userId);
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "해당 이미지에 대한 접근 권한이 없습니다.");
        }
    }

    /**
     * ObjectKey에서 파일명 추출
     * 형식: users/{userId}/{UUID}_{fileName} -> fileName
     */
    private String extractFileNameFromObjectKey(String objectKey) {
        // users/5/aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee_test.png
        String lastPart = objectKey.substring(objectKey.lastIndexOf("/") + 1);
        // aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee_test.png -> test.png
        int underscoreIndex = lastPart.indexOf("_");
        return lastPart.substring(underscoreIndex + 1);
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
