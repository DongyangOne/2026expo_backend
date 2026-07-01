package one._026expo_backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.user.domain.Users;

/**
 * 마이페이지 프로필 수정 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "마이페이지 프로필 수정 응답 DTO")
public class UserProfileUpdateResponseDto {

    @Schema(description = "사용자 식별자", example = "1")
    private Long userId;

    @Schema(description = "이메일", example = "newemail@example.com")
    private String email;

    @Schema(description = "로그인 아이디", example = "kmh0707")
    private String loginId;

    @Schema(description = "이름", example = "김민혁")
    private String name;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile.png", nullable = true)
    private String profileImageUrl;

    /**
     * 수정이 반영된 사용자 정보를 화면 응답 형식에 맞춰 변환한다.
     *
     * 현재 Users 엔티티에는 별도 프로필 이미지 컬럼이 없어, 화면 스펙은 유지하되 값은 null로 응답한다.
     *
     * @param user 수정된 사용자 엔티티
     * @return 마이페이지 프로필 수정 응답 DTO
     */
    public static UserProfileUpdateResponseDto from(Users user) {
        return UserProfileUpdateResponseDto.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .loginId(user.getLoginId())
                .name(user.getUsername())
                .profileImageUrl(null)
                .build();
    }
}
