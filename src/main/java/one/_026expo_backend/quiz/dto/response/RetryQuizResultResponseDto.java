package one._026expo_backend.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "퀴즈 다시풀기 결과 응답")
@Getter
@Builder
public class RetryQuizResultResponseDto {

    @Schema(description = "전체 문제 개수", example = "3")
    private Integer totalCount;

    @Schema(description = "정답 개수", example = "2")
    private Integer correctCount;

    @Schema(description = "오답 개수", example = "1")
    private Integer wrongCount;

    @Schema(description = "정답률", example = "66")
    private Integer correctRate;

    @Schema(description = "획득 경험치. 다시풀기는 경험치를 지급하지 않으므로 항상 0", example = "0")
    private Integer earnedExp;

    @Schema(description = "결과 문구")
    private String resultMessage;

    public static RetryQuizResultResponseDto of(Integer totalCount, Integer correctCount, String resultMessage) {
        int correctRate = totalCount == 0 ? 0 : (correctCount * 100) / totalCount;

        return RetryQuizResultResponseDto.builder()
                .totalCount(totalCount)
                .correctCount(correctCount)
                .wrongCount(totalCount - correctCount)
                .correctRate(correctRate)
                .earnedExp(0)
                .resultMessage(resultMessage)
                .build();
    }
}