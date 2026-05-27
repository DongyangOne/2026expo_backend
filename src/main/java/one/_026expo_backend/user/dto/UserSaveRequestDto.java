package one._026expo_backend.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class UserSaveRequestDto {

    @Schema(description = "이름", example = "홍길동")
    @NotBlank
    @Size(max = 8)
    private String username;

    @Schema(description = "로그인 아이디", example = "user123")
    @NotBlank
    @Size(max = 12)
    private String loginId;

    @Schema(description = "비밀번호", example = "P@ssw0rd")
    @NotBlank
    @Size(min = 6, max = 128)
    private String password;

    @Schema(description = "이메일", example = "user@example.com")
    @Email
    @NotBlank
    private String email;

    @Schema(description = "이용약관 동의 여부 (Y/N)", example = "Y")
    @NotBlank
    private String agreeTerms;

    public Users toEntity(String encodedPassword, UseYnEnum termsAgreed) {
        return Users.builder()
                .username(username)
                .loginId(loginId)
                .password(encodedPassword)
                .email(email)
                .emailVerified(UseYnEnum.N)
                .rememberMe(UseYnEnum.N)
                .termsAgreed(termsAgreed)
                .socialProviderId(null)
                .socialType(SocialType.LOCAL)
                .isDeleted(UseYnEnum.N)
                .deletedAt(null)
                .build();
    }
}
