package one._026expo_backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.auth.service.AuthService;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.auth.dto.ExistsCheckResponseDto;
import one._026expo_backend.auth.dto.SignupRequestDto;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name="auth", description = "auth 엔드포인트")
public class AuthController {
    private final AuthService authService;

    /**
     * loginId 중복 체크 API
     *
     * @param loginId 중복 여부를 확인할 loginId
     * @return 중복 여부
     */
    @Operation(summary = "아이디 중복 체크", description="loginId를 이용해 해당 loginId가 존재하는지 조회합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_LOGIN_ID})
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<ExistsCheckResponseDto>> isExistsLoginId(@RequestParam String loginId) {
        UseYnEnum exists = authService.isExistsLoginId(loginId);
        return ResponseEntity.ok(ApiResponse.ok(new ExistsCheckResponseDto(exists)));
    }

    /**
     * LOCAL 회원가입 API
     *
     * @param request 유저 회원가입 요청 정보
     * @return 회원가입 된 유저 응답 객체(수정 예정)
     */
    @Operation(summary = "LOCAL 회원가입", description="유저가 요청한 회원가입 정보로 LOCAL 회원가입을 진행합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_INPUT, ErrorCode.DUPLICATE_USER, ErrorCode.DUPLICATE_EMAIL, ErrorCode.TERMS_NOT_AGREED})
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<Long>> signup(@Valid @RequestBody SignupRequestDto request) {
        Long id = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok(id));
    }
}
