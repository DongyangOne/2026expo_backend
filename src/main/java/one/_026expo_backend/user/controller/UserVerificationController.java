package one._026expo_backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.config.auth.CurrentUser;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.user.dto.request.UserVerificationEmailSendRequestDto;
import one._026expo_backend.user.dto.response.UserVerificationEmailSendResponseDto;
import one._026expo_backend.user.service.UserVerificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 사용자 인증 관련 엔드포인트를 제공합니다.
 *
 * 기존 유저 컨트롤러를 건드리지 않고, 사용자 인증 화면에 필요한 API만 분리해 추가합니다.
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "user", description = "유저 엔드포인트")
public class UserVerificationController {

    private final UserVerificationService userVerificationService;

    /**
     * 로그인한 사용자의 계정 이메일로 사용자 인증 번호를 발송합니다.
     *
     * 클라이언트가 이메일을 직접 넘기지 않게 해, 본인 계정 이메일로만 인증 메일이 발송되도록 제한합니다.
     *
     * @param userId 인증된 사용자 식별자
     * @param requestDto 확장 대비용 빈 요청 DTO
     * @return 마스킹된 이메일과 인증 코드 유효 시간을 담은 응답
     */
    @Operation(
            summary = "마이페이지 사용자 인증 이메일 전송",
            description = "로그인한 사용자의 계정 이메일로 6자리 인증 번호를 발송합니다."
    )
    @ApiErrorExceptions({
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.INVALID_INPUT,
            ErrorCode.TOO_MANY_EMAIL_REQUESTS,
            ErrorCode.EMAIL_SEND_FAILED,
            ErrorCode.REDIS_CONNECTION_ERROR,
            ErrorCode.INTERNAL_ERROR
    })
    @PostMapping("/verification/email")
    public ResponseEntity<ApiResponse<UserVerificationEmailSendResponseDto>> sendVerificationEmail(
            @CurrentUser Long userId,
            @RequestBody(required = false) UserVerificationEmailSendRequestDto requestDto
    ) {
        UserVerificationEmailSendResponseDto response = userVerificationService.sendVerificationEmail(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
