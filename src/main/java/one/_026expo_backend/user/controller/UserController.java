package one._026expo_backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.config.auth.CurrentUser;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.user.dto.response.UserProfileResponseDto;
import one._026expo_backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
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
}
