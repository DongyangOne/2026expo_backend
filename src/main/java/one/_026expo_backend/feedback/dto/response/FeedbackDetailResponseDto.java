package one._026expo_backend.feedback.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.domain.FeedbackDetail;
import one._026expo_backend.global.enums.UseYnEnum;

import java.time.format.DateTimeFormatter;

@Schema(description = "피드백 상세 조회 응답 데이터")
@Getter
@Builder
public class FeedbackDetailResponseDto {

    @Schema(description = "피드백 ID", example = "3")
    private Long feedbackId;

    @Schema(description = "피드백 날짜", example = "2026.05.06")
    private String date;

    @Schema(description = "피드백 시간", example = "12:34")
    private String time;

    @Schema(description = "올바른 분리수거 여부 (true: 성공, false: 실패)", example = "false")
    private Boolean isSuccess;

    @Schema(description = "쓰레기 종류", example = "캔")
    private String wasteType;

    @Schema(description = "피드백 제목", example = "캔을 올바르게 버리지 못했어요.")
    private String title;

    @Schema(description = "분리수거 가이드 영상 URL", example = "https://s3.ap-northeast-2.amazonaws.com/...")
    private String videoUrl;

    @Schema(description = "분리수거 가이드 내용", example = "1. 안에 내용물이 없어야 합니다.\n2. 물로 헹군 후 배출해야 합니다.")
    private String content;

    public static FeedbackDetailResponseDto of(Feedback feedback, FeedbackDetail detail) {
        boolean isSuccess = feedback.getIsFailed() == UseYnEnum.N;
        String wasteName = feedback.getWasteType().getDescription();

        String suffix = (wasteName.equals("종이") || wasteName.equals("일반 쓰레기")) ? "를" : "을";

        String generatedTitle = isSuccess ?
                wasteName + suffix + " 올바르게 분리수거하셨어요." :
                wasteName + suffix + " 올바르게 버리지 못했어요.";

        return FeedbackDetailResponseDto.builder()
                .feedbackId(feedback.getFeedbackId())
                .date(feedback.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .time(feedback.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")))
                .isSuccess(isSuccess)
                .wasteType(wasteName)
                .title(generatedTitle)
                .videoUrl(detail.getFeedbackVideoAddr())
                .content(detail.getFeedbackContent())
                .build();
    }
}