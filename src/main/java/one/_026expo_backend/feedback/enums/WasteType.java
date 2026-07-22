package one._026expo_backend.feedback.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WasteType {
    // 수거 허용 쓰레기
    PLASTIC("플라스틱"),
    CAN("캔"),
    PAPER("종이"),
    VINYL("비닐"),

    // 수거 비허용 쓰레기
    GLASS("유리병"),
    BATTERY("건전지"),
    FLUORESCENT("형광등"),
    STYROFOAM("스티로폼");

    private final String description;
}