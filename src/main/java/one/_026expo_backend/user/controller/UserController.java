package one._026expo_backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.config.auth.CurrentUser;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.user.dto.response.UserDashboardResponseDto;
import one._026expo_backend.user.dto.response.UserProfileResponseDto;
import one._026expo_backend.user.dto.response.UserVerificationEmailSendResponseDto;
import one._026expo_backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "user", description = "유저 엔드포인트")
public class UserController {
    private final UserService userService;

    /**
     * 로그인한 사용자의 마이페이지 프로필을 조회한다.
     *
     * 사용자 식별자는 인증 객체에서만 받아, 클라이언트가 임의의 userId로 프로필을 조회하지 못하게 한다.
     *
     * @param userId 인증된 사용자 식별자
     * @return 마이페이지 프로필 응답
     */
    @Operation(summary = "마이페이지 프로필 조회", description = "로그인한 사용자의 마이페이지 프로필 정보를 조회합니다.")
    @ApiErrorExceptions({ErrorCode.UNAUTHORIZED, ErrorCode.USER_NOT_FOUND})
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponseDto>> findOneProfile(@CurrentUser Long userId) {
        UserProfileResponseDto response = userService.findOneProfile(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

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
            ErrorCode.INTERNAL_ERROR
    })
    @PostMapping("/verification/email")
    public ResponseEntity<ApiResponse<UserVerificationEmailSendResponseDto>> sendVerificationEmail(
            @CurrentUser Long userId
    ) {
        UserVerificationEmailSendResponseDto response = userService.sendVerificationEmail(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 로그인한 사용자의 캐릭터, 퀴즈 정보, 분리수거 기록을 반환하는 API
     *
     * @param userId 로그인한 사용자의 고유 식별 아이디
     */
    @Operation(summary = "사용자 정보 조회", description = "로그인한 사용자의 정보를 조회합니다.")
    @ApiErrorExceptions({ErrorCode.USER_CHARACTER_NOT_FOUND})
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDashboardResponseDto>> getMyDashboard(
            @CurrentUser Long userId
    ) {
        UserDashboardResponseDto response = userService.getUserDashboard(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
