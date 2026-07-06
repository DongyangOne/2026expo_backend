package one._026expo_backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.dto.response.FeedbackDetailResponseDto;
import one._026expo_backend.feedback.service.FeedbackDetailService;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/feedback-detail")
@RequiredArgsConstructor
@Validated
@Tag(name = "feedbackDetail", description = "피드백 디테일 엔드포인트")
public class FeedbackDetailController {
    private final FeedbackDetailService feedbackDetailService;

    /**
     * 피드백 상세 조회 API
     *
     * api 요청 예시 : GET /api/v1/feedback-detail/{feedbackId}
     * 요청 데이터 : feedbackId (경로 변수)
     * 응답 데이터 : 특정 피드백의 성공 여부, 날짜/시간, 쓰레기 종류 및 분리수거 상세 가이드(영상 URL, 설명 내용)
     */
    @Operation(summary = "피드백 상세 조회", description = "특정 피드백의 상세 정보와 분리수거 가이드 영상/내용을 조회합니다.")
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.FEEDBACK_NOT_FOUND, ErrorCode.ACCESS_DENIED, ErrorCode.FEEDBACK_DETAIL_NOT_FOUND, ErrorCode.INVALID_INPUT})
    @GetMapping("/{feedbackId}")
    public ResponseEntity<ApiResponse<FeedbackDetailResponseDto>> getFeedbackDetail(
            @AuthenticationPrincipal Long userId,
            @Parameter(description = "피드백 id", example = "3")
            @PathVariable("feedbackId") @Positive Long feedbackId
    ) {
        FeedbackDetailResponseDto response = feedbackDetailService.getFeedbackDetail(userId, feedbackId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
