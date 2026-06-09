package one._026expo_backend.global.exception;

import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리기
 * * 애플리케이션 전역에서 발생하는 예외를 가로채 공통 응답 규격(ApiResponse)으로 변환함
 * * 예상치 못한 런타임 에러로부터 클라이언트에게 일관된 에러 포맷을 보장하기 위함
 */
@Slf4j
@RestControllerAdvice(basePackages = "one._026expo_backend")
public class GlobalExceptionHandler {

    /**
     * Bean Validation (@Valid) 실패 시 처리
     * * 유저에게 단순히 "입력값이 잘못되었습니다"라고 하기보다,
     * 어노테이션에 정의한 상세 메시지를 전달하여 가이드를 제공함
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        // 첫 번째 검증 오류 메시지만 추출하여 응답 부하를 줄이고 핵심 내용만 전달
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("[VALIDATION_ERROR] {}", errorMessage);

        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), errorMessage));
    }

    /**
     * 비즈니스 로직 예외 처리
     * * BusinessException을 처리하며,
     * 미리 정의된 ErrorCode의 상태 코드와 메시지를 활용함
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[BUSINESS_EXCEPTION] code: {}, message: {}", errorCode.getCode(), errorCode.getMessage());

        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode));
    }

    /**
     * 데이터베이스 제약 조건 위반 처리 (중복 데이터 등)
     * * DB 계층의 에러를 그대로 노출하지 않고, 가독성 있는 에러로 변환하여 보안 및 가독성 확보
     */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(org.springframework.dao.DataIntegrityViolationException e) {
        log.warn("[DATABASE_ERROR] 중복 데이터 또는 제약 조건 위반: {}", e.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("DUPLICATE_RESOURCE", "이미 존재하는 정보이거나 처리할 수 없는 데이터입니다."));
    }

    /**
     * 최상위 예외 처리
     * * 위에서 처리되지 않은 모든 예외를 500 에러로 처리하여
     * 서버 내부 로직(Stack Trace 등)이 외부로 유출되는 것을 방지함
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        // 내부 디버깅을 위해 에러 로그에 스택 트레이스를 기록함
        log.error("[SYSTEM_ERROR]", e);

        return ResponseEntity
                .status(ErrorCode.INTERNAL_ERROR.getStatus())
                .body(ApiResponse.error(ErrorCode.INTERNAL_ERROR));
    }

    /**
     * JSON 메시지 파싱 및 역직렬화 실패 시 처리
     * * 클라이언트의 잘못된 JSON 형식 요청이나 데이터 타입 변환 에러를 가로챔
     * * 스프링(Jackson)이 감싼 예외 껍데기를 벗겨내어, Enum 내부 등에서 발생한 근본적인 비즈니스 예외(BusinessException)를 정확하게 찾아 응답하기 위함
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(org.springframework.http.converter.HttpMessageNotReadableException e) {
        // 스프링이 감싼 예외의 가장 깊은 곳에 있는 진짜 원인(Root Cause)을 추출
        Throwable rootCause = e.getMostSpecificCause();

        // 그 원인이 우리가 정의한 비즈니스 예외라면 해당 에러 코드로 응답
        if (rootCause instanceof BusinessException) {
            BusinessException be = (BusinessException) rootCause;
            ErrorCode errorCode = be.getErrorCode();
            log.warn("[JSON_PARSE_BUSINESS_EXCEPTION] code: {}, message: {}", errorCode.getCode(), errorCode.getMessage());

            return ResponseEntity
                    .status(errorCode.getStatus())
                    .body(ApiResponse.error(errorCode));
        }

        // 단순 JSON 문법 오류나 타입 불일치일 경우 범용적인 입력 오류로 응답
        log.warn("[MESSAGE_NOT_READABLE] {}", e.getMessage());
        return ResponseEntity
                .status(ErrorCode.INVALID_INPUT.getStatus())
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), "요청하신 JSON 데이터의 형식이 올바르지 않습니다."));
    }
}