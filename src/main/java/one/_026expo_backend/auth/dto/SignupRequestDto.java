package one._026expo_backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.enums.SocialType;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "회원가입 요청 DTO")
public class SignupRequestDto {

    @Schema(description = "이름", example = "홍길동")
    @NotBlank(message = "이름은 필수입니다.")
    @Size(min= 2, max = 8, message = "이름은 2자 이상 8자 이하여야 합니다.")
    private String username;

    @Schema(description = "로그인 아이디", example = "user123")
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(min= 4, max = 12, message = "아이디는 4자 이상 12자 이하여야 합니다.")
    private String loginId;

    @Schema(description = "비밀번호", example = "password123*")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 8, max = 16, message = "비밀번호는 8자 이상 16자 이하여야 합니다.")
    private String password;

    @Schema(description = "이메일", example = "user@example.com")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @Schema(description = "이용약관 동의 여부", example = "Y", allowableValues = {"Y", "N"})
    @NotNull(message = "이용약관 동의 여부는 필수입니다.")
    private UseYnEnum agreeTerms;

    public Users toEntity(String encodedPassword, UseYnEnum emailVerified) {
        return Users.builder()
                .username(username)
                .loginId(loginId)
                .password(encodedPassword)
                .email(email)
                .emailVerified(emailVerified)
                .rememberMe(UseYnEnum.N)
                .termsAgreed(agreeTerms)
                .socialProviderId(null)
                .socialType(SocialType.LOCAL)
                .isDeleted(UseYnEnum.N)
                .deletedAt(null)
                .build();
    }
}
