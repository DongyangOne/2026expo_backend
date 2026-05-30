package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.auth.dto.request.EmailSendRequestDto;
import one._026expo_backend.auth.dto.response.EmailSendResponseDto;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import one._026expo_backend.user.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendService {
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;

    private static final String REDIS_PREFIX = "AUTH:EMAIL:";
    private static final String REDIS_LIMIT_PREFIX = "AUTH:EMAIL:LIMIT:";

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.auth.code-ttl-minutes}")
    private int authCodeValidMinutes;

    public EmailSendResponseDto sendVerificationEmail(EmailSendRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        String limitKey = REDIS_LIMIT_PREFIX + dto.getEmail();
        Boolean emailAlreadyExists = redisTemplate.hasKey(limitKey);

        if (emailAlreadyExists) {
            log.warn("이메일 발송 요청 과도 - 대상: {}", dto.getEmail());
            throw new  BusinessException(ErrorCode.TOO_MANY_EMAIL_REQUESTS);
        }

        String authCode = createAuthCode();
        String redisKey = REDIS_PREFIX + dto.getEmail();

        // Redis에 1분 제한 키 등록
        redisTemplate.opsForValue().set(limitKey, "blocked", 1, TimeUnit.MINUTES);

        // Redis에 정보 저장
        redisTemplate.opsForValue().set(redisKey, authCode, authCodeValidMinutes, TimeUnit.MINUTES);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(dto.getEmail());
            message.setSubject("[2026 ONE Expo] 회원가입 인증 번호 안내");
            message.setText("2026 ONE Expo 회원가입 인증 번호입니다.\n\n" +
                    "인증 번호는 [" + authCode + "] 입니다.\n" +
                    "5분 이내에 입력하여 주세요.");
            mailSender.send(message);
        } catch (Exception e) {
            // 메일 발송 실패 시, 기존에 존재하던 redis 키 롤백
            try {
                redisTemplate.delete(redisKey);
                redisTemplate.delete(limitKey);
            } catch (Exception redisException) {
                log.error("메일 발송 실패 후 Redis 롤백 중 오류 발생 - 대상: {}, 이유: {}", dto.getEmail(), redisException.getMessage());
            }
            if (e instanceof MailAuthenticationException) {
                log.error("SMTP 인증 실패 -  대상: {}, 이유: {}", dto.getEmail(), e.getMessage());
                throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
            } else if (e instanceof MailParseException) {
                log.error("이메일 주소 파싱 실패 - 대상: {}, 이유: {}", dto.getEmail(), e.getMessage());
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            } else if (e instanceof MailSendException) {
                log.error("SMTP 메일 전송 실패 - 대상: {}, 이유: {}", dto.getEmail(), e.getMessage());
                throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
            } else {
                log.error("이메일 발송 중 알 수 없는 시스템 오류 - 대상: {}, 이유: {}", dto.getEmail(), e.getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        }
        return EmailSendResponseDto.of(dto.getEmail(), LocalDateTime.now().plusMinutes(authCodeValidMinutes));
    }

    private String createAuthCode() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(100000 + random.nextInt(900000));
    }
}