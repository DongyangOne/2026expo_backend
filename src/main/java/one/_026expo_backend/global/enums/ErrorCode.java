package one._026expo_backend.global.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 요청입니다."),
    INVALID_LOGIN_ID(HttpStatus.BAD_REQUEST, "INVALID_LOGIN_ID", "아이디는 비어 있을 수 없습니다."),
    DUPLICATE_USER(HttpStatus.CONFLICT, "DUPLICATE_USER", "이미 사용중인 아이디 입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용중인 이메일입니다."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "TERMS_NOT_AGREED", "이용약관에 동의 해 주세요."),
    DELETED_USER(HttpStatus.UNAUTHORIZED, "DELETED_USER", "탈퇴한 회원입니다."),
    SOCIAL_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "SOCIAL_LOGIN_REQUIRED", "소셜 로그인 계정입니다. 해당 로그인 방식을 이용해 주세요."),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "EMAIL_NOT_VERIFIED", "이메일 인증이 완료되지 않았습니다."),
    KAKAO_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "KAKAO_EMAIL_REQUIRED", "카카오 이메일 제공 동의가 필요합니다."),
    KAKAO_LOGIN_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_LOGIN_FAILED", "카카오 로그인 처리에 실패했습니다."),
    GOOGLE_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "GOOGLE_EMAIL_REQUIRED", "구글 이메일 제공 동의가 필요합니다."),
    GOOGLE_LOGIN_FAILED(HttpStatus.BAD_GATEWAY, "GOOGLE_LOGIN_FAILED", "구글 로그인 처리에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),
    REDIS_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_CONNECTION_ERROR", "Redis 서버 연결에 실패했습니다."),
    SSE_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SSE_CONNECTION_ERROR", "SSE 채널 생성에 실패했습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "비밀번호가 일치하지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디/비밀번호가 맞지 않습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    INVALID_QR_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_QR_TOKEN", "유효하지 않은 QR토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "리프레시 토큰이 만료되었습니다."),
    AUTH_CODE_EXPIRED(HttpStatus.GONE, "AUTH_CODE_EXPIRED", "인증 코드가 만료되었습니다."),
    AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_CODE_MISMATCH", "인증 코드가 일치하지 않습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SEND_FAILED", "이메일 발송에 실패했습니다."),
    TOO_MANY_EMAIL_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_EMAIL_REQUESTS", "이메일 인증 요청이 너무 잦습니다. 1분 후에 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
