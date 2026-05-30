package one._026expo_backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.auth.dto.LoginRequestDto;
import one._026expo_backend.auth.dto.LoginResponseDto;
import one._026expo_backend.auth.dto.RefreshTokenRequestDto;
import one._026expo_backend.auth.dto.RefreshTokenResponseDto;
import one._026expo_backend.auth.service.AuthService;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.auth.dto.ExistsCheckResponseDto;
import one._026expo_backend.auth.dto.SignupRequestDto;
import one._026expo_backend.auth.dto.SignupResponseDto;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
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
     * @return 회원가입 된 유저 응답 객체
     */
    @Operation(summary = "LOCAL 회원가입", description="유저가 요청한 회원가입 정보로 LOCAL 회원가입을 진행합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_INPUT, ErrorCode.DUPLICATE_USER, ErrorCode.DUPLICATE_EMAIL, ErrorCode.TERMS_NOT_AGREED})
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> signup(@Valid @RequestBody SignupRequestDto request) {
        SignupResponseDto response = authService.signup(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
    
    /**
     * LOCAL 로그인 API
     * loginId와 비밀번호로 사용자를 인증, AccessToken과 RefreshToken을 발급
     * 
     * @param requestDto 로그인 요청 데이터
     * @return 사용자 정보와 토큰을 포함한 로그인 응답
     */
    @Operation(summary = "LOCAL 로그인", description="유저가 요청한 로그인 정보로 LOCAL 로그인 진행합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_CREDENTIALS, ErrorCode.DELETED_USER, ErrorCode.SOCIAL_LOGIN_REQUIRED, ErrorCode.EMAIL_NOT_VERIFIED})
    @PostMapping("/login")
    public ApiResponse<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        return ApiResponse.ok(authService.login(requestDto));
    }

    /**
     * Refresh Token 재발급 API
     * 앱에서 저장된 Refresh Token을 전달하면 Access Token과 Refresh Token을 새로 발급합니다.
     *
     * @param request 리프레시 토큰 요청 데이터
     * @return 갱신된 사용자 정보와 토큰 응답
     */
    @Operation(summary = "Refresh Token 재발급", description =
            "앱에서 전달한 Refresh Token으로 Access Token과 Refresh Token을 재발급합니다. <br>앱에서는 Access Token만 재저장하는 것이 아닌, 새로 발급된 Refresh Token도 함께 재저장해야 합니다."
    )
    @ApiErrorExceptions({ErrorCode.INVALID_TOKEN, ErrorCode.EXPIRED_REFRESH_TOKEN, ErrorCode.DELETED_USER})
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshTokenResponseDto>> refreshToken(@Valid @RequestBody RefreshTokenRequestDto request) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(request)));
    }
}
