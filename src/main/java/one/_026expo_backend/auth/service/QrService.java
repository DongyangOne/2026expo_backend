package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.auth.dto.response.QrTokenResponseDto;
import one._026expo_backend.auth.dto.response.QrLoginResponseDto;
import one._026expo_backend.global.dto.ApiResponse;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.Role;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.global.security.JwtTokenProvider;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class QrService {

    private static final String QR_SUCCESS_EVENT = "LOGIN_SUCCESS";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>(); // QR 토큰별 SSE 연결 객체를 저장하는 맵 (스레드에서 안전)
    private final JwtTokenProvider jwtProvider;
    private final UserRepository userRepository;

    private static final String QR_PREFIX = "qr:";
    private static final String QR_LOCK_PREFIX = "qr:lock:";
    private static final String QR_PENDING = "PENDING";
    private static final String REFRESH_TOKEN_PREFIX = "refreshToken:tablet:";

    /**
     * UUID로 QR 생성용 토큰을 생성하고 유효 시간과 함께 Redis에 저장한다.
     * 초기 저장 상태는 대기(PENDING) 상태이며, 3분 후 자동으로 만료된다.
     *
     * @return 생성된 QR 토큰
     */
    public QrTokenResponseDto createQrToken() {
        String qrToken = UUID.randomUUID().toString(); // UUID로 토큰 생성

        String redisKey = QR_PREFIX + qrToken;// Redis에 저장할 Key 포맷 설정

        try {
            // Key=토큰, Value="PENDING"으로 저장, 3분 뒤 삭제하도록 설정
            // 대기 상태(PENDING)로 저장, 앱에서 사용 시(로그인 완료 시) 완료(SUCCESS) 상태로 변경
            redisTemplate.opsForValue().set(redisKey, QR_PENDING, Duration.ofMinutes(3));
            log.info("Successfully generated QR token and saved to Redis: {}", qrToken);

        } catch (RedisConnectionFailureException e) { // 백엔드 서버 <-> Redis 서버 연결 실패
            log.error("Redis 서버가 꺼져있거나 네트워크 장애가 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_ERROR);
        }

        return QrTokenResponseDto.of(qrToken);
    }

    /**
     * 태블릿과 서버 간의 실시간 SSE 연결선을 만들고 메모리에 보관한다.
     *
     * @param qrToken 발급받은 고유 QR 토큰
     * @return 연결되어 만든 {@link SseEmitter} 객체
     */
    public SseEmitter createSseConnection(String qrToken) {
        String redisKey = QR_PREFIX + qrToken;
        Boolean hasKey;

        // Redis에 해당 토큰 키가 존재하는지 확인
        try {
            hasKey = redisTemplate.hasKey(redisKey);
        } catch (RedisConnectionFailureException e) { // 백엔드 서버 <-> Redis 서버 연결 실패
            log.error("Redis 서버가 꺼져있거나 네트워크 장애가 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.REDIS_CONNECTION_ERROR);
        }

        // 유효하지 않거나 만료된 토큰 검증
        if (Boolean.FALSE.equals(hasKey)) { // 잘못된 토큰 요청
            throw new BusinessException(ErrorCode.INVALID_QR_TOKEN);
        }

        // 검증을 통과한 정상적인 경우에만 Emitter 생성
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5분 동안 유지되는 연결선 생성

        emitters.put(qrToken, emitter); // 메모리 맵에 보관

        // 연결선 만료 및 에러 핸들러 세팅
        // 클라이언트 <-> 백엔드 서버 연결 끊김
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
            emitter.send(SseEmitter.event().name("INIT").data(ApiResponse.ok("Connected!"))); // 최종 응답이 아니므로 ApiResponse로만 감쌈
        } catch (IOException e) { // 클라이언트 <-> 백엔드 서버 연결선 수립 실패
            log.error("토큰에 대한 SSE 초기화 데이터 전송 실패: {}", qrToken);
            emitters.remove(qrToken);
            // 대답을 수신할 클라이언트가 없는 상태이므로 응답을 보내기 위한 것이 아닌 작업 중지용 에러 처리
            // 이미 5분 연결선을 만들었기 때문에 정리하기 위함
            throw new BusinessException(ErrorCode.SSE_CONNECTION_ERROR);
        }

        return emitter;
    }

    /**
     * 스마트폰 앱으로부터 QR 로그인 승인 요청을 받아 처리한다.
     *
     * @param qrToken 스마트폰이 QR에서 인식한 토큰
     * @param userId  모바일 앱이 지닌 유저고유 ID
     * @return 태블릿용 토큰이 포함된 로그인 정보
     */
    public QrLoginResponseDto approveQrLogin(String qrToken, Long userId) {
        // 로그인 사용자 ID 확인
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 실제 사용자 조회
        Users user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 탈퇴 계정 차단
        if (user.getIsDeleted() == UseYnEnum.Y) {
            throw new BusinessException(ErrorCode.DELETED_USER);
        }

        // QR 토큰 키 생성
        String redisKey = QR_PREFIX + qrToken;

        // QR 토큰 상태 확인
        String status = redisTemplate.opsForValue().get(redisKey);
        if (status == null || !status.equals(QR_PENDING)) { // Redis에서 대기 상태가 아니거나 상태가 비어있는 경우
            throw new BusinessException(ErrorCode.INVALID_QR_TOKEN);
        }

        // 동시에 같은 QR 토큰이 중복 승인되는 것을 막기 위한 짧은 락 (처리 중 표시)
        String lockKey = QR_LOCK_PREFIX + qrToken;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", Duration.ofSeconds(10));
        if (Boolean.FALSE.equals(locked)) { // 이미 다른 요청이 같은 QR 토큰을 처리 중
            throw new BusinessException(ErrorCode.QR_LOGIN_IN_PROGRESS);
        }

        try {
            // 태블릿용 토큰 발급
            String tabletAccessToken = jwtProvider.createAccessToken(userId, Role.USER);
            String tabletRefreshToken = jwtProvider.createRefreshToken(userId, Role.USER);

            // tabletRefreshToken 만료 시간 계산
            Duration refreshTokenTtl = calculateRefreshTokenTtl(tabletRefreshToken);

            // 태블릿 RefreshToken을 Redis에 저장
            String tabletRefreshKey = REFRESH_TOKEN_PREFIX + userId;
            redisTemplate.opsForValue().set(tabletRefreshKey, tabletRefreshToken, refreshTokenTtl);

            // SSE 전송용 응답 생성
            QrLoginResponseDto loginResponse = QrLoginResponseDto.from(user, tabletAccessToken, tabletRefreshToken);

            // 연결된 SSE 찾기
            SseEmitter tabletEmitter = emitters.get(qrToken);

            if (tabletEmitter != null) {
                try {
                    // 태블릿에 성공 이벤트 전송
                    tabletEmitter.send(SseEmitter.event()
                            .name(QR_SUCCESS_EVENT)
                            .data(ApiResponse.ok(loginResponse)));

                    // SSE 연결 종료
                    tabletEmitter.complete();

                    // 태블릿에 실제로 전달이 성공했을 때만 QR 토큰을 소비 처리
                    redisTemplate.delete(redisKey);
                } catch (IOException e) {
                    // 전송 실패 시 QR 토큰은 그대로 두어 같은 QR로 재시도할 수 있게 함
                    log.error("태블릿으로 로그인 데이터 전송 중 실패. qrToken: {}", qrToken, e);
                } finally {
                    // 메모리 연결 정리
                    emitters.remove(qrToken);
                }
            } else {
                // 태블릿에 전달할 방법이 없으므로 QR 토큰은 그대로 두어 재시도할 수 있게 함
                log.warn("토큰은 유효하나 연결된 태블릿의 SSE 이미터를 찾을 수 없음(이미 브라우저를 닫았거나 만료됨) qrToken: {}", qrToken);
            }

            return loginResponse;
        } finally {
            // 처리 완료 후 락 해제 (성공/실패 모두)
            redisTemplate.delete(lockKey);
        }
    }

    /**
     * RefreshToken의 유효 기간을 계산하는 메서드
     *
     * @param tabletRefreshToken 태블릿용 리프레시 토큰
     * @return 태블릿용 리프레시 토큰의 남은 유효 시간
     */
    private Duration calculateRefreshTokenTtl(String tabletRefreshToken) {
        Date refreshTokenExpiration = jwtProvider.getTokenExpirationTime(tabletRefreshToken);
        Duration defaultTtl = Duration.ofHours(1); // 기본 시간(1시간) 설정

        if (refreshTokenExpiration != null) { // 만료 날짜가 null이 아니라면
            long ttlMillis = refreshTokenExpiration.getTime() - System.currentTimeMillis(); // 남은 시간 구함
            if (ttlMillis > 0) {
                // refreshToken의 남은 시간 반환
                return Duration.ofMillis(ttlMillis);
            }
        }

        // RefreshToken 남은 시간을 못 찾았을 경우 기본 시간(1시간)으로 설정
        // Redis에 저장되더라도 올바르지 않은 토큰이므로 보안 위험 없음
        return defaultTtl;
    }
}