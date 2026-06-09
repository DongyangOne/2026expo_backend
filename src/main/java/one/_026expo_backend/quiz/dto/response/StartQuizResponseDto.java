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
    @Schema(description = "문제에 사용할 퀴즈 id리스트", example = "[1, 5, 8, 9, 10]")
    private List<Long> quizList;
    @Schema(description = "첫번째 문제 퀴즈 id", example = "1")
    private Long quizId;
    @Schema(description = "첫번째 문제 내용", example = "페트병은 뚜껑과 함께 버려야 한다.")
    private String question;

    public static StartQuizResponseDto of(
            List<Long> quizList,
            Quiz quiz
            ) {
        return StartQuizResponseDto.builder()
                .quizList(quizList)
                .quizId(quiz.getQuizId())
                .question(quiz.getQuestion())
                .build();
    }
}
