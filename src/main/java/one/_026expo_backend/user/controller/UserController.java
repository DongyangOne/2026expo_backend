package one._026expo_backend.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.user.dto.ExistsCheckResponseDto;
import one._026expo_backend.user.dto.UserSaveRequestDto;
import one._026expo_backend.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "user", description = "유저 엔드포인트")
public class UserController {
    private final UserService userService;

    @Operation(summary = "아이디 중복 체크")
    @GetMapping("/exists")
    public ResponseEntity<ApiResponse<ExistsCheckResponseDto>> isExistsLoginId(@RequestParam String loginId) {
        boolean isExist = userService.isExistsLoginId(loginId);
        return ResponseEntity.ok(ApiResponse.ok(new ExistsCheckResponseDto(isExist)));
    }

    @Operation(summary = "LOCAL 회원가입")
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Long>> save(@Valid @RequestBody UserSaveRequestDto request) {
        Long id = userService.save(request);
        return ResponseEntity.ok(ApiResponse.ok(id));
    }
}

