package one._026expo_backend.admin.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.admin.domain.Admin;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "관리자 회원가입 요청 DTO")
public class AdminSignupRequestDto {

    @Schema(description = "관리자 로그인 아이디", example = "admin123")
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min = 4, max = 12, message = "아이디는 4자 이상 12자 이하여야 합니다.")
    private String adminId;

    @Schema(description = "비밀번호", example = "password123*")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하여야 합니다.")
    private String adminPassword;

    @Schema(description = "소속 팀명", example = "운영기획팀")
    @NotBlank(message = "소속 팀은 필수입니다.")
    @Size(min = 2, max = 20, message = "팀명은 2자 이상 20자 이하여야 합니다.")
    private String team;

    public Admin toEntity(String encodedPassword) {
        return Admin.builder()
                .adminId(adminId)
                .adminPassword(encodedPassword)
                .team(team)
                .build();
    }
}