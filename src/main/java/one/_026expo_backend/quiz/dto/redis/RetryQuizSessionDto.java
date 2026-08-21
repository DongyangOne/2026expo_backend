package one._026expo_backend.quiz.dto.redis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Redis에 저장할 다시풀기 퀴즈 진행 상태 dto")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RetryQuizSessionDto {
    private String sessionId;
    private List<Long> quizIds;
    private Integer nextIndex;
    private Boolean finished;
    private Integer correctCount;

    // Redis에 이전 값이 없거나 null로 들어온 경우를 방어합니다.
    public Integer getCorrectCount() {
        return correctCount == null ? 0 : correctCount;
    }
}