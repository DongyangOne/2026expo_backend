package one._026expo_backend.admin.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.admin.dto.request.AdminLoginRequestDto;
import one._026expo_backend.admin.dto.request.AdminSignupRequestDto;
import one._026expo_backend.admin.dto.response.AdminLoginResponseDto;
import one._026expo_backend.admin.dto.response.AdminSignupResponseDto;
import one._026expo_backend.admin.service.AdminService;
import one._026expo_backend.auth.dto.ExistsCheckResponseDto;
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
    public ResponseEntity<ApiResponse<ExistsCheckResponseDto>> isExistsAdminId(@RequestParam String adminId) {
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
}
