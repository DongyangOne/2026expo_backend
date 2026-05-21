package one._026expo_backend.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import one._026expo_backend.global.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * 전역 공통 응답 규격
 * * 성공/실패 여부에 상관없이 [데이터, 메시지, 응답 코드]를 포함하는 표준 규격을 정의함
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Schema(description = "공통 응답 규격")
public class ApiResponse<T> {

    // 클라이언트가 사용자에게 즉시 보여줄 수 있는 안내 문구 (성공/실패 공통)
    @Schema(description = "응답 메시지", example = "요청이 성공적으로 처리되었습니다.")
    private String message;

    // 비즈니스 로직의 결과를 식별하는 고유 코드 (성공 시 "SUCCESS", 실패 시 에러 코드)
    @Schema(description = "비즈니스 응답 코드", example = "SUCCESS")
    private String code;

    // 성공 시 반환되는 실제 데이터. 실패 시에는 JSON 결과에서 제외하여 혼선 방지
    @Schema(description = "실제 데이터")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    private boolean success;

    /**
     * 성공 응답 생성
     * * 데이터와 함께 기본 성공 메시지/코드를 결합하여 반환
     */
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code("SUCCESS")
                .message("요청에 성공하였습니다.")
                .data(data)
                .build();
    }

    /**
     * 에러 응답 생성 (ErrorCode 기반)
     * * 정의된 에러 규격을 error 객체에 담아 반환함으로써
     * 클라이언트가 '에러 발생 시 접근해야 할 필드'를 고정할 수 있게 함
     */
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return ApiResponse.<Void>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    /**
     * 에러 응답 생성 (커스텀 메시지)
     * * Validation 오류처럼 런타임에 동적으로 변하는 에러 내용을 전달할 때 사용
     */
    public static ApiResponse<Void> error(String code, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }

    /**
     * 에러 객체 캡슐화
     * * 응답 최상위의 code/message와 별개로 '에러 전용 객체'를 둠으로써
     * 향후 필드 확장(ex: 유효성 검사 실패 목록 등)에 유연하게 대응하기 위함
     */
    @Getter
    @AllArgsConstructor
    @Schema(description = "에러 상세 정보")
    public static class Error {
        @Schema(description = "에러 코드", example = "USER_NOT_FOUND")
        private String code;

        @Schema(description = "에러 메시지", example = "사용자를 찾을 수 없습니다")
        private String message;
    }
}