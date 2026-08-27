package one._026expo_backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.feedback.dto.response.AdminFeedbackResponseDto;
import one._026expo_backend.feedback.dto.response.FeedbackDetailResponseDto;
import one._026expo_backend.feedback.service.FeedbackDetailService;
import one._026expo_backend.feedback.dto.request.AiFeedbackRequestDto;
import one._026expo_backend.feedback.dto.response.AiDetectionCreateResponseDto;
import one._026expo_backend.feedback.service.AiDetectionService;
import one._026expo_backend.feedback.service.FeedbackService;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.pagination.PageRequestDto;
import one._026expo_backend.global.pagination.PageResponseDto;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback-detail")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "feedbackDetail", description = "피드백 디테일 엔드포인트")
public class FeedbackDetailController {
    private final FeedbackDetailService feedbackDetailService;
    private final FeedbackService feedbackService;
    private final AiDetectionService aiDetectionService;


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

    @Operation(summary = "관리자 피드백 조회", description = "로그인한 관리자와 소속이 같은 사용자의 피드백을 최신순으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDto<AdminFeedbackResponseDto>>> getFeedbacks(
            @AuthenticationPrincipal Long adminId,
            @Valid @ParameterObject PageRequestDto pageRequestDto
    ) {
        PageResponseDto<AdminFeedbackResponseDto> response = feedbackDetailService.getFeedbacks(adminId, pageRequestDto);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.MISSING_FEEDBACK_TEXT})
    @Operation(summary = "AI 쓰레기 분류 결과 수신",
            description = "AI 하드웨어 기기에서 쓰레기 분류가 끝난 후 결과를 백엔드로 전송할 때 호출하는 콜백(Webhook) API입니다.<br>" +
                    "성공/실패 여부, 실패 시 사유, 쓰레기 종류 등의 데이터를 전달받아 사용자의 피드백 기록으로 저장합니다."
    )
    @PostMapping("/result")
    public ApiResponse<Void> receiveAiResult(@Valid @RequestBody AiFeedbackRequestDto requestDto) {
        log.info("[AI_CALLBACK_RECEIVED] clientId={}, status={}", requestDto.getClientId(), requestDto.getStatus());
        feedbackService.saveAiFeedback(requestDto);
        log.info("[AI_CALLBACK_HANDLED] clientId={}", requestDto.getClientId());
        return ApiResponse.ok(null);
    }

    @Operation(summary = "AI 검사 시작", description = "검사 고유 식별자인 clientId를 생성하고 로그인 사용자와 연결합니다.")
    @PostMapping("/detections")
    public ResponseEntity<ApiResponse<AiDetectionCreateResponseDto>>
    createDetection(@AuthenticationPrincipal Long userId) {
        AiDetectionCreateResponseDto response = aiDetectionService.createDetection(userId);
        log.info("[AI_DETECTION_START_RESPONSE] userId={}, clientId={}", userId, response.getClientId());

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
