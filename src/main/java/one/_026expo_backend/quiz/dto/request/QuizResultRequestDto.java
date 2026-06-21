package one._026expo_backend.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "퀴즈 종료 및 결과 정산 요청")
@Getter
@NoArgsConstructor
public class QuizResultRequestDto {
    @Schema(description = "퀴즈 세션 id", example = "584aa531-df55-4d62-b923-3618ee92391e")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "퀴즈 세션 id 형식이 올바르지 않습니다."
    )
    @NotBlank(message = "퀴즈 세션 id를 입력해주세요.")
    private String sessionId;
}
