package com.rlaqjant.miniature_backlog_api.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드 정의
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통 에러 (1xxx)
    INTERNAL_SERVER_ERROR("E1000", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT_VALUE("E1001", "입력값이 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    METHOD_NOT_ALLOWED("E1002", "지원하지 않는 HTTP 메서드입니다.", HttpStatus.METHOD_NOT_ALLOWED),
    RESOURCE_NOT_FOUND("E1003", "요청한 리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 인증/인가 에러 (2xxx)
    UNAUTHORIZED("E2000", "인증이 필요합니다.", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("E2001", "접근 권한이 없습니다.", HttpStatus.FORBIDDEN),
    INVALID_TOKEN("E2002", "유효하지 않은 토큰입니다.", HttpStatus.UNAUTHORIZED),
    EXPIRED_TOKEN("E2003", "만료된 토큰입니다.", HttpStatus.UNAUTHORIZED),

    // 사용자 에러 (3xxx)
    USER_NOT_FOUND("E3000", "사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    DUPLICATE_EMAIL("E3001", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    INVALID_PASSWORD("E3002", "비밀번호가 올바르지 않습니다.", HttpStatus.BAD_REQUEST),
    ADMIN_DELETE_NOT_ALLOWED("E3003", "관리자 계정은 삭제할 수 없습니다.", HttpStatus.BAD_REQUEST),

    // 미니어처 에러 (4xxx)
    MINIATURE_NOT_FOUND("E4000", "미니어처를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    MINIATURE_ACCESS_DENIED("E4001", "해당 미니어처에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 백로그 아이템 에러 (5xxx)
    BACKLOG_ITEM_NOT_FOUND("E5000", "백로그 항목을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // 진행 로그 에러 (6xxx)
    PROGRESS_LOG_NOT_FOUND("E6000", "진행 로그를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    PROGRESS_LOG_ACCESS_DENIED("E6001", "해당 진행 로그에 접근 권한이 없습니다.", HttpStatus.FORBIDDEN),

    // 이미지 에러 (7xxx)
    IMAGE_NOT_FOUND("E7000", "이미지를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    IMAGE_UPLOAD_FAILED("E7001", "이미지 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;
}
