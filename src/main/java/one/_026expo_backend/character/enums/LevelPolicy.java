package one._026expo_backend.character.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;


@Getter
@RequiredArgsConstructor
public enum LevelPolicy {
    // 맥스 경험치 100부터 시작해서 200씩 추가
    LEVEL_0(0, 100),    // 알
    LEVEL_1(1, 300),    // 알에 금가기 시작 (1~3)
    LEVEL_2(2, 500),
    LEVEL_3(3, 700),
    LEVEL_4(4, 900),    // 갓 태어난 응애 (4~6)
    LEVEL_5(5, 1100),
    LEVEL_6(6, 1300),
    LEVEL_7(7, 1500),   // 유치원생 (7~9)
    LEVEL_8(8, 1700),
    LEVEL_9(9, 1900),
    LEVEL_10(10, 999999); // 초등학생 (만렙) - 더 이상 렙업하지 않도록 큰 값 부여

    private final int level;
    private final int maxExp;

    // 현재 레벨에 해당하는 최대 경험치를 가져오는 메서드
    public static int getMaxExpForLevel(int level) {
        for (LevelPolicy policy : values()) {
            if (policy.getLevel() == level) {
                return policy.getMaxExp();
            }
        }
        // Enum에 정의된 범위를 벗어나면 LEVEL_10(만렙)의 경험치 반환
        return LEVEL_10.getMaxExp();
    }
}