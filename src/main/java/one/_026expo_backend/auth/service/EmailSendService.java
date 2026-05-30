package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.auth.dto.request.EmailSendRequestDto;
import one._026expo_backend.auth.dto.response.EmailSendResponseDto;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendService {
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_PREFIX = "AUTH:EMAIL:";

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.auth.code-ttl-minutes}")
    private int authCodeValidMinutes;

    public EmailSendResponseDto sendVerificationEmail(EmailSendRequestDto dto) {
        String authCode = createAuthCode();

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
            log.error("이메일 발송 시스템 오류 - 대상: {}, 이유: {}", dto.getEmail(), e.getMessage());
            throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
        }

        /**
         * Redis에 정보 저장
         */
        String redisKey = REDIS_PREFIX + dto.getEmail();
        redisTemplate.opsForValue().set(
                redisKey,
                authCode,
                authCodeValidMinutes,
                TimeUnit.MINUTES
        );

        return EmailSendResponseDto.of(dto.getEmail(), LocalDateTime.now().plusMinutes(authCodeValidMinutes));
    }

    private String createAuthCode() {
        Random random = new Random();
        return String.valueOf(100000 + random.nextInt(900000));
    }
}