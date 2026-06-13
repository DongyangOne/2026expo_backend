package one._026expo_backend.user.enums;

/**
 * 사용자 인증 코드의 사용 목적을 구분합니다.
 *
 * Redis 키를 재사용하더라도 목적을 함께 저장해, 이후 검증 API가 다른 인증 흐름과 혼동하지 않도록 합니다.
 */
public enum UserVerificationPurpose {
    MYPAGE_USER_VERIFICATION
}
