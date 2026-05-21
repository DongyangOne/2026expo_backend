package one._026expo_backend.admin.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.admin.service.AdminService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "admin", description = "admin 엔드포인트")
public class AdminController {
    private final AdminService adminService;
}
