package one._026expo_backend.feedback.enums;

public enum DetectionStatus {
    ALLOWED,         // 재활용 허용
    REJECTED,        // 조건 불충족 또는 완전거부
    GENERAL_WASTE,   // 일반쓰레기
    NOT_DETECTED     // 미감지
}