package one._026expo_backend.feedback.enums;

public enum GuidanceCode {
    EMPTY_CONTENTS,   // 플라스틱·페트·캔 무게 이상 또는 내용물 존재
    WEIGHT_ANOMALY,   // 종이·비닐 무게 이상
    FOREIGN_MATERIAL, // 외부 이물질
    REMOVE_LABEL,     // 라벨 미제거
    COMPRESS          // 미압착
}
