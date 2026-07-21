package one._026expo_backend.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class QrTokenResponseDto {
    @Schema(description = "생성된 QR용 토큰", example = "5edf4094-23...")
    private String qrToken;

    public static QrTokenResponseDto of(String qrToken) {
        return new QrTokenResponseDto(qrToken);
    }
}