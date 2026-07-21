package one._026expo_backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.dto.request.AiFeedbackRequestDto;
import one._026expo_backend.feedback.service.FeedbackService;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "AI Callback", description = "AI 하드웨어 쓰레기 분류 결과 웹훅 API")
@RestController
@RequestMapping("/api/v1/ai-callback")
@RequiredArgsConstructor
public class AiCallbackController {
    private final FeedbackService feedbackService;

    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.MISSING_FEEDBACK_TEXT})
    @Operation(
            summary = "AI 쓰레기 분류 결과 수신",
            description = "AI 하드웨어 기기에서 쓰레기 분류가 끝난 후 결과를 백엔드로 전송할 때 호출하는 콜백(Webhook) API입니다.<br>" +
                    "성공/실패 여부, 실패 시 사유, 쓰레기 종류 등의 데이터를 전달받아 사용자의 피드백 기록으로 저장합니다."
    )
    @PostMapping("/result")
    public ResponseEntity<ApiResponse<Void>> receiveAiResult(@Valid @RequestBody AiFeedbackRequestDto requestDto) {
        feedbackService.saveAiFeedback(requestDto);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}