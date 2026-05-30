package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.auth.dto.request.EmailCheckRequestDto;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerifyService {
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_PREFIX = "AUTH:EMAIL:";
    private static final String VERIFIED_PREFIX = "AUTH:VERIFIED:";

    @Value("${spring.mail.auth.verified-ttl-minutes}")
    private int verifiedValidMinutes;

    public void verifyAuthCode(EmailCheckRequestDto dto) {
        String authKey = REDIS_PREFIX + dto.getEmail();
        String storedCode = redisTemplate.opsForValue().get(authKey);

        if (storedCode == null) {
            log.warn("이메일 인증 실패 (코드 만료 혹은 이력 없음) - 대상: {}", dto.getEmail());
            throw new BusinessException(ErrorCode.AUTH_CODE_EXPIRED);
        }

        if (!storedCode.equals(dto.getAuthCode())) {
            log.warn("이메일 인증 실패 (코드 불일치) - 대상: {}, 입력 코드: {}", dto.getEmail(), dto.getAuthCode());
            throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
        }

         try {
            // 기존에 저장한 단순 이메일 정보는 삭제
            redisTemplate.delete(authKey);

            // 인증된 이메일이라는 정보만 저장
            String verifiedKey = VERIFIED_PREFIX + dto.getEmail();
            redisTemplate.opsForValue().set(
                    verifiedKey,
                    "인증 성공",
                    verifiedValidMinutes,
                    TimeUnit.MINUTES
            );

            log.info("이메일 인증 성공 - 대상: {}, 유효 시간: {}", dto.getEmail(),  verifiedValidMinutes);
        } catch (Exception e) {
             // Redis 통신 오류 등 인프라 에러가 서비스 로직을 삼키지 않도록 로그 추적
             log.error("이메일 인증 프로세스 완료 처리 중 Redis 오류 발생 - 대상: {}, 이유: {}", dto.getEmail(), e.getMessage());
             throw new BusinessException(ErrorCode.INTERNAL_ERROR);
         }

    }
}