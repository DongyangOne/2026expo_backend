package one._026expo_backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.config.auth.CurrentUser;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.user.dto.request.UserWithdrawRequestDto;
import one._026expo_backend.user.dto.response.UserWithdrawResponseDto;
import one._026expo_backend.user.service.UserWithdrawalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 회원탈퇴 화면에서 사용하는 탈퇴 처리 엔드포인트를 제공합니다.
 *
 * 기존 사용자 조회 컨트롤러와 책임을 분리해, 탈퇴 전용 입력과 soft delete 로직을 독립적으로 유지합니다.
 */
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "user", description = "유저 엔드포인트")
public class UserWithdrawalController {

    private final UserWithdrawalService userWithdrawalService;

    /**
     * 로그인한 사용자의 비밀번호를 재확인한 뒤 회원탈퇴를 처리한다.
     *
     * @param userId 인증된 사용자 식별자
     * @param requestDto 비밀번호 및 탈퇴 사유 요청 정보
     * @return 회원탈퇴 완료 응답
     */
    @Operation(
            summary = "회원탈퇴",
            description = "로그인한 사용자의 비밀번호를 재확인한 뒤 탈퇴 사유를 저장하고 계정을 soft delete 처리합니다. "
                    + "Access Token은 stateless JWT 구조이므로 클라이언트에서도 함께 제거해야 합니다."
    )
    @ApiErrorExceptions({
            ErrorCode.INVALID_INPUT,
            ErrorCode.UNAUTHORIZED,
            ErrorCode.USER_NOT_FOUND,
            ErrorCode.DELETED_USER,
            ErrorCode.PASSWORD_MISMATCH
    })
    @PatchMapping("/withdrawal")
    public ResponseEntity<ApiResponse<UserWithdrawResponseDto>> softDelete(
            @CurrentUser Long userId,
            @Valid @RequestBody UserWithdrawRequestDto requestDto
    ) {
        UserWithdrawResponseDto response = userWithdrawalService.softDelete(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
