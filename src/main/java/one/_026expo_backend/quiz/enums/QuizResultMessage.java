package one._026expo_backend.quiz.enums;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public enum QuizResultMessage {

    ENCOURAGEMENT(
            0,
            79,
            List.of(
                    "조금만 더 집중하면 정답에 가까워져요",
                    "아직 괜찮아요! 다음 문제는 꼭 맞혀봐요",
                    "분리수거 고수까지 한 걸음 남았어요",
                    "헷갈릴 땐 재질 표시를 다시 확인해보세요",
                    "아쉽지만 실력이 점점 늘고 있어요",
                    "환경 지킴이 레벨업에 도전 중",
                    "이번엔 틀렸지만 경험치는 쌓였어요",
                    "다시 도전하면 더 잘할 수 있어요",
                    "분리배출 마스터는 반복 연습에서 시작됩니다",
                    "조금 더 공부해서 지구를 지켜봐요",
                    "거의 다 왔어요! 한 번 더 도전해봐요",
                    "다음 문제는 분명 정답일 거예요"
            )
    ),

    PRAISE(
            80,
            100,
            List.of(
                    "완벽해요! 진짜 분리수거 박사네요",
                    "정답입니다! 환경 지킴이 인정",
                    "대단해요! 정확하게 알고 있었네요",
                    "센스 최고! 올바르게 분리했어요",
                    "지구가 고마워하고 있어요",
                    "분리배출 실력이 엄청난데요"
            )
    );

    private final int minCorrectRate;
    private final int maxCorrectRate;
    private final List<String> messages;


    QuizResultMessage(int minCorrectRate, int maxCorrectRate, List<String> messages) {
        this.minCorrectRate = minCorrectRate;
        this.maxCorrectRate = maxCorrectRate;
        this.messages = messages;
    }


    /**
     * 계산된 정답률에 따라 격려(ENCOURAGEMENT)를 할지 칭찬(PRAISE)을 할지 그룹을 판별하고,
     * 사용자에게 상황에 맞는 적절한 퀴즈 결과 피드백을 제공하는 메소드
     *
     * @param correctCount 맞힌 문제 수
     * @param totalCount 전체 문제 수
     * @return 조건에 맞는 무작위 결과 메시지 문자열
     */
    public static String pick(Integer correctCount, Integer totalCount) {
        int correctRate = calculateCorrectRate(correctCount, totalCount);

        for (QuizResultMessage group : values()) {
            if (group.includes(correctRate)) {
                return group.randomMessage();
            }
        }

        return ENCOURAGEMENT.randomMessage();
    }

    /**
     * 사용자의 정답 수와 전체 문제 수를 이용해 백분율(%) 형태의 정답률을 정수로 계산합니다.
     *
     * @param correctCount 맞힌 문제 수
     * @param totalCount 전체 문제 수
     * @return 계산된 정답률 (0~100)
     */
    private static int calculateCorrectRate(Integer correctCount, Integer totalCount) {
        if (totalCount == null || totalCount == 0) {
            return 0;
        }

        return (correctCount * 100) / totalCount;
    }

    /**
     * 전달받은 정답률이 현재 메시지 그룹(Enum 상수)의 허용 범위(최소~최대 정답률) 내에 포함되는지 확인합니다.
     *
     * @param correctRate 계산된 정답률
     * @return 범위 포함 여부 (true/false)
     */
    private boolean includes(Integer correctRate) {
        return correctRate >= minCorrectRate && correctRate <= maxCorrectRate;
    }

    /**
     * 해당 메시지 그룹이 가지고 있는 여러 메시지 리스트 중 하나를 랜덤으로 뽑아 반환합니다.
     *
     * @return 무작위로 선택된 메시지
     */
    private String randomMessage() {
        int index = ThreadLocalRandom.current().nextInt(messages.size());
        return messages.get(index);
    }
}