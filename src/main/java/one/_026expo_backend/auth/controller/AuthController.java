package one._026expo_backend.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.auth.dto.request.LoginRequestDto;
import one._026expo_backend.auth.dto.response.LoginResponseDto;
import one._026expo_backend.auth.dto.request.*;
import one._026expo_backend.auth.dto.response.*;
import one._026expo_backend.auth.dto.response.QrLoginResponseDto;
import one._026expo_backend.auth.dto.response.QrTokenResponseDto;
import one._026expo_backend.auth.dto.response.FindIdResponseDto;
import one._026expo_backend.auth.dto.response.SocialLoginResponseDto;
import one._026expo_backend.auth.dto.response.EmailCheckResponseDto;
import one._026expo_backend.auth.dto.response.EmailSendResponseDto;
import one._026expo_backend.auth.dto.request.SocialLoginRequestDto;
import one._026expo_backend.auth.dto.request.RefreshTokenRequestDto;
import one._026expo_backend.auth.dto.response.RefreshTokenResponseDto;
import one._026expo_backend.auth.service.AuthService;
import one._026expo_backend.auth.service.EmailService;
import one._026expo_backend.auth.service.QrService;
import one._026expo_backend.auth.service.SocialLoginService;
import one._026expo_backend.global.config.auth.CurrentUser;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.auth.dto.response.ExistsCheckResponseDto;
import one._026expo_backend.auth.dto.request.SignupRequestDto;
import one._026expo_backend.auth.dto.response.SignupResponseDto;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name="auth", description = "auth 엔드포인트")
public class AuthController {
    private final AuthService authService;
    private final EmailService emailService;
    private final SocialLoginService socialLoginService;
    private final QrService qrService;

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
     * 회원가입 API (LOCAL / 소셜)
     *
     * @param request 유저 회원가입 요청 정보
     * @return 회원가입 된 유저 응답 객체
     */
    @Operation(summary = "회원가입", description="유저가 요청한 회원가입 정보로 회원가입을 진행합니다. social이 LOCAL이면 일반 회원가입, 그 외(KAKAO/NAVER/GOOGLE)면 소셜 회원가입으로 처리되며, 이 경우 providerId가 필수입니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_INPUT, ErrorCode.DUPLICATE_USER, ErrorCode.DUPLICATE_EMAIL, ErrorCode.EMAIL_NOT_VERIFIED, ErrorCode.TERMS_NOT_AGREED, ErrorCode.CHARACTER_NOT_FOUND})
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
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(@Valid @RequestBody LoginRequestDto requestDto) {
        LoginResponseDto response = authService.login(requestDto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 현재 로그인한 사용자의 로그아웃을 처리한다.
     *
     * @param userId 인증된 사용자 식별자
     * @return 로그아웃 완료 응답
     */
    @Operation(summary = "로그아웃", description = "현재 로그인한 사용자의 Refresh Token을 무효화해 로그아웃을 처리합니다.")
    @ApiErrorExceptions({ErrorCode.UNAUTHORIZED, ErrorCode.USER_NOT_FOUND})
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<AuthLogoutResponseDto>> logout(@CurrentUser Long userId) {
        AuthLogoutResponseDto response = authService.logout(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * KAKAO 로그인 API
     *
     * @param requestDto 카카오 로그인 요청 데이터
     * @return 사용자 정보와 토큰을 포함한 로그인 응답 / 회원가입 정보
     */
    @Operation(summary = "KAKAO 로그인", description = "카카오 인가 코드로 카카오 계정을 조회하고, 기존 유저 존재시 로그인 / 미존재 시 회원가입 정보를 반환합니다. <br>" +
            "인가코드는 \"https://kauth.kakao.com/oauth/authorize?response_type=code&client_id=<카카오REST_API_KEY>&redirect_uri=<redirect_uri>\"에 접근해 사용자가 로그인한 뒤 얻을 수 있습니다.")
    @ApiErrorExceptions({ErrorCode.KAKAO_EMAIL_REQUIRED, ErrorCode.KAKAO_LOGIN_FAILED, ErrorCode.DELETED_USER})
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<SocialLoginResponseDto>> kakaoLogin(@Valid @RequestBody SocialLoginRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.ok(socialLoginService.kakaoLogin(requestDto)));
    }

    /**
     * GOOGLE 로그인 API
     *
     * @param requestDto 구글 로그인 요청 데이터
     * @return 사용자 정보와 토큰을 포함한 로그인 응답 / 회원가입 정보
     */
    @Operation(summary = "GOOGLE 로그인", description = "구글 인가 코드로 구글 계정을 조회하고, 기존 유저 존재시 로그인 / 미존재 시 회원가입 정보를 반환합니다. <br>" +
            "인가코드는 \"https://accounts.google.com/o/oauth2/v2/auth?response_type=code&client_id=<구글CLIENT_ID>&redirect_uri=<redirect_uri>&scope=email%20profile\"에 접근해 사용자가 로그인한 뒤 얻을 수 있습니다.")
    @ApiErrorExceptions({ErrorCode.GOOGLE_EMAIL_REQUIRED, ErrorCode.GOOGLE_LOGIN_FAILED, ErrorCode.DELETED_USER})
    @PostMapping("/google/login")
    public ResponseEntity<ApiResponse<SocialLoginResponseDto>> googleLogin(@Valid @RequestBody GoogleLoginRequestDto requestDto) {
        return ResponseEntity.ok(ApiResponse.ok(socialLoginService.googleLogin(requestDto)));
    }

    /**
     * NAVER 로그인 API
     *
     * @param requestDto 네이버 로그인 요청 데이터
     * @return 사용자 정보와 토큰을 포함한 로그인 응답 / 회원가입 정보
     */
    @Operation(summary = "NAVER 로그인", description = "네이버 인가 코드로 네이버 계정을 조회하고, 기존 유저 존재시 로그인 / 미존재 시 회원가입 정보를 반환합니다. <br>" +
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
     * @param request 유저의 기존 리프레시 토큰
     * @return 갱신된 토큰 응답
     */
    @Operation(summary = "Refresh Token 재발급", description =
            "앱에서 전달한 Refresh Token으로 Access Token과 Refresh Token을 재발급합니다. <br>앱에서는 Access Token만 재저장하는 것이 아닌, 새로 발급된 Refresh Token도 함께 재저장해야 합니다."
    )
    @ApiErrorExceptions({ErrorCode.INVALID_TOKEN, ErrorCode.EXPIRED_REFRESH_TOKEN, ErrorCode.DELETED_USER})
    @PostMapping("/token")
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

    /**
     * QR 코드 생성용 토큰 발급 API
     * 본 토큰은 QR 코드의 식별값 및 실시간 통신 채널 Key로 활용
     *
     * @return {@link ResponseEntity} 구조에 담긴 QR 토큰 응답 DTO
     */
    @Operation(summary = "QR 코드 생성용 토큰 발급", description = "QR 코드용 토큰을 발급하고 Redis 서버에 저장합니다.")
    @ApiErrorExceptions({ErrorCode.REDIS_CONNECTION_ERROR})
    @PostMapping("/qr/token")
    public ResponseEntity<ApiResponse<QrTokenResponseDto>> createQrToken() {
        // 서비스 레이어를 호출하여 토큰 생성 및 Redis 저장 로직 수행
        QrTokenResponseDto qrToken = qrService.createQrToken();
        // 최종 DTO에 담아 응답 반환
        return ResponseEntity.ok(ApiResponse.ok(qrToken));
    }

    /**
     * 태블릿이 발급받은 QR 토큰을 사용한 서버 SSE 수립 API
     *
     * @param qrToken 고유 QR 토큰
     * @return 실시간 스트리밍 연결 유지를 위한 {@link SseEmitter} (Content-Type: text/event-stream)
     */
    @Operation(summary = "태블릿 QR 로그인 SSE 수립", description = "발급받은 QR 토큰을 기반으로 서버와 끊어지지 않는 실시간 통신 채널을 개설합니다. 앱에서 인증 완료 시 이 채널을 통해 로그인 토큰이 발송됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "SSE 연결 성공",
            content = {
                    @Content(mediaType = "text/event-stream", examples = {
                            @ExampleObject(name = "INIT", value = "event:INIT\ndata:{\"message\":\"요청에 성공하였습니다.\",\"code\":\"SUCCESS\",\"data\":\"Connected!\",\"success\":true}\n"),
                            @ExampleObject(name = "LOGIN_SUCCESS", value = "event:LOGIN_SUCCESS\ndata:{\"message\":\"요청에 성공하였습니다.\",\"code\":\"SUCCESS\",\"data\":{\"userId\":1,\"loginId\":\"user123\",\"email\":\"user123@gmail.com\",\"username\":\"홍길동\",\"team\":\"개발팀\",\"accessToken\":\"eyJhbGciOiJIUzUxMiJ9...\",\"refreshToken\":\"eyJhbGciOiJIUzUxMiJ9...\"},\"success\":true}\n")
                    }),
                    @Content(mediaType = "application/json") // 스웨거에서 application/json 선택지를 유지하기 위한 용도 (실제 200이 json으로 오진 않음)
            })
    @ApiErrorExceptions({ErrorCode.INVALID_QR_TOKEN, ErrorCode.REDIS_CONNECTION_ERROR})
    // 에러 발생 시 GlobalExceptionHandler가 JSON으로 응답할 수 있도록 application/json도 producible 타입에 포함
    @GetMapping(value = "/qr/connect/{qrToken}", produces = {MediaType.TEXT_EVENT_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE}) // produces = MediaType.TEXT_EVENT_STREAM_VALUE로 SSE 사용 선언
    public ResponseEntity<SseEmitter> connectQrSse(@PathVariable String qrToken) {
        SseEmitter emitter = qrService.createSseConnection(qrToken);
        return ResponseEntity.ok(emitter); // SseEmitter 객체는 json 형태 응답이 아니므로 ApiResponse로 감싸지 않음
    }

    /**
     * 모바일 앱에서 QR 로그인을 승인하는 API
     *
     * @param request QR 토큰이 담긴 승인 요청
     * @param userId 모바일 앱(Access Token)에 저장되어있는 사용자 ID
     * @return 태블릿용 토큰이 포함된 로그인 정보
     */
    @Operation(summary = "QR 로그인 승인", description = "로그인된 사용자가 QR 토큰을 승인하면 태블릿용 로그인 토큰을 발급하고 SSE로 전달합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_QR_TOKEN, ErrorCode.UNAUTHORIZED, ErrorCode.DELETED_USER, ErrorCode.INVALID_TOKEN})
    @PostMapping("/qr/login")
    public ResponseEntity<ApiResponse<QrLoginResponseDto>> approveQrLogin(@Valid @RequestBody QrLoginRequestDto request, @CurrentUser Long userId) {
        QrLoginResponseDto response = qrService.approveQrLogin(request.getQrToken(), userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 인증 번호 이메일 전송 API
     * @param dto 아이디를 찾고자 하는 사용자의 이메일 주소
     * @return 발송 정보 및 만료 시간
     */
    @Operation(summary = "아이디 찾기 - 인증번호 발송", description = "가입된 이메일인지 확인한 후, 해당 메일 주소로 아이디 찾기용 6자리 인증번호를 발송합니다.")
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.TOO_MANY_EMAIL_REQUESTS, ErrorCode.EMAIL_SEND_FAILED})
    @PostMapping("/find-id/send")
    public ResponseEntity<ApiResponse<EmailSendResponseDto>> sendFindId(
            @Valid @RequestBody FindIdRequestDto dto
    ) {
        EmailSendResponseDto response = emailService.sendFindIdEmail(dto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 이메일 인증 번호 검증 및 ID 반환 API
     * @param dto 이메일 주소와 사용자가 입력한 인증 번호
     * @return 검증 성공 시 마스킹 없는 사용자의 오리지널 로그인 ID 반환
     */
    @Operation(summary = "아이디 찾기 - 인증번호 검증 및 ID 반환", description = "발송된 6자리 인증번호를 검증하고, 성공 시 해당 이메일로 가입된 유저의 로그인 아이디를 반환합니다.")
    @ApiErrorExceptions({ErrorCode.AUTH_CODE_EXPIRED, ErrorCode.AUTH_CODE_MISMATCH, ErrorCode.USER_NOT_FOUND})
    @PostMapping("/find-id/check")
    public ResponseEntity<ApiResponse<FindIdResponseDto>> checkFindId(
            @Valid @RequestBody FindIdCheckRequestDto dto
    ) {
        FindIdResponseDto response = emailService.verifyFindIdAndGetId(dto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
  
    /**
     * 비밀범호 찾기(재설정)를 위해 이메일로 인증 코드를 전송하는 API
     *
     * @param dto 비밀번호를 변경하고자 하는 사용자의 아이디와 비밀번호
     *
     */
    @Operation(summary = "비밀번호 찾기(재설정) - 인증번호 발송", description = "가입된 사용자인지 확인한 후, 해당 메일 주소로 비밀번호 찾기(재생성)용 6자리 인증번호를 발송합니다.")
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.TOO_MANY_EMAIL_REQUESTS, ErrorCode.EMAIL_SEND_FAILED})
    @PostMapping("/find-password/send")
    public ResponseEntity<ApiResponse<EmailSendResponseDto>> sendFindPassword(
            @Valid @RequestBody FindPasswordRequestDto dto
    ) {
        EmailSendResponseDto response = emailService.sendFindPasswordEmail(dto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 인증 코드 검증 및 비밀번호 찾기(재설정)를 위한 임시 권한 토큰 발급 API
     *
     * @param dto 비밀번호를 변경하려는 사용자의 아이디, 이메일, 인증 코드 정보
     */
    @Operation(summary = "비밀번호 찾기(재설정) - 인증번호 검증 및 임시 권한 토큰 발급",
            description = "발송된 6자리 인증번호를 검증하고, 성공 시 비밀번호 변경을 위한 임시 권한 토큰을 발급합니다.")
    @ApiErrorExceptions({ErrorCode.AUTH_CODE_EXPIRED, ErrorCode.AUTH_CODE_MISMATCH, ErrorCode.USER_NOT_FOUND})
    @PostMapping("/find-password/check")
    public ResponseEntity<ApiResponse<ResetTokenResponseDto>> checkFindPassword(
            @Valid @RequestBody FindPasswordCheckRequestDto dto
    ) {
        ResetTokenResponseDto response = emailService.verifyFindPasswordAndGetToken(dto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 사용자의 비밀번호를 변경하는 API
     *
     * @param dto 발급받은 임시 권한 토큰과 새로운 비밀번호
     */
    @Operation(summary = "비밀번호 찾기(재설정) - 비밀번호 변경",
            description = "발급한 임시 권한 토큰을 검증하고, 입력받은 새로운 비밀번호를 암호화하여 변경합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_TOKEN, ErrorCode.USER_NOT_FOUND})
    @PostMapping("find-password/reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody PasswordResetRequestDto dto
    ) {
        emailService.updatePassword(dto);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
