package one._026expo_backend.quiz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;


@Schema(description = "퀴즈 종료 및 결과 정산 응답")
@Getter
@Builder
public class QuizResultResponseDto {

    @Schema(description = "전체 문제 개수", example = "5")
    private Integer totalCount;

    @Schema(description = "정답 개수", example = "4")
    private Integer correctCount;

    @Schema(description = "오답 개수", example = "1")
    private Integer wrongCount;

    @Schema(description = "정답률", example = "80")
    private Integer correctRate;

    @Schema(description = "획득 경험치", example = "8")
    private Integer earnedExp;

    @Schema(description = "결과 문구", example = "완벽해요! 진짜 분리수거 박사네요")
    private String resultMessage;

    @Schema(description = "경험치 반영 전 레벨", example = "1")
    private Integer beforeLevel;

    @Schema(description = "경험치 반영 전 경험치", example = "30")
    private Integer beforeExp;

    @Schema(description = "경험치 반영 후 현재 레벨", example = "1")
    private Integer currentLevel;

    @Schema(description = "경험치 반영 후 현재 경험치", example = "38")
    private Integer currentExp;

    @Schema(description = "레벨업 여부", example = "false")
    private Boolean levelUp;

    @Schema(description = "다음 레벨까지 필요한 최대 경험치", example = "100")
    private Integer maxExp;

    @Schema(description = "현재 레벨 기준 경험치 퍼센트", example = "38")
    private Integer expPercent;

    @Schema(description = "다음 레벨까지 남은 경험치", example = "62")
    private Integer remainingExp;

    @Schema(description = "유저가 보유한 캐릭터 ID", example = "1")
    private Long userCharacterId;

    @Schema(description = "현재 적용된 캐릭터 원본 ID", example = "3")
    private Long characterId;

    @Schema(description = "현재 적용된 캐릭터 이름", example = "갓 태어난 아기")
    private String characterName;

    @Schema(description = "현재 적용된 캐릭터 이미지 URL", example = "https://minio.oneexpo.kro.kr/expo/character/3.png")
    private String characterImageUrl;

    @Schema(description = "현재 적용된 캐릭터 진화 단계", example = "2")
    private Integer evolutionStage;

    public static QuizResultResponseDto of(
            Integer totalCount,
            Integer correctCount,
            Integer earnedExp,
            String resultMessage,
            Long userCharacterId,
            Long characterId,
            String characterName,
            String characterImageUrl,
            Integer evolutionStage,
            Integer beforeLevel,
            Integer beforeExp,
            Integer currentLevel,
            Integer currentExp,
            Integer maxExp
    ) {
        int correctRate = totalCount == 0 ? 0 : (correctCount * 100) / totalCount;
        int expPercent = maxExp == 0 ? 0 : (currentExp * 100) / maxExp;
        boolean levelUp = currentLevel > beforeLevel;

        return QuizResultResponseDto.builder()
                .totalCount(totalCount)
                .correctCount(correctCount)
                .wrongCount(totalCount - correctCount)
                .correctRate(correctRate)
                .earnedExp(earnedExp)
                .resultMessage(resultMessage)
                .beforeExp(beforeExp)
                .currentLevel(currentLevel)
                .currentExp(currentExp)
                .levelUp(levelUp)
                .maxExp(maxExp)
                .expPercent(expPercent)
                .remainingExp(maxExp - currentExp)
                .userCharacterId(userCharacterId)
                .characterId(characterId)
                .characterName(characterName)
                .characterImageUrl(characterImageUrl)
                .evolutionStage(evolutionStage)
                .beforeLevel(beforeLevel)
                .build();
    }
}