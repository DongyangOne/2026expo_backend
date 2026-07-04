package one._026expo_backend.feedback.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WasteType {
    CAN("캔"),
    PET("페트병"),
    PAPER("종이"),
    TRASH("일반 쓰레기");

    private final String description;

    public String getParticle() {
        return (this == PAPER || this == TRASH)
                ? "를"
                : "을";
    }

    public String generateTitle(boolean success) {
        String action = success
                ? " 올바르게 분리수거하셨어요."
                : " 올바르게 버리지 못했어요.";

        return description + getParticle() + action;
    }
}