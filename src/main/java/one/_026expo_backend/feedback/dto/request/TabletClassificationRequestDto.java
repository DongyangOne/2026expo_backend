package one._026expo_backend.feedback.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "태블릿 AI 분류 요청 DTO")
public class TabletClassificationRequestDto {

    @NotBlank(message = "clientId는 필수입니다.")
    @Schema(description = "AI 검사 고유 식별자", example = "d8e4a4f7-3eec-488b-bff5-ed9f67edb8f0")
    private String clientId;

    @NotNull(message = "image는 필수입니다.")
    @Schema(description = "AI 서버로 전달할 재활용품 이미지")
    private MultipartFile image;

    @Schema(description = "무게 센서 측정값(g)", example = "28.0", nullable = true)
    private Double weightG;

    @Schema(description = "태블릿 또는 업로드 서버에 저장된 이미지 URL", nullable = true)
    private String imageUrl;
}
