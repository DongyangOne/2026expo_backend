package one._026expo_backend.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.quiz.domain.Quiz;

@Schema(description = "정답 제출 및 다음 퀴즈 조회 응답")
@Getter
@Builder
@AllArgsConstructor
public class NextQuizResponseDto {
    @Schema(description = "현재 퀴즈 정답 설명", example = "페트병과 뚜껑은 따로 버려야 해요!" )
    private String explan;
    @Schema(description = "현재 퀴즈 정답 여부", example = "true")
    private Boolean isCorrect;
    @Schema(description = "퀴즈 종료 여부", example = "false")
    private Boolean finished;
    @Schema(description = "다음 퀴즈 id", example = "6")
    private Long nextQuizId;
    @Schema(description = "다음 퀴즈 내용", example = "깨진 유리컵이나 접시는 신문지에 싸서 유리류 수거함에 분리배출해야 한다.")
    private String nextQuestion;

    public static NextQuizResponseDto of(
            Quiz nowQuiz,
            Quiz nextQuiz,
            Boolean isCorrect
    ){
        boolean finished = nextQuiz == null;
        return NextQuizResponseDto.builder()
                .explan(nowQuiz.getExplan())
                .isCorrect(isCorrect)
                .nextQuizId(nextQuiz.getQuizId())
                .nextQuestion(nextQuiz.getQuestion())
                .finished(finished)
                .build();
    }
}