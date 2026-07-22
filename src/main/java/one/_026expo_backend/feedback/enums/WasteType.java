package one._026expo_backend.feedback.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WasteType {
    // 수거 허용 쓰레기
    PLASTIC("플라스틱", "을"),
    CAN("캔", "을"),
    PAPER("종이", "를"),
    VINYL("비닐", "을"),

    // 수거 비허용 쓰레기
    GLASS("유리병", "을"),
    BATTERY("건전지", "를"),
    FLUORESCENT("형광등", "을"),
    STYROFOAM("스티로폼", "을");

    private final String description;
    private final String particle; // 조사(을/를)를 필드로 분리

    public String generateTitle(boolean success) {
        String action = success
                ? " 올바르게 분리수거하셨어요."
                : " 올바르게 버리지 못했어요.";

        return this.description + this.particle + action;
    }
}