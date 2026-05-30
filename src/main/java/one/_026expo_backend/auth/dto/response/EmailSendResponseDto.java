package one._026expo_backend.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class EmailSendResponseDto {
    private String email;
    private LocalDateTime expiredAt;

    public static EmailSendResponseDto of(String email, LocalDateTime expiredAt) {
        return EmailSendResponseDto.builder()
                .email(email)
                .expiredAt(expiredAt)
                .build();
    }
}