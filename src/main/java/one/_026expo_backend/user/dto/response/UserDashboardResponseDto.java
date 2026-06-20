package one._026expo_backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import one._026expo_backend.feedback.enums.WasteType;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Schema(description = "사용자 정보(레벨, 퀴즈결과, 분리수거 로그) 응답 DTO")
public class UserDashboardResponseDto {

    @Schema(description = "사용자 캐릭터 정보")
    private CharacterInfo characterInfo;

    @Schema(description = "퀴즈 정답률")
    private QuizProfileInfo quizProfileInfo;

    @Schema(description = "최근 10개 분리수거 로그")
    private List<RecyclingLogInfo> recyclingLogInfo;


    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "1. 내부 케릭터 정보 DTO")
    public static class CharacterInfo {
        @Schema(description = "캐릭터 고유 ID", example = "1")
        private Long characterId;

        @Schema(description = "캐릭터 이름", example = "알")
        private String characterName;

        @Schema(description = "MinIO 이미지 URL", example = "https://minio-storage.../.../0.png")
        private String imageUrl;

        @Schema(description = "현재 진화 단계", example = "1")
        private Integer evolutionStage;

        public static CharacterInfo of(
                Long characterId, String characterName, String imageUrl, Integer evolutionStage) {
            return CharacterInfo.builder()
                    .characterId(characterId)
                    .characterName(characterName)
                    .imageUrl(imageUrl)
                    .evolutionStage(evolutionStage)
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "2. 내부 퀴즈 프로필 DTO")
    public static class QuizProfileInfo {
        @Schema(description = "맞춘 문제 수", example = "8")
        private Integer correctQuiz;

        @Schema(description = "푼 문제 수", example = "10")
        private Integer solvedQuiz;

        public static QuizProfileInfo of(Integer correctQuiz, Integer solvedQuiz) {
            return QuizProfileInfo.builder()
                    .correctQuiz(correctQuiz)
                    .solvedQuiz(solvedQuiz)
                    .build();
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor
    @Schema(description = "3. 내부 분리수거 로그 DTO")
    public static class RecyclingLogInfo {
        @Schema(description = "쓰레기 종류", example = "CAN")
        private WasteType wasteType;

        @Schema(description = "분리수거 일시", example = "2026-06-14T11:20:00")
        private LocalDateTime recycledAt;

        public static RecyclingLogInfo of(WasteType wasteType, LocalDateTime recycledAt) {
            return RecyclingLogInfo.builder()
                    .wasteType(wasteType)
                    .recycledAt(recycledAt)
                    .build();
        }
    }

    public static UserDashboardResponseDto of(
            CharacterInfo characterInfo,
            QuizProfileInfo quizProfileInfo,
            List<RecyclingLogInfo> recyclingLogInfo) {
        return UserDashboardResponseDto.builder()
                .characterInfo(characterInfo)
                .quizProfileInfo(quizProfileInfo)
                .recyclingLogInfo(recyclingLogInfo)
                .build();
    }
}