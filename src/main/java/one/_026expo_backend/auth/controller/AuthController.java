package one._026expo_backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.auth.dto.LoginRequestDto;
import one._026expo_backend.auth.dto.LoginResponseDto;
import one._026expo_backend.auth.dto.request.GoogleLoginRequestDto;
import one._026expo_backend.auth.dto.response.SocialLoginResponseDto;
import one._026expo_backend.auth.dto.request.KakaoLoginRequestDto;
import one._026expo_backend.auth.dto.request.NaverLoginRequestDto;
import one._026expo_backend.auth.dto.request.EmailCheckRequestDto;
import one._026expo_backend.auth.dto.request.EmailSendRequestDto;
import one._026expo_backend.auth.dto.response.EmailCheckResponseDto;
import one._026expo_backend.auth.dto.response.EmailSendResponseDto;
import one._026expo_backend.auth.dto.RefreshTokenRequestDto;
import one._026expo_backend.auth.dto.RefreshTokenResponseDto;
import one._026expo_backend.auth.service.AuthService;
import one._026expo_backend.auth.service.EmailService;
import one._026expo_backend.auth.service.SocialLoginService;
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
    private final EmailService emailService;
    private final SocialLoginService socialLoginService;

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
     * KAKAO 로그인 API
     */
    @Operation(summary = "KAKAO 로그인", description = "카카오 인가 코드로 카카오 계정을 조회하고, 기존 유저가 없으면 회원가입 후 로그인합니다. <br>" +
            "인가코드는 \"https://kauth.kakao.com/oauth/authorize?response_type=code&client_id={카카오REST_API_KEY}&redirect_uri={redirect_uri}\"에 접근해 사용자가 로그인한 뒤 얻을 수 있습니다.")
    @ApiErrorExceptions({ErrorCode.KAKAO_EMAIL_REQUIRED, ErrorCode.KAKAO_LOGIN_FAILED, ErrorCode.DELETED_USER})
    @PostMapping("/kakao/login")
    public ResponseEntity<ApiResponse<SocialLoginResponseDto>> kakaoLogin(@Valid @RequestBody KakaoLoginRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.ok(socialLoginService.kakaoLogin(requestDto)));
    }

    /**
     * GOOGLE 로그인 API
     */
    @Operation(summary = "GOOGLE 로그인", description = "구글 인가 코드로 구글 계정을 조회하고, 기존 유저가 없으면 회원가입 후 로그인합니다. <br>" +
            "인가코드는 \"https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id={구글CLIENT_ID}&redirect_uri={redirect_uri}&scope=email%20profile\"에 접근해 사용자가 로그인한 뒤 얻을 수 있습니다.")
    @ApiErrorExceptions({ErrorCode.GOOGLE_EMAIL_REQUIRED, ErrorCode.GOOGLE_LOGIN_FAILED, ErrorCode.DELETED_USER})
    @PostMapping("/google/login")
    public ResponseEntity<ApiResponse<SocialLoginResponseDto>> googleLogin(@Valid @RequestBody GoogleLoginRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.ok(socialLoginService.googleLogin(requestDto)));
    }

    /**
     * NAVER 로그인 API
     */
    @Operation(summary = "NAVER 로그인", description = "네이버 인가 코드로 네이버 계정을 조회하고, 기존 유저가 없으면 회원가입 후 로그인합니다. <br>" +
            "인가코드는 \"https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id={네이버 CLIENT_ID}&redirect_uri={redirect_uri}&state={앱 STATE 값}\"에 접근해 사용자가 로그인한 뒤 얻을 수 있습니다.")
    @ApiErrorExceptions({ErrorCode.NAVER_EMAIL_REQUIRED, ErrorCode.NAVER_LOGIN_FAILED, ErrorCode.DELETED_USER})
    @PostMapping("/naver/login")
    public ResponseEntity<ApiResponse<SocialLoginResponseDto>> naverLogin(@Valid @RequestBody NaverLoginRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.ok(socialLoginService.naverLogin(requestDto)));
    }

    /**
     * 회원가입 이메일 인증번호 발송 API
     */
    @Operation(summary = "이메일 인증번호 발송", description = "입력한 이메일로 회원가입용 6자리 인증번호를 발송합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_INPUT, ErrorCode.EMAIL_SEND_FAILED})
    @PostMapping("/email/send")
    public ResponseEntity<ApiResponse<EmailSendResponseDto>> sendVerificationEmail(
            @Valid @RequestBody EmailSendRequestDto dto
    ) {
        EmailSendResponseDto response = emailService.sendVerificationEmail(dto);
        return ResponseEntity.ok(ApiResponse.ok(response));
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

    /**
     * 이메일로 전송된 인증 번호 검증 API
     * <p>발송된 인증 번호를 검증하고, 성공 시 그 상태를 30분간 유지합니다.</p>
     *
     * @param dto 이메일 주소와 검증항 인증 번호
     * @return 성공 시 Void 데이터가 포함된 ApiResponse
     */
    @Operation(summary = "이메일로 전송된 인증 번호 검증", description = "발송된 6자리 인증 번호를 검증합니다.")
    @ApiErrorExceptions({ErrorCode.AUTH_CODE_EXPIRED, ErrorCode.AUTH_CODE_MISMATCH, ErrorCode.INTERNAL_ERROR})
    @PostMapping("/email/check")
    public  ResponseEntity<ApiResponse<EmailCheckResponseDto>> checkEmail(
            @Valid @RequestBody EmailCheckRequestDto dto) {
        EmailCheckResponseDto response = emailService.verifyAuthCode(dto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
