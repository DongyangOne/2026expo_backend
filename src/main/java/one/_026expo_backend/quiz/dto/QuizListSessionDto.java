package one._026expo_backend.quiz.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Redis에 저장할 퀴즈 진행 상태 dto")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QuizListSessionDto {
    private List<Long> quizIds;
    private Integer currentIndex;
    private Boolean finished;
}
