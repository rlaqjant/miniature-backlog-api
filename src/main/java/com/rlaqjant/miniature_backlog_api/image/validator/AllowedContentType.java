package com.rlaqjant.miniature_backlog_api.image.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * 허용된 이미지 Content-Type 검증 어노테이션
 */
@Documented
@Constraint(validatedBy = AllowedContentTypeValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface AllowedContentType {

    String message() default "허용되지 않은 파일 타입입니다. (허용: image/jpeg, image/png, image/gif, image/webp)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
