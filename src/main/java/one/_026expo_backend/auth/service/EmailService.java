package one._026expo_backend.auth.service;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.auth.dto.request.EmailCheckRequestDto;
import one._026expo_backend.auth.dto.request.EmailSendRequestDto;
import one._026expo_backend.auth.dto.request.FindIdCheckRequestDto;
import one._026expo_backend.auth.dto.request.FindIdRequestDto;
import one._026expo_backend.auth.dto.request.FindPasswordCheckRequestDto;
import one._026expo_backend.auth.dto.request.FindPasswordRequestDto;
import one._026expo_backend.auth.dto.request.PasswordResetRequestDto;
import one._026expo_backend.auth.dto.response.EmailCheckResponseDto;
import one._026expo_backend.auth.dto.response.EmailSendResponseDto;
import one._026expo_backend.auth.dto.response.FindIdResponseDto;
import one._026expo_backend.auth.dto.response.ResetTokenResponseDto;
import one._026expo_backend.auth.enums.EmailVerificationPurpose;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private static final String REDIS_PREFIX = "AUTH:EMAIL:";
    private static final String REDIS_LIMIT_PREFIX = "AUTH:EMAIL:LIMIT:";
    private static final String VERIFIED_PREFIX = "AUTH:VERIFIED:";
    private static final String USER_VERIFICATION_CONFIRMED_PREFIX = "AUTH:USER_VERIFICATION:CONFIRMED:";
    private static final String RESET_TOKEN_PREFIX = "AUTH:PASSWORD:RESET:";
    private static final long USER_VERIFICATION_CONFIRMED_TTL_MINUTES = 10L;
    private static final String VERIFIED_SUCCESS = "인증 성공";

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.auth.code-ttl-minutes}")
    private int authCodeValidMinutes;

    @Value("${spring.mail.auth.verified-ttl-minutes}")
    private int verifiedValidMinutes;

    public EmailSendResponseDto sendVerificationEmail(EmailSendRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        EmailAuthInfo authInfo = prepareAuthCode(dto.getEmail(), EmailVerificationPurpose.SIGN_UP);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(dto.getEmail());
        message.setSubject(EmailTemplate.SIGNUP.getSubject());
        message.setText(EmailTemplate.SIGNUP.createContent(authInfo.authCode()));

        sendEmailAndHandleExceptions(message, dto.getEmail(), authInfo.redisKey(), authInfo.limitKey());
        return EmailSendResponseDto.of(dto.getEmail(), LocalDateTime.now().plusMinutes(authCodeValidMinutes));
    }

    public EmailCheckResponseDto verifyAuthCode(EmailCheckRequestDto dto) {
        verifyCode(dto.getEmail(), dto.getAuthCode(), EmailVerificationPurpose.SIGN_UP);

        String verifiedKey = VERIFIED_PREFIX + dto.getEmail();
        redisTemplate.opsForValue().set(
                verifiedKey,
                VERIFIED_SUCCESS,
                verifiedValidMinutes,
                TimeUnit.MINUTES
        );

        log.info("이메일 인증 성공 - 대상: {}, 유효 시간: {}", dto.getEmail(), verifiedValidMinutes);
        return EmailCheckResponseDto.of(true, "이메일 인증이 완료되었습니다.");
    }

    public EmailSendResponseDto sendFindIdEmail(FindIdRequestDto dto) {
        if (!userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        EmailAuthInfo authInfo = prepareAuthCode(dto.getEmail(), EmailVerificationPurpose.FIND_ID);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(dto.getEmail());
        message.setSubject(EmailTemplate.FIND_ID.getSubject());
        message.setText(EmailTemplate.FIND_ID.createContent(authInfo.authCode()));

        sendEmailAndHandleExceptions(message, dto.getEmail(), authInfo.redisKey(), authInfo.limitKey());
        return EmailSendResponseDto.of(dto.getEmail(), LocalDateTime.now().plusMinutes(authCodeValidMinutes));
    }

    public FindIdResponseDto verifyFindIdAndGetId(FindIdCheckRequestDto dto) {
        verifyCode(dto.getEmail(), dto.getAuthCode(), EmailVerificationPurpose.FIND_ID);

        Users user = userRepository.findByEmail(dto.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        log.info("아이디 찾기 검증 완료 - 대상: {}", dto.getEmail());
        return new FindIdResponseDto(user.getId(), user.getLoginId());
    }

    public EmailSendResponseDto sendFindPasswordEmail(FindPasswordRequestDto dto) {
        if (!userRepository.existsByLoginIdAndEmail(dto.getLoginId(), dto.getEmail())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        EmailAuthInfo authInfo = prepareAuthCode(dto.getEmail(), EmailVerificationPurpose.RESET_PASSWORD);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(dto.getEmail());
        message.setSubject(EmailTemplate.FIND_PASSWORD.getSubject());
        message.setText(EmailTemplate.FIND_PASSWORD.createContent(authInfo.authCode()));

        sendEmailAndHandleExceptions(message, dto.getEmail(), authInfo.redisKey(), authInfo.limitKey());
        return EmailSendResponseDto.of(dto.getEmail(), LocalDateTime.now().plusMinutes(authCodeValidMinutes));
    }

    public ResetTokenResponseDto verifyFindPasswordAndGetToken(FindPasswordCheckRequestDto dto) {
        verifyCode(dto.getEmail(), dto.getAuthCode(), EmailVerificationPurpose.RESET_PASSWORD);

        Users user = userRepository.findByLoginIdAndEmail(dto.getLoginId(), dto.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String passwordResetToken = UUID.randomUUID().toString();
        String resetTokenKey = RESET_TOKEN_PREFIX + passwordResetToken;
        redisTemplate.opsForValue().set(resetTokenKey, dto.getLoginId(), authCodeValidMinutes, TimeUnit.MINUTES);

        log.info("비밀번호 찾기 검증 성공 및 임시 토큰 발행 - 대상: {}", dto.getEmail());
        return new ResetTokenResponseDto(user.getId(), passwordResetToken);
    }

    @Transactional
    public void updatePassword(PasswordResetRequestDto dto) {
        String resetTokenKey = RESET_TOKEN_PREFIX + dto.getPasswordResetToken();
        String loginId = redisTemplate.opsForValue().get(resetTokenKey);

        if (loginId == null) {
            log.warn("시간이 초과(5분)되었거나 유효하지 않은 검증 토큰");
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        try {
            Users user = userRepository.findByLoginId(loginId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            String newPassword = passwordEncoder.encode(dto.getNewPassword());
            user.changePassword(newPassword);
            redisTemplate.delete(resetTokenKey);

            log.info("비밀번호 변경 완료 - 대상: {}", loginId);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("비밀번호 변경 처리 중 오류 발생 - 대상: {}, 이유: {}", loginId, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    public void verifyCode(String email, String authCode, EmailVerificationPurpose purpose) {
        validateAndDeleteAuthCode(email, authCode, purpose);
    }

    public void verifyCode(Long userId, String email, String authCode, EmailVerificationPurpose purpose) {
        verifyCode(email, authCode, purpose);

        if (purpose == EmailVerificationPurpose.MYPAGE_USER_VERIFICATION) {
            String confirmedKey = USER_VERIFICATION_CONFIRMED_PREFIX + userId + ":" + purpose.name();
            redisTemplate.opsForValue().set(
                    confirmedKey,
                    VERIFIED_SUCCESS,
                    USER_VERIFICATION_CONFIRMED_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        }
    }

    private String createAuthCode() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    private void sendEmailAndHandleExceptions(SimpleMailMessage message, String email, String redisKey, String limitKey) {
        try {
            mailSender.send(message);
        } catch (Exception e) {
            try {
                redisTemplate.delete(redisKey);
                redisTemplate.delete(limitKey);
            } catch (Exception redisException) {
                log.error("메일 발송 실패 후 Redis 롤백 중 오류 발생 - 대상: {}, 이유: {}", email, redisException.getMessage());
            }

            if (e instanceof MailAuthenticationException) {
                log.error("SMTP 인증 실패 - 대상: {}, 이유: {}", email, e.getMessage());
                throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
            }
            if (e instanceof MailParseException) {
                log.error("이메일 주소 파싱 실패 - 대상: {}, 이유: {}", email, e.getMessage());
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            if (e instanceof MailSendException) {
                log.error("SMTP 메일 전송 실패 - 대상: {}, 이유: {}", email, e.getMessage());
                throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
            }

            log.error("이메일 발송 중 예상하지 못한 시스템 오류 - 대상: {}, 이유: {}", email, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private EmailAuthInfo prepareAuthCode(String email, EmailVerificationPurpose purpose) {
        String limitKey = buildLimitKey(email, purpose);
        Boolean isSetSuccess = redisTemplate.opsForValue().setIfAbsent(limitKey, "blocked", 1, TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(isSetSuccess)) {
            log.warn("이메일 발송 요청 과도 - 대상: {}", email);
            throw new BusinessException(ErrorCode.TOO_MANY_EMAIL_REQUESTS);
        }

        String authCode = createAuthCode();
        String redisKey = buildAuthCodeKey(email, purpose);
        redisTemplate.opsForValue().set(redisKey, authCode, authCodeValidMinutes, TimeUnit.MINUTES);

        return new EmailAuthInfo(authCode, redisKey, limitKey);
    }

    private void validateAndDeleteAuthCode(String email, String authCode, EmailVerificationPurpose purpose) {
        String authKey = buildAuthCodeKey(email, purpose);
        String storedCode = redisTemplate.opsForValue().get(authKey);

        if (storedCode == null) {
            log.warn("이메일 인증 실패 (코드 만료 혹은 이력 없음) - 대상: {}, 목적: {}", email, purpose.name());
            throw new BusinessException(ErrorCode.AUTH_CODE_EXPIRED);
        }

        if (!storedCode.equals(authCode)) {
            log.warn("이메일 인증 실패 (코드 불일치) - 대상: {}, 목적: {}", email, purpose.name());
            throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
        }

        try {
            redisTemplate.delete(authKey);
        } catch (Exception e) {
            log.error("인증 코드 삭제 중 Redis 오류 발생 - 대상: {}, 목적: {}, 이유: {}", email, purpose.name(), e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String buildAuthCodeKey(String email, EmailVerificationPurpose purpose) {
        return REDIS_PREFIX + purpose.name() + ":" + email;
    }

    private String buildLimitKey(String email, EmailVerificationPurpose purpose) {
        return REDIS_LIMIT_PREFIX + purpose.name() + ":" + email;
    }

    private enum EmailTemplate {
        SIGNUP("[2026 ONE Expo] 회원가입 인증 번호 안내", "2026 ONE Expo 회원가입 인증 번호입니다.\n\n"),
        FIND_ID("[2026 ONE Expo] 아이디 찾기 인증 번호 안내", "2026 ONE Expo 아이디 찾기 인증 번호입니다.\n\n"),
        FIND_PASSWORD("[2026 ONE Expo] 비밀번호 찾기 인증 번호 안내", "2026 ONE Expo 비밀번호 찾기 인증 번호입니다.\n\n");

        @Getter
        private final String subject;

        private final String content;

        EmailTemplate(String subject, String content) {
            this.subject = subject;
            this.content = content;
        }

        public String createContent(String authCode) {
            return content + "인증 번호는 [" + authCode + "] 입니다.\n5분 이내에 입력해 주세요.";
        }
    }

    private record EmailAuthInfo(
            String authCode,
            String redisKey,
            String limitKey
    ) {
    }
}
