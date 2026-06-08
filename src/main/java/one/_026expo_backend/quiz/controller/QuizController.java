package one._026expo_backend.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.quiz.dto.request.NextQuizRequestDto;
import one._026expo_backend.quiz.dto.response.NextQuizResponseDto;
import one._026expo_backend.quiz.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
@Tag(name = "quiz", description = "퀴즈 엔드포인트")
public class QuizController {
    private final QuizService quizService;
    /**
     * 정답 제출 및 다음 퀴즈 조회 API
     *
     * api 요청 예시 : POST /api/v1/quiz/next
     * 요청 데이터 : NextQuizRequestDto
     * 응답 데이터 : NextQuizResponseDto
     */
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.QUIZ_NOT_FOUND, ErrorCode.INVALID_QUIZ_ANSWER})
    @Operation(summary = "정답 제출 및 다음 퀴즈 조회", description = "요청 정보를 이용하여 현재 문제를 채점 및 기록하고 다음 퀴즈 정보를 가져옵니다.")
    @PostMapping("/next")
    public ResponseEntity<ApiResponse<NextQuizResponseDto>> StartQuiz(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid NextQuizRequestDto requestDto
    ){
        NextQuizResponseDto responseDto = quizService.moveOnQuiz(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

}
