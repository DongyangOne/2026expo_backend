package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>(); // QR 토큰별 SSE 연결 객체를 저장하는 맵 (스레드에서 안전)

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

    /**
     * 태블릿과 서버 간의 실시간 SSE 연결선을 만들고 메모리에 보관한다.
     *
     * @param qrToken 발급받은 고유 QR 토큰
     * @return 연결되어 만든 {@link SseEmitter} 객체
     */
    public SseEmitter createSseConnection(String qrToken) {
        String redisKey = "qr:" + qrToken;

        Boolean hasKey = redisTemplate.hasKey(redisKey);// Redis에 해당 토큰 키가 존재하는지 확인

        if (Boolean.FALSE.equals(hasKey)) { // 유효하지 않거나 만료된 토큰
            // 에러 메시지만 즉시 전송하고 채널을 바로 닫기 위해 수명이 0인 임시 이미터 생성 (메모리 낭비 방지)
            // 유효하지 않은 토큰이지만, 현 API는 SSE 통로이기 때문에 반환타입을 SseEmitter로 주기 위해 객체를 생성함
            SseEmitter errorEmitter = new SseEmitter(0L);
            try {
                // 공통 규격으로 에러 반환
                ApiResponse<?> errorResponse = ApiResponse.error(ErrorCode.INVALID_QR_TOKEN);

                // 프론트가 쉽게 인지하도록 이벤트명("ERROR") 지정
                errorEmitter.send(SseEmitter.event().name("ERROR").data(errorResponse));
                errorEmitter.complete();
            } catch (IOException e) {
                log.error("에러 메시지 전송 실패", e);
            }
            return errorEmitter;
        }

        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5분 동안 유지되는 연결선 생성

        emitters.put(qrToken, emitter); // 메모리 맵에 보관

        // 연결선 만료 및 에러 핸들러 세팅
        emitter.onCompletion(() -> { // SSE 연결이 정상 종료된 경우
            emitters.remove(qrToken); // 서버 메모리에서 토큰 연결 삭제
        });

        emitter.onTimeout(() -> {
            log.info("토큰에 대한 SSE 연결 시간 만료: {}", qrToken);
            emitters.remove(qrToken);
            emitter.complete(); // 스프링에 연결이 끝났음을 알려줌
        });

        emitter.onError((e) -> {
            log.error("토큰에 대한 SSE 연결 중 에러 발생: {}, message: {}", qrToken, e.getMessage());
            emitters.remove(qrToken);
        });

        try {
            // SSE는 첫 연결 시 더미 데이터를 전송해야 연결이 유지됨
            emitter.send(SseEmitter.event().name("INIT").data(ApiResponse.ok("Connected!")));
        } catch (IOException e) {
            log.error("토큰에 대한 SSE 초기화 데이터 전송 실패: {}", qrToken);
            emitters.remove(qrToken);
            throw new BusinessException(ErrorCode.SSE_CONNECTION_ERROR);
        }

        return emitter;
    }
}