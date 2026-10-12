package one._026expo_backend.feedback.enums;

public enum GuidanceCode {
    EMPTY_CONTENTS,   // 플라스틱·페트·캔 무게 이상 또는 내용물 존재
    WEIGHT_ANOMALY,   // 종이·비닐 무게 이상
    REMOVE_STRAW,     // 컵에 부착된 빨대 제거
    REMOVE_CUP_HOLDER,// 컵에 부착된 종이 홀더 제거
    FOREIGN_MATERIAL, // 외부 이물질
    REMOVE_LABEL,     // 라벨 미제거
    COMPRESS          // 미압착
}
