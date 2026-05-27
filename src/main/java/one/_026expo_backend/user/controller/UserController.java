package one._026expo_backend.user.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.user.dto.UserSaveRequestDto;
import one._026expo_backend.user.service.UserService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "user", description = "유저 엔드포인트")
public class UserController {
    private final UserService userService;

    @Operation(summary = "LOCAL 회원가입")
    @PostMapping("/save")
    public ResponseEntity<ApiResponse<Long>> save(@Valid @RequestBody UserSaveRequestDto request) {
        Long id = userService.save(request);
        return ResponseEntity.ok(ApiResponse.ok(id));
    }
}

