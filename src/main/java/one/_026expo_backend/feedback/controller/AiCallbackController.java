package one._026expo_backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.dto.request.AiDetectionRequestDto;
import one._026expo_backend.feedback.service.AiDetectionService;
import one._026expo_backend.global.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai-callback")
@RequiredArgsConstructor
@Tag(name = "ai-callback", description = "AI 판별 결과 웹훅 엔드포인트")
public class AiCallbackController {

    private final AiDetectionService aiDetectionService;

    /**
     * AI 쓰레기 분류 결과 저장 API
     *
     * api 요청 예시 : POST /api/v1/ai-callback/result
     * 응답 데이터 : DB에 저장된 AI 판별 결과의 식별자(ID)
     */
    @Operation(summary = "AI 쓰레기 분류 결과 저장",
            description = "AI 서버에서 판별 완료 후 전송하는 쓰레기 분류 결과(status)와 식별값(client_id)을 DB에 저장합니다.")
    @PostMapping("/result")
    public ResponseEntity<ApiResponse<Long>> receiveAndSaveAiResult(
            @Valid @RequestBody AiDetectionRequestDto requestDto
    ) {
        Long savedId = aiDetectionService.saveAiResult(requestDto);
        return ResponseEntity.ok(ApiResponse.ok(savedId));
    }
}