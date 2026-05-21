package one._026expo_backend.auth.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.auth.service.AuthService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/auth")
@RequiredArgsConstructor
@Tag(name="auth", description = "auth 엔드포인트")
public class AuthController {
    private final AuthService authService;
}
