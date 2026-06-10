package one._026expo_backend.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.quiz.dto.request.StartQuizRequestDto;
import one._026expo_backend.quiz.dto.response.StartQuizResponseDto;
import one._026expo_backend.quiz.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
@Tag(name = "quiz", description = "퀴즈 엔드포인트")
@Validated
public class QuizController {
    private final QuizService quizService;
    /**
     * 퀴즈 시작 API
     *
     * api 요청 예시 : POST /api/v1/quiz/start
     * 요청 데이터 : 퀴즈 개수(quantity)
     * 응답 데이터 : 퀴즈 리스트를 저장하는 redis session id, 첫번째 퀴즈 정보(id, 문제)
     */
    @ApiErrorExceptions({ErrorCode.QUIZ_NOT_FOUND, ErrorCode.NOT_ENOUGH_QUIZ, ErrorCode.USER_NOT_FOUND})
    @Operation(summary = "퀴즈 시작", description = "유저가 선택한 퀴즈 개수를 이용하여 퀴즈 id리스트와 첫번재 문제 정보를 불러옵니다.")
    @PostMapping("/start")
    public ResponseEntity<ApiResponse<StartQuizResponseDto>> StartQuiz(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid StartQuizRequestDto requestDto
    ){
        StartQuizResponseDto responseDto = quizService.startQuiz(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }
}
