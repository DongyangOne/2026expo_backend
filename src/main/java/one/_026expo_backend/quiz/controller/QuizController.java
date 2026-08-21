package one._026expo_backend.quiz.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.config.swagger.ApiErrorExceptions;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.quiz.dto.request.NextQuizRequestDto;
import one._026expo_backend.quiz.dto.response.*;
import one._026expo_backend.quiz.dto.request.StartQuizRequestDto;
import one._026expo_backend.quiz.service.QuizService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.validation.annotation.Validated;

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
     * api 요청 예시 : POST /api/v1/quiz/sessions
     * 요청 데이터 : 퀴즈 개수(quantity)
     * 응답 데이터 : 퀴즈 리스트를 저장하는 redis session id, 첫번째 퀴즈 정보(id, 문제)
     */
    @ApiErrorExceptions({ErrorCode.QUIZ_NOT_FOUND, ErrorCode.NOT_ENOUGH_QUIZ, ErrorCode.USER_NOT_FOUND, ErrorCode.QUIZ_SESSION_SAVE_FAILED})
    @Operation(summary = "퀴즈 시작", description = "유저가 선택한 퀴즈 개수를 이용하여 퀴즈 id리스트와 첫번째 문제 정보를 불러옵니다.")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<StartQuizResponseDto>> startQuiz(
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid StartQuizRequestDto requestDto
    ){
        StartQuizResponseDto responseDto = quizService.startQuiz(userId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }
    /**
     * 정답 제출 및 다음 퀴즈 조회 API
     *
     * api 요청 예시 : POST /api/v1/quiz/sessions/{sessionId}/answers
     * 요청 데이터 : sessionId, NextQuizRequestDto
     * 응답 데이터 : NextQuizResponseDto
     */
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.QUIZ_NOT_FOUND, ErrorCode.INVALID_QUIZ_ANSWER, ErrorCode.ALREADY_SOLVED_QUIZ,
                    ErrorCode.QUIZ_SESSION_NOT_FOUND, ErrorCode.INVALID_QUIZ_SEQUENCE, ErrorCode.INVALID_QUIZ_SESSION,
                    ErrorCode.QUIZ_SESSION_READ_FAILED, ErrorCode.QUIZ_SESSION_UPDATE_FAILED, ErrorCode.QUIZ_SESSION_COMPLETE_FAILED,
                    ErrorCode.INVALID_QUIZ_ID, ErrorCode.MISSING_QUIZ_ANSWER, ErrorCode.MISSING_SESSION_ID, ErrorCode.INVALID_SESSION_FORMAT,
                    ErrorCode.QUIZ_SESSION_STATE_CONFLICT})
    @Operation(summary = "정답 제출 및 다음 퀴즈 조회", description = "요청 정보를 이용하여 현재 문제를 채점 및 기록하고 다음 퀴즈 정보를 가져옵니다.")
    @PostMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<ApiResponse<NextQuizResponseDto>> submitAnswerAndNextQuiz(
            @AuthenticationPrincipal Long userId,
            @PathVariable String sessionId,
            @RequestBody NextQuizRequestDto requestDto
    ){
        NextQuizResponseDto responseDto = quizService.moveOnQuiz(userId, sessionId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

    /**
     * 퀴즈 종료 및 결과정산 API
     *
     * api 요청 예시 : POST /api/v1/quiz/sessions/{sessionId}/result
     * 요청 데이터 : QuizResultRequestDto
     * 응답 데이터 : QuizResponseResponseDto
     */
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.QUIZ_NOT_FINISHED, ErrorCode.QUIZ_RESULT_RECORD_NOT_MATCHED, ErrorCode.USER_CHARACTER_NOT_FOUND,
                        ErrorCode.INVALID_QUIZ_SESSION, ErrorCode.INVALID_SESSION_FORMAT, ErrorCode.MISSING_SESSION_ID})
    @Operation(summary = "퀴즈 종료 및 결과 정산", description = "완료된 퀴즈 세션의 결과를 집계하고 캐릭터 경험치를 증가시킵니다.")
    @PostMapping("/sessions/{sessionId}/result")
    public ResponseEntity<ApiResponse<QuizResultResponseDto>> resultQuiz
    (
            @AuthenticationPrincipal Long userId,
            @Parameter(
                    name = "sessionId",
                    description = "정산할 퀴즈의 세션 ID",
                    example = "550e8400-e29b-41d4-a716-446655440000"
            )
            @PathVariable("sessionId") String sessionId
    ) {
        QuizResultResponseDto responseDto = quizService.resultQuiz(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

    /**
     * 퀴즈 다시풀기 시작 API
     *
     * api 요청 예시 : POST /api/v1/quiz/sessions/{sessionId}/retry
     * 원본 sessionId의 오답 문제만 Redis에 다시풀기 세션으로 저장합니다.
     */
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.QUIZ_NOT_FOUND,
            ErrorCode.INVALID_SESSION_FORMAT, ErrorCode.MISSING_SESSION_ID,
            ErrorCode.QUIZ_SESSION_SAVE_FAILED, ErrorCode.NOT_LATEST_QUIZ_SESSION})
    @Operation(summary = "퀴즈 다시풀기 시작", description = "기존 퀴즈 세션에서 틀린 문제만 가져와 다시풀기 세션을 시작합니다.")
    @PostMapping("/sessions/{sessionId}/retry")
    public ResponseEntity<ApiResponse<RetryQuizStartResponseDto>> startRetryQuiz(
            @AuthenticationPrincipal Long userId,
            @PathVariable String sessionId
    ) {
        RetryQuizStartResponseDto responseDto = quizService.startRetryQuiz(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

    /**
     * 다시풀기 정답 제출 및 다음 퀴즈 조회 API
     *
     * api 요청 예시 : POST /api/v1/quiz/retry-sessions/{sessionId}/answers
     */
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.QUIZ_NOT_FOUND,
            ErrorCode.INVALID_QUIZ_ANSWER, ErrorCode.ALREADY_SOLVED_QUIZ,
            ErrorCode.QUIZ_SESSION_NOT_FOUND, ErrorCode.INVALID_QUIZ_SEQUENCE,
            ErrorCode.INVALID_QUIZ_SESSION, ErrorCode.QUIZ_SESSION_READ_FAILED,
            ErrorCode.QUIZ_SESSION_UPDATE_FAILED, ErrorCode.QUIZ_SESSION_COMPLETE_FAILED,
            ErrorCode.INVALID_QUIZ_ID, ErrorCode.MISSING_QUIZ_ANSWER,
            ErrorCode.MISSING_SESSION_ID, ErrorCode.INVALID_SESSION_FORMAT,
            ErrorCode.QUIZ_SESSION_STATE_CONFLICT})
    @Operation(summary = "다시풀기 정답 제출 및 다음 퀴즈 조회", description = "다시풀기 세션의 현재 문제를 채점하고 다음 문제를 반환합니다.")
    @PostMapping("/retry-sessions/{sessionId}/answers")
    public ResponseEntity<ApiResponse<NextQuizResponseDto>> submitRetryAnswerAndNextQuiz(
            @AuthenticationPrincipal Long userId,
            @PathVariable String sessionId,
            @RequestBody NextQuizRequestDto requestDto
    ) {
        NextQuizResponseDto responseDto = quizService.moveOnRetryQuiz(userId, sessionId, requestDto);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

    /**
     * 다시풀기 결과 조회 API
     *
     * api 요청 예시 : POST /api/v1/quiz/retry-sessions/{sessionId}/result
     */
    @ApiErrorExceptions({ErrorCode.USER_NOT_FOUND, ErrorCode.QUIZ_NOT_FINISHED,
            ErrorCode.QUIZ_RESULT_RECORD_NOT_MATCHED, ErrorCode.INVALID_QUIZ_SESSION,
            ErrorCode.INVALID_SESSION_FORMAT, ErrorCode.MISSING_SESSION_ID})
    @Operation(summary = "다시풀기 결과 조회", description = "완료된 다시풀기 세션의 결과를 조회합니다. 경험치는 지급하지 않습니다.")
    @PostMapping("/retry-sessions/{sessionId}/result")
    public ResponseEntity<ApiResponse<RetryQuizResultResponseDto>> resultRetryQuiz(
            @AuthenticationPrincipal Long userId,
            @PathVariable String sessionId
    ) {
        RetryQuizResultResponseDto responseDto = quizService.resultRetryQuiz(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.ok(responseDto));
    }

}
