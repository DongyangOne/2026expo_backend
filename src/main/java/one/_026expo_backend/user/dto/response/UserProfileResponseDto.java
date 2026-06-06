package one._026expo_backend.user.dto.response;

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
public class UserProfileResponseDto {

    private Long userId;
    private String profileImageUrl;
    private String name;
    private String loginId;
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
