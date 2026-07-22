package one._026expo_backend.feedback.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.global.enums.UseYnEnum;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
@Schema(description = "관리자 피드백 조회 데이터")
public class AdminFeedbackResponseDto {

    @Schema(description = "피드백 ID", example = "3")
    private Long feedbackId;

    @Schema(description = "분리수거 날짜", example = "2026.05.06")
    private String date;

    @Schema(description = "버린 시간", example = "13:34")
    private String time;

    @Schema(description = "사용자 이름", example = "최예은")
    private String username;

    @Schema(description = "올바른 분리수거 여부", example = "false")
    private Boolean isSuccess;

    @Schema(description = "분리수거 결과 내용", example = "올바른 분리수거가 이루어지지 않았어요. (캔 안에 내용물이 들어있었음)")
    private String content;

    /**
     * 피드백 엔티티를 관리자 화면의 한 행으로 변환
     *
     * @param feedback 사용자 피드백 기록
     * @return 관리자 화면 응답 데이터
     */
    public static AdminFeedbackResponseDto from(Feedback feedback) {
        boolean isSuccess = feedback.getIsFailed() == UseYnEnum.N;

        String wasteName = feedback.getWasteType().getDescription();

        String content;

        if (isSuccess) {
            content = wasteName + "을(를) 올바르게 분리수거하셨어요.";
        } else {
            content = "올바른 분리수거가 이루어지지 않았어요.";

            if (feedback.getFeedbackText() != null && !feedback.getFeedbackText().isBlank()) {
                content += "\n(" + feedback.getFeedbackText() + ")";
            }
        }

        return AdminFeedbackResponseDto.builder()
                .feedbackId(feedback.getFeedbackId())
                .date(feedback.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("yyyy.MM.dd")))
                .time(feedback.getCreatedAt()
                        .format(DateTimeFormatter.ofPattern("HH:mm")))
                .username(feedback.getUser().getUsername())
                .isSuccess(isSuccess)
                .content(content)
                .build();
    }
}
