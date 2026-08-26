package one._026expo_backend.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.quiz.domain.Quiz;

@Schema(description = "퀴즈 다시풀기 시작 응답")
@Getter
@Builder
@AllArgsConstructor
public class RetryQuizStartResponseDto {

    @Schema(description = "다시풀기 Redis 세션 id", example = "550e8400-e29b-41d4-a716-446655440000")
    private String sessionId;

    @Schema(description = "다시 풀 문제 총 개수", example = "3")
    private Integer totalCount;

    @Schema(description = "첫번째 문제 퀴즈 id", example = "1")
    private Long quizId;

    @Schema(description = "첫번째 문제 내용")
    private String question;

    public static RetryQuizStartResponseDto of(String sessionId, Integer totalCount, Quiz quiz) {
        return RetryQuizStartResponseDto.builder()
                .sessionId(sessionId)
                .totalCount(totalCount)
                .quizId(quiz.getQuizId())
                .question(quiz.getQuestion())
                .build();
    }
}