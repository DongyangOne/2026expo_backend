package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private static final String QR_PENDING = "PENDING";

    /**
     * UUID로 QR 생성용 토큰을 생성하고 유효 시간과 함께 Redis에 저장한다.
     * 초기 저장 상태는 대기(PENDING) 상태이며, 3분 후 자동으로 만료된다.
     */
    public String createQrToken() {
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

        return qrToken;
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
            return createAndSendErrorEmitter(qrToken, ErrorCode.REDIS_CONNECTION_ERROR); // 메소드 안에서 에러 응답 공통 규격 처리
        }

        // 유효하지 않거나 만료된 토큰 검증
        if (Boolean.FALSE.equals(hasKey)) { // 잘못된 토큰 요청
            return createAndSendErrorEmitter(qrToken, ErrorCode.INVALID_QR_TOKEN); // 메소드 안에서 에러 응답 공통 규격 처리
        }

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
            emitter.send(SseEmitter.event().name("INIT").data(ApiResponse.ok("Connected!")));
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
     * 에러시 발송할 공통 응답을 위한 임시 이미터 생성 및 전송 로직
     * * 연결이 유지되어 있을 시 매개변수로 받은 에러를 공통 응답으로 send
     * * 연결이 끊길 시 백엔드 서버 내에서 로그 처리
     *
     * 클라이언트에 정상적으로 에러 응답을 보내면 complete()메소드를 이용해 설정된 수명(0L)에 따라 연결선 정상 종료
     */
    private SseEmitter createAndSendErrorEmitter(String qrToken, ErrorCode errorCode) {
        // 공통 규격을 반환하기 위한 일회용 이미터 생성
        SseEmitter errorEmitter = new SseEmitter(0L); // 타임아웃 타이머는 try 블록 코드가 끝난 뒤 작동
        try {
            // 공통 규격으로 에러 반환
            ApiResponse<?> errorResponse = ApiResponse.error(errorCode);

            // 프론트가 쉽게 인지하도록 이벤트명("ERROR") 지정
            errorEmitter.send(SseEmitter.event().name("ERROR").data(errorResponse));
            errorEmitter.complete();
        } catch (IOException e) { // 클라이언트 <-> 백엔드 서버 연결 실패
            // 네트워크 연결이 끊긴 상태이므로 로그만 남김
            log.warn("연결이 끊어져 에러 메시지 전송 실패. 토큰: {}, 에러: {}", qrToken, errorCode.getMessage());
        }
        return errorEmitter;
    }

    /**
     * 스마트폰 앱으로부터 QR 로그인 승인 요청을 받아 처리한다.
     * @param qrToken 스마트폰이 QR에서 인식한 토큰
     * @param userId  모바일 앱이 지닌 유저고유 ID
     */
    public QrLoginResponseDto approveQrLogin(String qrToken, Long userId) {
        // 로그인 사용자 ID 확인
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // 실제 사용자 조회
        Users user = userRepository.findById(userId).orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));

        // 탈퇴 계정 차단
        if (user.getIsDeleted() == UseYnEnum.Y) {
            throw new BusinessException(ErrorCode.DELETED_USER);
        }

        // QR 토큰 키 생성
        String redisKey = QR_PREFIX + qrToken;

        // QR 토큰 상태 확인
        String status = redisTemplate.opsForValue().get(redisKey);
        if (status == null || !status.equals(QR_PENDING)) { // 대기 상태가 아니거나 상태가 비어있는 경우
            throw new BusinessException(ErrorCode.INVALID_QR_TOKEN);
        }

        // 태블릿용 토큰 발급
        String tabletAccessToken = jwtProvider.createAccessToken(userId, Role.USER);
        String tabletRefreshToken = jwtProvider.createRefreshToken(userId, Role.USER);

        // RefreshToken 만료 시간 계산
        Date refreshTokenExpiration = jwtProvider.getTokenExpirationTime(tabletRefreshToken);
        Duration refreshTokenTtl = Duration.ofMinutes(30);
        if (refreshTokenExpiration != null) {
            long ttlMillis = refreshTokenExpiration.getTime() - System.currentTimeMillis();
            if (ttlMillis > 0) {
                refreshTokenTtl = Duration.ofMillis(ttlMillis);
            }
        }

        // 태블릿 RefreshToken 저장
        String tabletRefreshKey = "refreshToken:tablet:" + userId;
        redisTemplate.opsForValue().set(tabletRefreshKey, tabletRefreshToken, refreshTokenTtl);

        // SSE 전송용 응답 생성
        QrLoginResponseDto tokenResponse = QrLoginResponseDto.builder()
                .accessToken(tabletAccessToken)
                .refreshToken(tabletRefreshToken)
                .build();

        // 연결된 SSE 찾기
        SseEmitter tabletEmitter = emitters.get(qrToken);

        if (tabletEmitter != null) {
            try {
                // 태블릿에 성공 이벤트 전송
                tabletEmitter.send(SseEmitter.event()
                        .name(QR_SUCCESS_EVENT)
                        .data(ApiResponse.ok(tokenResponse)));

                // SSE 연결 종료
                tabletEmitter.complete();
            } catch (IOException e) {
                log.error("태블릿으로 로그인 데이터 전송 중 실패. qrToken: {}", qrToken, e);
            } finally {
                // 메모리 연결 정리
                emitters.remove(qrToken);
            }
        } else {
            log.warn("토큰은 유효하나 연결된 태블릿의 SSE 이미터를 찾을 수 없음(이미 브라우저를 닫았거나 만료됨) qrToken: {}", qrToken);
        }

        // 사용한 QR 토큰 삭제
        redisTemplate.delete(redisKey);

        return tokenResponse;
    }
}