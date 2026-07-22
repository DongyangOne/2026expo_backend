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
    @Schema(description = "현재 퀴즈 정답 설명", example = "페트병 내부에 이물질이 들어가는 것을 막고 압축 상태를 유지하기 위해 뚜껑을 닫아 배출하는 것이 올바른 방법입니다." )
    private String explan;
    @Schema(description = "현재 퀴즈 정답 여부", example = "false")
    private Boolean isCorrect;
    @Schema(description = "퀴즈 종료 여부", example = "false")
    private Boolean finished;
    @Schema(description = "다음 퀴즈 id", example = "6")
    private Long nextQuizId;
    @Schema(description = "다음 퀴즈 내용", example = "양파 껍질, 파 뿌리, 마늘 껍질은 음식물 쓰레기가 아닌 일반 쓰레기이다.")
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
                .nextQuizId(finished ? null : nextQuiz.getQuizId())
                .nextQuestion(finished ? null : nextQuiz.getQuestion())
                .finished(finished)
                .build();
    }
}