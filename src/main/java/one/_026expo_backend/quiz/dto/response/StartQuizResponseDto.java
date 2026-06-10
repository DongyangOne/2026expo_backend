package one._026expo_backend.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.quiz.domain.Quiz;

import java.util.List;

@Schema(description = "퀴즈 시작 응답")
@Getter
@Builder
@AllArgsConstructor
public class StartQuizResponseDto {
    @Schema(description = "퀴즈 세션 id", example = "550e8400-e29b-41d4-a716-446655440000")
    private String sessionId;
    @Schema(description = "첫번째 문제 퀴즈 id", example = "1")
    private Long quizId;
    @Schema(description = "첫번째 문제 내용", example = "페트병은 뚜껑과 함께 버려야 한다.")
    private String question;

    public static StartQuizResponseDto of(
            String sessionId,
            Quiz quiz
            ) {
        return StartQuizResponseDto.builder()
                .sessionId(sessionId)
                .quizId(quiz.getQuizId())
                .question(quiz.getQuestion())
                .build();
    }
}
