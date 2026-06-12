package one._026expo_backend.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.quiz.enums.QuizAnswer;

@Schema(description = "정답 제출 및 다음 퀴즈 조회 요청")
@Getter
@NoArgsConstructor
public class NextQuizRequestDto {
    @Schema(description = "퀴즈 세션 id", example = "550e8400-e29b-41d4-a716-446655440000")
    @Pattern(
            regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "퀴즈 세션 id 형식이 올바르지 않습니다."
    )
    @NotBlank(message = "퀴즈 세션 id를 입력해주세요.")
    private String sessionId;

    @Schema(description = "현재 퀴즈 id", example = "3")
    @NotNull(message = "현재 퀴즈 id를 입력해주세요.")
    @Min(value = 1, message = "퀴즈 id는 양수여야 합니다.")
    @Max(value = 1000, message = "퀴즈 id는 1000을 넘을 수가 없습니다.")
    private Long currentQuizId;

    @Schema(description = "현재 퀴즈 정답", example = "X")
    @NotNull(message = "현재 퀴즈 정답을 입력해주세요.")
    private QuizAnswer answer;
}
