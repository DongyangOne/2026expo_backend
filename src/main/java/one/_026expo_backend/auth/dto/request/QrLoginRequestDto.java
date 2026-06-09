package one._026expo_backend.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "QR 로그인 승인 요청 DTO")
public class QrLoginRequestDto {

    @NotBlank(message = "QR 토큰은 필수입니다.")
    @Schema(description = "승인할 QR 토큰", example = "5edf4094-23a...")
    private String qrToken;
}