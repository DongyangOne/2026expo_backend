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
}