package one._026expo_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocialProfileDto {

    private String providerId;
    private String email;
    private String nickname;

    public static SocialProfileDto from(String providerId, String email, String nickname) {
        return new SocialProfileDto(providerId, email, nickname);
    }
}