package one._026expo_backend.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "퀴즈 시작 요청")
@Getter
@NoArgsConstructor
public class StartQuizRequestDto {
    @Schema(description = "퀴즈 개수", example = "5", defaultValue = "5")
    @NotNull(message = "퀴즈 개수를 지정해주세요.")
    @Min(value = 5, message = "퀴즈 개수는 5개 이상이어야 합니다.")
    @Max(value = 10, message = "퀴즈 개수는 10개 이하이어야 합니다.")
    private Integer quantity = 5;
}