package one._026expo_backend.quiz.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;

import java.util.Arrays;

public enum QuizAnswer {
    O, X;

    @JsonCreator
    public static QuizAnswer from(String value) {

        return Arrays.stream(values())
                .filter(a -> a.name().equals(value))
                .findFirst()  // 찾은 거 반환
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_QUIZ_ANSWER));
    }
}
