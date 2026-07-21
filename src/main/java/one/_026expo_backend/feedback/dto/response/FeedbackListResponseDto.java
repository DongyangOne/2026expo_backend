package one._026expo_backend.feedback.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.global.enums.UseYnEnum;

import java.time.format.DateTimeFormatter;

/**
 * 피드백 리스트 조회 시 리스트의 요소가 될 데이터 구조
 */
@Schema(description = "피드백 리스트 요소 데이터 구조")
@Getter
@Builder
@AllArgsConstructor
public class FeedbackListResponseDto {
    @Schema(description = "피드백 id", example = "3")
    private Long feedbackId;

    @Schema(description = "날짜", example = "2026.05.06")
    private String date;

    @Schema(description = "시간", example = "13:34")
    private String time;

    @Schema(description = "올바른 분리수거 성공 여부", example = "true")
    private Boolean isSuccess;

    @Schema(description = "쓰레기 종류", example = "캔")
    private String wasteType;

    @Schema(description = "피드백 내용 (성공 시 null)", example = "캔에 음식물이 들어있었다", nullable = true)
    private String feedbackText;


    public static FeedbackListResponseDto from(Feedback feedback) {
        boolean isSuccess = feedback.getIsFailed() == UseYnEnum.N;

        String formattedDate = feedback.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd"));
        String formattedTime = feedback.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm"));

        return FeedbackListResponseDto.builder()
                .feedbackId(feedback.getFeedbackId())
                .date(formattedDate)
                .time(formattedTime)
                .isSuccess(isSuccess)
                .wasteType(feedback.getWasteType().getDescription())
                .feedbackText(feedback.getFeedbackText())
                .build();
    }
}