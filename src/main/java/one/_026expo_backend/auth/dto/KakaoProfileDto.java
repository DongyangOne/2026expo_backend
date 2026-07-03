package one._026expo_backend.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KakaoProfileDto {

    private String providerId;
    private String email;
    private String nickname;

    public static KakaoProfileDto from(String providerId, String email, String nickname) {
        return new KakaoProfileDto(providerId, email, nickname);
    }
}