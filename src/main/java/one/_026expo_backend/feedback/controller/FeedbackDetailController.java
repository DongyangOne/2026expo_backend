package one._026expo_backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.dto.request.FeedbackDetailRequestDto;
import one._026expo_backend.feedback.dto.response.FeedbackDetailResponseDto;
import one._026expo_backend.feedback.service.FeedbackDetailService;
import one._026expo_backend.feedback.dto.request.AiFeedbackRequestDto;
import one._026expo_backend.feedback.dto.response.AiDetectionCreateResponseDto;
import one._026expo_backend.feedback.service.AiDetectionService;
import one._026expo_backend.feedback.service.FeedbackService;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedbackDetail")
@RequiredArgsConstructor
@Validated
@Tag(name = "feedbackDetail", description = "피드백 디테일 엔드포인트")
public class FeedbackDetailController {
    private final FeedbackDetailService feedbackDetailService; 
    private final FeedbackService feedbackService;
    private final AiDetectionService aiDetectionService;


    /**
     * 피드백 상세 조회 API
     *
     * api 요청 예시 : GET /api/v1/feedbackDetail/3
     * 요청 데이터 : feedbackId (경로 변수)
     * 응답 데이터 : 특정 피드백의 성공 여부, 날짜/시간, 쓰레기 종류 및 분리수거 상세 가이드(영상 URL, 설명 내용)
     */
    @Operation(summary = "피드백 상세 조회", description = "특정 피드백의 상세 정보와 분리수거 가이드 영상/내용을 조회합니다.")
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.FEEDBACK_NOT_FOUND, ErrorCode.ACCESS_DENIED})
    @GetMapping("/{feedbackId}")
    public ResponseEntity<ApiResponse<FeedbackDetailResponseDto>> getFeedbackDetail(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute FeedbackDetailRequestDto requestDto
    ) {
        FeedbackDetailResponseDto response = feedbackDetailService.getFeedbackDetail(userId, requestDto.getFeedbackId());
      
      return ResponseEntity.ok(ApiResponse.ok(response));
    }


    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.MISSING_FEEDBACK_TEXT})
    @Operation(summary = "AI 쓰레기 분류 결과 수신",
            description = "AI 하드웨어 기기에서 쓰레기 분류가 끝난 후 결과를 백엔드로 전송할 때 호출하는 콜백(Webhook) API입니다.<br>" +
                    "성공/실패 여부, 실패 시 사유, 쓰레기 종류 등의 데이터를 전달받아 사용자의 피드백 기록으로 저장합니다."
    )
    @PostMapping("/result")
    public ApiResponse<Void> receiveAiResult(@Valid @RequestBody AiFeedbackRequestDto requestDto) {
        feedbackService.saveAiFeedback(requestDto);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "AI 검사 시작", description = "검사 고유 식별자인 clientId를 생성하고 로그인 사용자와 연결합니다.")
    @PostMapping("/detections")
    public ResponseEntity<ApiResponse<AiDetectionCreateResponseDto>>
    createDetection(@AuthenticationPrincipal Long userId) {
        AiDetectionCreateResponseDto response = aiDetectionService.createDetection(userId);

        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
