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
    @NotBlank(message = "이름은 필수입니다.")
    @Size(max = 8, message = "이름은 8자 이하여야 합니다.")
    private String username;

    @Schema(description = "로그인 아이디", example = "user123")
    @NotBlank(message = "아이디는 필수입니다.")
    @Size(max = 12, message = "아이디는 12자 이하여야 합니다.")
    private String loginId;

    @Schema(description = "비밀번호", example = "P@ssw0rd")
    @NotBlank(message = "비밀번호는 필수입니다.")
    @Size(min = 6, max = 128, message = "비밀번호는 6자 이상 128자 이하여야 합니다.")
    private String password;

    @Schema(description = "이메일", example = "user@example.com")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    @Schema(description = "이용약관 동의 여부", example = "Y", allowableValues = {"Y", "N"})
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
