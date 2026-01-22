package com.rlaqjant.miniature_backlog_api.image.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Set;

/**
 * 허용된 이미지 Content-Type 검증 Validator
 */
public class AllowedContentTypeValidator implements ConstraintValidator<AllowedContentType, String> {

    // 허용된 이미지 MIME 타입 화이트리스트
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    @Override
    public void initialize(AllowedContentType constraintAnnotation) {
        // 초기화 불필요
    }

    @Override
    public boolean isValid(String contentType, ConstraintValidatorContext context) {
        // null은 @NotBlank에서 처리
        if (contentType == null) {
            return true;
        }
        return ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase());
    }
}
