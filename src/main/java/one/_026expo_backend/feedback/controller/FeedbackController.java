package one._026expo_backend.feedback.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.dto.response.FeedbackListResponseDto;
import one._026expo_backend.feedback.service.FeedbackService;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.pagination.PageRequestDto;
import one._026expo_backend.global.pagination.PageResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
@Tag(name = "feedback", description = "피드백 엔드포인트")
public class FeedbackController {
    private final FeedbackService feedbackService;


    /**
     * 피드백 리스트 조회 API
     *
     * api 요청 예시 : GET /api/v1/feedback
     * 응답 데이터 : 로그인한 사용자의 피드백 리스트(최신순)
     */
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND})
    @Operation(summary = "피드백 리스트 조회",
            description = "로그인한 사용자의 피드백 리스트를 최신순(날짜, 시간별)으로 정렬하여 페이징 처리된 데이터로 반환합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponseDto<FeedbackListResponseDto>>> getFeedbackList(
            @AuthenticationPrincipal Long userId,
            @Valid @ModelAttribute PageRequestDto pageRequestDto
    ) {
        PageResponseDto<FeedbackListResponseDto> response = feedbackService.getFeedbackList(userId, pageRequestDto);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
