package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrService {

    private final StringRedisTemplate redisTemplate;

    /**
     * UUID로 QR 생성용 토큰을 생성하고 유효 시간과 함께 Redis에 저장한다.
     * 초기 저장 상태는 대기(PENDING) 상태이며, 3분 후 자동으로 만료된다.
     */
    public String createQrToken() {
        String qrToken = UUID.randomUUID().toString(); // UUID로 토큰 생성

        String redisKey = "qr:" + qrToken;// Redis에 저장할 Key 포맷 설정

        try {
            // Key=토큰, Value="PENDING"으로 저장, 3분 뒤 삭제하도록 설정
            // 대기 상태(PENDING)로 저장, 앱에서 사용 시(로그인 완료 시) 완료(SUCCESS) 상태로 변경
            redisTemplate.opsForValue().set(redisKey, "PENDING", Duration.ofMinutes(3));
            log.info("Successfully generated QR token and saved to Redis: {}", qrToken);

        } catch (RedisConnectionFailureException e) {
            log.error("Redis 서버가 꺼져있거나 네트워크 장애가 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_ERROR);
        }

        return qrToken;
    }
}