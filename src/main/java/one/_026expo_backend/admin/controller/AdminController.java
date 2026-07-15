package one._026expo_backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.admin.dto.request.AdminLoginRequestDto;
import one._026expo_backend.admin.dto.request.AdminRefreshTokenRequestDto;
import one._026expo_backend.admin.dto.request.AdminSignupRequestDto;
import one._026expo_backend.admin.dto.response.AdminLoginResponseDto;
import one._026expo_backend.admin.dto.response.AdminRefreshTokenResponseDto;
import one._026expo_backend.admin.dto.response.AdminSignupResponseDto;
import one._026expo_backend.admin.service.AdminService;
import one._026expo_backend.auth.dto.response.ExistsCheckResponseDto;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "admin", description = "admin 엔드포인트")
public class AdminController {
    private final AdminService adminService;

    /**
     * 관리자 회원가입 API
     */
    @Operation(summary = "관리자 회원가입", description = "관리자가 요청한 정보로 회원가입을 진행합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_INPUT, ErrorCode.DUPLICATE_USER})
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<AdminSignupResponseDto>> signup(@Valid @RequestBody AdminSignupRequestDto request) {
        AdminSignupResponseDto response = adminService.adminSignup(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 관리자 아이디 중복 체크 API
     */
    @Operation(summary = "관리자 아이디 중복 체크", description = "adminId를 이용해 존재 여부를 조회합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_LOGIN_ID})
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<ExistsCheckResponseDto>> isExistsAdminId(
            @Parameter(
                    description = "중복 체크할 관리자 아이디",
                    example = "admin123"
            )
            @RequestParam String adminId) {
        UseYnEnum exists = adminService.isExistsAdminId(adminId);
        return ResponseEntity.ok(ApiResponse.ok(new ExistsCheckResponseDto(exists)));
    }

    /**
     *
     * 관리자 로그인 API
     */
    @Operation(summary = "관리자 로그인", description = "관리자가 요청한 정보로 로그인을 진행합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_CREDENTIALS})
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResponseDto>> login(@Valid @RequestBody AdminLoginRequestDto request) {
        AdminLoginResponseDto response = adminService.adminLogin(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * 관리자 리프레시 토큰 재발급 API
     *
     * @param request 관리자 리프레시 토큰 재발급 요청 DTO
     * @return 새로운 토큰 세트를 포함한 응답
     */
    @Operation(summary = "관리자 리프레시 토큰 재발급", description = "만료된 Access Token을 갱신하기 위해 검증 및 세트를 재발급합니다.")
    @ApiErrorExceptions({ErrorCode.INVALID_TOKEN, ErrorCode.EXPIRED_REFRESH_TOKEN})
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<AdminRefreshTokenResponseDto>> reissue(
            @Valid @RequestBody AdminRefreshTokenRequestDto request) {
        AdminRefreshTokenResponseDto response = adminService.reissueToken(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
