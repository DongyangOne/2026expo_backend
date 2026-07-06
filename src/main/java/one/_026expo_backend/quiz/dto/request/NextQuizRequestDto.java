package one._026expo_backend.quiz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.enums.QuizAnswer;

@Schema(description = "정답 제출 및 다음 퀴즈 조회 요청")
@Getter
@NoArgsConstructor
public class NextQuizRequestDto {
    @Schema(description = "현재 퀴즈 id", example = "3")
    private Long currentQuizId;

    @Schema(description = "현재 퀴즈 정답", example = "X")
    private QuizAnswer answer;

    public void validate() {
        if (currentQuizId == null || currentQuizId < 1 || currentQuizId > 1000) {
            throw new BusinessException(ErrorCode.INVALID_QUIZ_ID);
        }
        if (answer == null) {
            throw new BusinessException(ErrorCode.MISSING_QUIZ_ANSWER);
        }
    }
}
