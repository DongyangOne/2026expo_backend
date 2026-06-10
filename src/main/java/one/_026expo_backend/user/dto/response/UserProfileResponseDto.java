package one._026expo_backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.user.domain.Users;

/**
 * 마이페이지 프로필 단건 조회 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
@Schema(description = "마이페이지 프로필 조회 응답 DTO")
public class UserProfileResponseDto {

    @Schema(description = "사용자 식별자", example = "1")
    private Long userId;

    @Schema(description = "프로필 이미지 URL", example = "https://cdn.example.com/profile.png", nullable = true)
    private String profileImageUrl;

    @Schema(description = "이름", example = "김민혁")
    private String name;

    @Schema(description = "로그인 아이디", example = "kmh0707")
    private String loginId;

    @Schema(description = "이메일", example = "kmh0707@naver.com")
    private String email;

    /**
     * 현재 화면에서 필요한 사용자 정보만 노출하기 위해 엔티티를 프로필 응답 객체로 변환한다.
     *
     * 현재 Users 엔티티에는 별도 프로필 이미지 컬럼이 없어, 화면 스펙은 유지하되 값은 null로 응답한다.
     *
     * @param user 로그인 사용자 엔티티
     * @return 마이페이지 프로필 응답 DTO
     */
    public static UserProfileResponseDto from(Users user) {
        return UserProfileResponseDto.builder()
                .userId(user.getId())
                .profileImageUrl(null)
                .name(user.getUsername())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .build();
    }
}
