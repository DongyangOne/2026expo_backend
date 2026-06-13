package one._026expo_backend.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.dto.request.UserVerificationEmailSendRequestDto;
import one._026expo_backend.user.dto.response.UserVerificationEmailSendResponseDto;
import one._026expo_backend.user.enums.UserVerificationPurpose;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * 마이페이지 사용자 인증용 이메일 발송을 담당합니다.
 *
 * 로그인 사용자 기준으로만 대상을 결정해, 클라이언트가 임의 이메일로 인증 메일을 보내지 못하게 합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserVerificationService {

    private static final String VERIFICATION_EMAIL_PREFIX = "USER_VERIFICATION_EMAIL:";
    private static final String VERIFICATION_EMAIL_LIMIT_PREFIX = "USER_VERIFICATION_EMAIL_LIMIT:";
    private static final int VERIFICATION_CODE_LENGTH = 6;
    private static final int VERIFICATION_CODE_EXPIRE_SECONDS = 300;
    private static final int RESEND_COOLDOWN_SECONDS = 60;

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserVerificationMailService userVerificationMailService;

    /**
     * 로그인한 사용자의 계정 이메일로 사용자 인증 번호를 발송합니다.
     *
     * @param userId 인증된 사용자 식별자
     * @param requestDto 확장 대비용 빈 요청 DTO
     * @return 마스킹된 이메일 주소와 인증 코드 유효 시간
     */
    public UserVerificationEmailSendResponseDto sendVerificationEmail(
            Long userId,
            UserVerificationEmailSendRequestDto requestDto
    ) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Users user = userRepository.findByIdAndIsDeletedAndDeletedAtIsNull(userId, UseYnEnum.N)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateEmailExists(user.getEmail());

        VerificationEmailAuthInfo authInfo = prepareVerificationCode(user.getId());
        try {
            userVerificationMailService.sendVerificationCode(
                    user.getEmail(),
                    authInfo.authCode(),
                    VERIFICATION_CODE_EXPIRE_SECONDS / 60
            );
        } catch (BusinessException e) {
            rollbackRedisKeys(authInfo.redisKey(), authInfo.limitKey(), user.getEmail());
            throw e;
        }

        return UserVerificationEmailSendResponseDto.of(maskEmail(user.getEmail()), VERIFICATION_CODE_EXPIRE_SECONDS);
    }

    private void validateEmailExists(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private VerificationEmailAuthInfo prepareVerificationCode(Long userId) {
        String limitKey = VERIFICATION_EMAIL_LIMIT_PREFIX + userId;
        String redisKey = VERIFICATION_EMAIL_PREFIX + userId;

        try {
            Boolean isSetSuccess = redisTemplate.opsForValue().setIfAbsent(
                    limitKey,
                    "blocked",
                    RESEND_COOLDOWN_SECONDS,
                    TimeUnit.SECONDS
            );

            if (Boolean.FALSE.equals(isSetSuccess)) {
                log.warn("마이페이지 사용자 인증 메일 과다 요청 - userId: {}", userId);
                throw new BusinessException(ErrorCode.TOO_MANY_EMAIL_REQUESTS);
            }

            String authCode = createVerificationCode();
            String hashedCode = passwordEncoder.encode(authCode);

            String cacheValue = createVerificationCacheValue(
                    hashedCode,
                    LocalDateTime.now().plusSeconds(VERIFICATION_CODE_EXPIRE_SECONDS)
            );

            redisTemplate.opsForValue().set(
                    redisKey,
                    cacheValue,
                    VERIFICATION_CODE_EXPIRE_SECONDS,
                    TimeUnit.SECONDS
            );

            return new VerificationEmailAuthInfo(authCode, redisKey, limitKey);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("사용자 인증 메일 Redis 저장 실패 - userId: {}, 이유: {}", userId, e.getMessage());
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_ERROR);
        }
    }

    private String createVerificationCode() {
        SecureRandom random = new SecureRandom();
        int bound = (int) Math.pow(10, VERIFICATION_CODE_LENGTH);
        int value = random.nextInt(bound);
        return String.format("%0" + VERIFICATION_CODE_LENGTH + "d", value);
    }

    private String createVerificationCacheValue(String hashedCode, LocalDateTime expiredAt) {
        return "{\"hashedCode\":\"" + hashedCode
                + "\",\"expiredAt\":\"" + expiredAt
                + "\",\"purpose\":\"" + UserVerificationPurpose.MYPAGE_USER_VERIFICATION.name()
                + "\"}";
    }

    private void rollbackRedisKeys(String redisKey, String limitKey, String email) {
        try {
            redisTemplate.delete(redisKey);
            redisTemplate.delete(limitKey);
        } catch (Exception e) {
            log.error("메일 발송 실패 후 Redis 롤백 중 오류 발생 - 대상: {}, 이유: {}", email, e.getMessage());
        }
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() == 1) {
            return "*@" + domain;
        }

        if (localPart.length() == 2) {
            return localPart.charAt(0) + "*@" + domain;
        }

        return localPart.charAt(0) + "****" + localPart.charAt(localPart.length() - 1) + "@" + domain;
    }

    private record VerificationEmailAuthInfo(
            String authCode,
            String redisKey,
            String limitKey
    ) {
    }
}
