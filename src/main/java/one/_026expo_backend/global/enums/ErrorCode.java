package one._026expo_backend.global.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 요청입니다."),
    INVALID_LOGIN_ID(HttpStatus.BAD_REQUEST, "INVALID_LOGIN_ID", "아이디는 비어 있을 수 없습니다."),
    DUPLICATE_USER(HttpStatus.CONFLICT, "DUPLICATE_USER", "이미 사용 중인 아이디입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "이미 사용 중인 이메일입니다."),
    TERMS_NOT_AGREED(HttpStatus.BAD_REQUEST, "TERMS_NOT_AGREED", "이용약관에 동의해 주세요."),
    DELETED_USER(HttpStatus.UNAUTHORIZED, "DELETED_USER", "탈퇴한 회원입니다."),
    DELETED_ADMIN(HttpStatus.UNAUTHORIZED, "DELETED_ADMIN", "탈퇴한 관리자입니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."),
    SOCIAL_LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "SOCIAL_LOGIN_REQUIRED", "소셜 로그인 계정입니다. 해당 로그인 방식을 이용해 주세요."),
    EMAIL_NOT_VERIFIED(HttpStatus.UNAUTHORIZED, "EMAIL_NOT_VERIFIED", "이메일 인증이 완료되지 않았습니다."),
    KAKAO_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "KAKAO_EMAIL_REQUIRED", "카카오 이메일 제공 동의가 필요합니다."),
    KAKAO_LOGIN_FAILED(HttpStatus.BAD_GATEWAY, "KAKAO_LOGIN_FAILED", "카카오 로그인 처리에 실패했습니다."),
    NAVER_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "NAVER_EMAIL_REQUIRED", "네이버 이메일 제공 동의가 필요합니다."),
    NAVER_LOGIN_FAILED(HttpStatus.BAD_GATEWAY, "NAVER_LOGIN_FAILED", "네이버 로그인 처리에 실패했습니다."),
    GOOGLE_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "GOOGLE_EMAIL_REQUIRED", "구글 이메일 제공 동의가 필요합니다."),
    GOOGLE_LOGIN_FAILED(HttpStatus.BAD_GATEWAY, "GOOGLE_LOGIN_FAILED", "구글 로그인 처리에 실패했습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "서버 오류가 발생했습니다."),
    REDIS_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "REDIS_CONNECTION_ERROR", "Redis 서버 연결에 실패했습니다."),
    SSE_CONNECTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SSE_CONNECTION_ERROR", "SSE 채널 생성에 실패했습니다."),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_MISMATCH", "비밀번호가 일치하지 않습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "아이디 또는 비밀번호가 맞지 않습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    INVALID_QR_TOKEN(HttpStatus.BAD_REQUEST, "INVALID_QR_TOKEN", "유효하지 않은 QR토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "EXPIRED_REFRESH_TOKEN", "리프레시 토큰이 만료되었습니다."),
    AUTH_CODE_EXPIRED(HttpStatus.GONE, "AUTH_CODE_EXPIRED", "인증 코드가 만료되었습니다."),
    AUTH_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_CODE_MISMATCH", "인증 코드가 일치하지 않습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SEND_FAILED", "이메일 발송에 실패했습니다."),
    TOO_MANY_EMAIL_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "TOO_MANY_EMAIL_REQUESTS", "이메일 인증 요청이 너무 잦습니다. 1분 후에 다시 시도해 주세요."),
    QUIZ_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_NOT_FOUND", "요청하신 퀴즈를 찾을 수가 없습니다."),
    INVALID_QUIZ_ANSWER(HttpStatus.BAD_REQUEST, "INVALID_QUIZ_ANSWER", "잘못된 형식의 정답을 입력하셨습니다."),
    ALREADY_SOLVED_QUIZ(HttpStatus.CONFLICT,"ALREADY_SOLVED_QUIZ", "이미 정답을 제출한 문제입니다."),
    QUIZ_SESSION_SAVE_FAILED(HttpStatus.CONFLICT,"QUIZ_SESSION_SAVE_FAILED", "퀴즈 세션 저장 중 오류가 발생했습니다."),
    QUIZ_SESSION_READ_FAILED(HttpStatus.CONFLICT,"QUIZ_SESSION_READ_FAILED", "퀴즈 세션 조회 중 오류가 발생했습니다."),
    QUIZ_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "QUIZ_SESSION_NOT_FOUND", "퀴즈에 대한 세션 정보를 찾을 수 없습니다."),
    QUIZ_SESSION_UPDATE_FAILED(HttpStatus.CONFLICT, "QUIZ_SESSION_UPDATE_FAILED", "퀴즈 세션 수정 중 오류가 발생했습니다."),
    QUIZ_SESSION_COMPLETE_FAILED(HttpStatus.CONFLICT, "QUIZ_SESSION_COMPLETE_FAILED", "퀴즈 세션 완료 처리 중 오류가 발생했습니다."),
    INVALID_QUIZ_SEQUENCE(HttpStatus.BAD_REQUEST, "INVALID_QUIZ_SEQUENCE", "잘못된 퀴즈 id를 입력하셨습니다."),
    NOT_ENOUGH_QUIZ(HttpStatus.UNPROCESSABLE_CONTENT, "NOT_ENOUGH_QUIZ", "요청한 개수만큼 퀴즈가 충분하지 않습니다."),
    INVALID_QUIZ_SESSION(HttpStatus.BAD_REQUEST, "INVALID_QUIZ_SESSION", "현재 진행 중인 퀴즈 세션과 일치하지 않습니다."),
    USER_CHARACTER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_CHARACTER_NOT_FOUND", "유저의 캐릭터 정보를 찾을 수 없습니다."),
    IMAGE_URL_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"IMAGE_URL_GENERATION_FAILED", "MinIO URL 생성에 실패하였습니다."),
    QUIZ_NOT_FINISHED(HttpStatus.BAD_REQUEST, "QUIZ_NOT_FINISHED", "아직 완료되지 않은 퀴즈 세션입니다."),
    QUIZ_RESULT_RECORD_NOT_MATCHED(HttpStatus.CONFLICT, "QUIZ_RESULT_RECORD_NOT_MATCHED", "퀴즈 세션 정보와 풀이 기록이 일치하지 않습니다."),
    INVALID_QUIZ_ID(HttpStatus.BAD_REQUEST, "INVALID_QUIZ_ID", "잘못된 형식의 퀴즈 아이디를 입력하셨습니다."),
    MISSING_QUIZ_ANSWER(HttpStatus.BAD_REQUEST, "MISSING_QUIZ_ANSWER", "제출할 퀴즈의 답을 입력해주세요."),
    MISSING_SESSION_ID(HttpStatus.BAD_REQUEST, "MISSING_SESSION_ID", "제출할 퀴즈의 세션 아이디를 입력해주세요."),
    INVALID_SESSION_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_SESSION_FORMAT", "잘못된 형식의 세션 아이디를 입력하셨습니다."),
    QUIZ_SESSION_STATE_CONFLICT(HttpStatus.CONFLICT, "QUIZ_SESSION_STATE_CONFLICT", "퀴즈 진행 상태가 올바르지 않거나 이미 종료된 세션입니다."),
    FEEDBACK_NOT_FOUND(HttpStatus.NOT_FOUND, "FEEDBACK_NOT_FOUND", "해당 피드백 기록을 찾을 수 없습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "해당 피드백에 접근할 권한이 없습니다."),
    FEEDBACK_DETAIL_NOT_FOUND(HttpStatus.NOT_FOUND, "FEEDBACK_DETAIL_NOT_FOUND", "해당 분리수거 가이드 상세 정보를 찾을 수 없습니다.");


    private final HttpStatus status;
    private final String code;
    private final String message;
}
