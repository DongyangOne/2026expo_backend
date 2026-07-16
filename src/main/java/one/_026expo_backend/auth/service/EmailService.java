package one._026expo_backend.auth.service;

import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.auth.dto.request.*;
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

/**
 * 회원가입 및 계정 찾기 시 인증 메일 발송과 Redis를 통한 상태 검증을 담당하는 서비스 클래스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    private static final String REDIS_PREFIX = "AUTH:EMAIL:";
    private static final String REDIS_LIMIT_PREFIX = "AUTH:EMAIL:LIMIT:";
    private static final String VERIFIED_PREFIX = "AUTH:VERIFIED:";
    private static final String USER_VERIFICATION_CONFIRMED_PREFIX = "AUTH:USER_VERIFICATION:CONFIRMED:";
    private static final String RESET_TOKEN_PREFIX = "AUTH:PASSWORD:RESET:";
    private static final long USER_VERIFICATION_CONFIRMED_TTL_MINUTES = 10L;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.auth.code-ttl-minutes}")
    private int authCodeValidMinutes;

    @Value("${spring.mail.auth.verified-ttl-minutes}")
    private int verifiedValidMinutes;

    /**
     * 회원가입용 인증 번호 이메일을 발송합니다.
     *
     * @param dto 회원가입을 시도하는 사용자의 이메일 정보가 담긴 요청 DTO
     * @return 발송된 이메일 주소와 인증 코드의 만료 시간이 담긴 응답 DTO
     * @throws BusinessException 이미 가입된 이메일이거나, 1분 이내에 재요청한 경우 발생
     */
    public EmailSendResponseDto sendVerificationEmail(EmailSendRequestDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        EmailAuthInfo authInfo = prepareAuthCode(dto.getEmail(), EmailVerificationPurpose.SIGN_UP);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(dto.getEmail());
        message.setSubject(EmailTemplate.SIGNUP.getSubject());
        message.setText(EmailTemplate.SIGNUP.createContent(authInfo.authCode));

        sendEmailAndHandleExceptions(message, dto.getEmail(), authInfo.redisKey, authInfo.limitKey);

        return EmailSendResponseDto.of(dto.getEmail(), LocalDateTime.now().plusMinutes(authCodeValidMinutes));
    }

    /**
     * 마이페이지 사용자 인증용 인증 번호 이메일을 발송합니다.
     *
     * purpose를 함께 저장해 기존 이메일 인증 흐름과 Redis 키가 섞이지 않도록 분리합니다.
     *
     * @param email 인증 번호를 수신할 이메일 주소
     * @param purpose 이메일 인증 코드 사용 목적
     * @return 발송된 이메일 주소와 인증 코드의 만료 시간이 담긴 응답 DTO
     */
    public EmailSendResponseDto sendVerificationEmail(String email, EmailVerificationPurpose purpose) {
        EmailAuthInfo authInfo = prepareAuthCode(email, purpose);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject(EmailTemplate.MYPAGE_USER_VERIFICATION.getSubject());
        message.setText(EmailTemplate.MYPAGE_USER_VERIFICATION.createContent(authInfo.authCode));

        sendEmailAndHandleExceptions(message, email, authInfo.redisKey, authInfo.limitKey);

        return EmailSendResponseDto.of(email, LocalDateTime.now().plusMinutes(authCodeValidMinutes));
    }

    /**
     * 회원가입 시, 사용자가 입력한 인증 번호를 검증
     *
     * @param dto 검증 대상 이메일과 사용자가 입력한 인증 번호가 담긴 요청 DTO
     * @return 인증 완료 메시지를 포함한 응답 DTO
     * @throws BusinessException 인증 코드가 만료되었거나 일치하지 않는 경우 발생
     */
    public EmailCheckResponseDto verifyAuthCode(EmailCheckRequestDto dto) {
        verifyCode(dto.getEmail(), dto.getAuthCode(), EmailVerificationPurpose.SIGN_UP);

        // 인증된 이메일이라는 정보만 저장
        String verifiedKey = VERIFIED_PREFIX + dto.getEmail();
        redisTemplate.opsForValue().set(
                verifiedKey,
                "인증 성공",
                verifiedValidMinutes,
                TimeUnit.MINUTES
        );

        log.info("이메일 인증 성공 - 대상: {}, 유효 시간: {}", dto.getEmail(),  verifiedValidMinutes);
        return EmailCheckResponseDto.of(true, "이메일 인증이 완료되었습니다.");
    }

    /**
     * 아이디 찾기용 인증 번호 이메일을 발송
     *
     * @param dto 아이디를 찾으려는 사용자의 이메일 정보가 담긴 요청 DTO
     * @return 발송된 이메일 주소와 인증 코드의 만료 시간이 담긴 응답 DTO
     * @throws BusinessException 가입되지 않은 이메일이거나, 1분 이내에 재요청한 경우 발생
     */
    public EmailSendResponseDto sendFindIdEmail(FindIdRequestDto dto) {
        // 가입하지 않은 이메일
        if (!userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        EmailAuthInfo authInfo = prepareAuthCode(dto.getEmail(), EmailVerificationPurpose.FIND_ID);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(dto.getEmail());
        message.setSubject(EmailTemplate.FIND_ID.getSubject());
        message.setText(EmailTemplate.FIND_ID.createContent(authInfo.authCode));

        sendEmailAndHandleExceptions(message, dto.getEmail(), authInfo.redisKey, authInfo.limitKey);

        return EmailSendResponseDto.of(dto.getEmail(), LocalDateTime.now().plusMinutes(authCodeValidMinutes));
    }

    /**
     * 아이디 찾기 단계에서 사용자가 입력한 인증 번호를 검증, 가입된 아이디 정보를 반환
     *
     * @param dto 검증 대상 이메일과 사용자가 입력한 인증 번호가 담긴 요청 DTO
     * @return 조회된 사용자의 식별 ID 및 로그인 ID 정보가 담긴 응답 DTO
     * @throws BusinessException 인증 코드가 만료되었거나 일치하지 않는 경우 발생
     */
    public FindIdResponseDto verifyFindIdAndGetId(FindIdCheckRequestDto dto) {
        verifyCode(dto.getEmail(), dto.getAuthCode(), EmailVerificationPurpose.FIND_ID);

        try {
            Users user = userRepository.findByEmail(dto.getEmail())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            log.info("아이디 찾기 검증 완료 - 대상: {}", dto.getEmail());

            return new FindIdResponseDto(user.getId(), user.getLoginId());
        } catch (BusinessException e) {
            throw e;
        }
    }

    /**
     * 비밀번호 찾기(재생성)용 인증 번호 이메일 발송
     * @param dto 비밀번호를 변경하려는 사용자의 로그인 ID와 이메일 정보가 담긴 요청 DTO
     * @return 발송된 이메일 주소와 인증 코드의 만료 시간이 담긴 응답 DTO
     * @throws BusinessException 아이디와 이메일 정보가 일치하는 유저가 없거나, 1분 이내에 재요청한 경우 발생
     */
    public EmailSendResponseDto sendFindPasswordEmail(FindPasswordRequestDto dto) {
        // 유저를 찾을 수 없음
        if (!userRepository.existsByLoginIdAndEmail(dto.getLoginId(), dto.getEmail()))
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);

        EmailAuthInfo authInfo = prepareAuthCode(dto.getEmail(), EmailVerificationPurpose.RESET_PASSWORD);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(dto.getEmail());
        message.setSubject(EmailTemplate.FIND_PASSWORD.getSubject());
        message.setText(EmailTemplate.FIND_PASSWORD.createContent(authInfo.authCode));

        sendEmailAndHandleExceptions(message, dto.getEmail(), authInfo.redisKey, authInfo.limitKey);

        return EmailSendResponseDto.of(dto.getEmail(), LocalDateTime.now().plusMinutes(authCodeValidMinutes));
    }

    /**
     * 비밀번호 찾기 시, 인증 번호를 검증하고 비밀번호 변경을 허가하는 임시 권한 토큰을 발급
     *
     * @param dto 사용자 식별 정보와 사용자가 입력한 인증 번호가 담긴 요청 DTO
     * @return 비밀번호 재설정 임시 권한 토큰과 사용자 식별 ID 정보가 담긴 응답 DTO
     * @throws BusinessException 인증 코드가 만료되었거나 일치하지 않는 경우 발생
     */
    public ResetTokenResponseDto verifyFindPasswordAndGetToken(FindPasswordCheckRequestDto dto) {
        verifyCode(dto.getEmail(), dto.getAuthCode(), EmailVerificationPurpose.RESET_PASSWORD);

        try {
            Users user = userRepository.findByLoginIdAndEmail(dto.getLoginId(), dto.getEmail())
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // 임시 권한 토큰 발급 및 저장 (UUID)
            String passwordResetToken = UUID.randomUUID().toString();
            String resetTokenKey = RESET_TOKEN_PREFIX + passwordResetToken;

            // Redis에 "AUTH:PASSWORD:RESET:{passwordResetToken}"형태로 저장
            redisTemplate.opsForValue().set(resetTokenKey, dto.getLoginId(), authCodeValidMinutes, TimeUnit.MINUTES);

            log.info("비밀번호 찾기 검증 성공 및 임시 토큰 발행 - 대상: {}", dto.getEmail());

            return new ResetTokenResponseDto(user.getId(), passwordResetToken);
        } catch (BusinessException e) {
            throw e;
        }
    }

    /**
     * 발급받은 임시 권한 토큰을 검증하여 사용자의 비밀번호 변경
     *
     * @param dto 이메일 검증 단계를 통해 발급받았던 임시 권한 토큰과 새로 변경할 비밀번호가 담긴 요청 DTO
     * @throws BusinessException 토큰 유효 시간이 초과되었거나 올바르지 않은 토큰인 경우 발생
     */
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

            // 사용한 토큰은 비밀번호 업데이트 후 삭제
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
                    "인증 성공",
                    USER_VERIFICATION_CONFIRMED_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        }
    }

    /**
     * 특정 목적의 사용자 인증 완료 여부를 검증한다.
     *
     * 마이페이지 민감 정보 수정처럼 추가 본인 확인이 필요한 흐름에서 Redis에 남겨둔 인증 완료 상태를 재사용한다.
     *
     * @param userId 인증 완료 여부를 확인할 사용자 식별자
     * @param purpose 인증 완료 상태의 사용 목적
     */
    public void validateVerificationConfirmed(Long userId, EmailVerificationPurpose purpose) {
        String confirmedKey = USER_VERIFICATION_CONFIRMED_PREFIX + userId + ":" + purpose.name();
        String verifiedStatus = redisTemplate.opsForValue().get(confirmedKey);

        if (verifiedStatus == null) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        try {
            redisTemplate.delete(confirmedKey);
        } catch (Exception e) {
            log.error("마이페이지 사용자 인증 완료 키 삭제 실패 - 사용자 ID: {}, 목적: {}, 이유: {}", userId, purpose.name(), e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String createAuthCode() {
        SecureRandom random = new SecureRandom();
        return String.valueOf(100000 + random.nextInt(900000));
    }

    /**
     * 중복된 메일 전송 및 SMTP 예외 분기 처리, 실패 시 Redis 키 롤백
     */
    private void sendEmailAndHandleExceptions(SimpleMailMessage message, String email, String redisKey, String limitKey) {
        try {
            mailSender.send(message);
        } catch (Exception e) {
            // 메일 발송 실패 시, 기존에 존재하던 redis 키 롤백
            try {
                redisTemplate.delete(redisKey);
                redisTemplate.delete(limitKey);
            } catch (Exception redisException) {
                log.error("메일 발송 실패 후 Redis 롤백 중 오류 발생 - 대상: {}, 이유: {}", email, redisException.getMessage());
            }
            if (e instanceof MailAuthenticationException) {
                log.error("SMTP 인증 실패 -  대상: {}, 이유: {}", email, e.getMessage());
                throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
            } else if (e instanceof MailParseException) {
                log.error("이메일 주소 파싱 실패 - 대상: {}, 이유: {}", email, e.getMessage());
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            } else if (e instanceof MailSendException) {
                log.error("SMTP 메일 전송 실패 - 대상: {}, 이유: {}", email, e.getMessage());
                throw new BusinessException(ErrorCode.EMAIL_SEND_FAILED);
            } else {
                log.error("이메일 발송 중 알 수 없는 시스템 오류 - 대상: {}, 이유: {}", email, e.getMessage());
                throw new BusinessException(ErrorCode.INTERNAL_ERROR);
            }
        }
    }

    /**
     * 이메일 발송 전 연속 요청(1분 이내) 차단 여부를 조회하고, 신규 인증 코드 Redis에 저장
     *
     * purpose를 함께 저장해 다른 용도의 인증 코드가 재사용되지 않도록 한다.
     */
    private EmailAuthInfo prepareAuthCode(String email, EmailVerificationPurpose purpose) {
        String limitKey = buildLimitKey(email, purpose);
        // Redis에 1분 제한 키 등록
        Boolean isSetSuccess = redisTemplate.opsForValue().setIfAbsent(limitKey, "blocked", 1,  TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(isSetSuccess)) {
            log.warn("이메일 발송 요청 과도 - 대상: {}", email);
            throw new BusinessException(ErrorCode.TOO_MANY_EMAIL_REQUESTS);
        }
        String authCode = createAuthCode();
        String redisKey = buildAuthCodeKey(email, purpose);

        // Redis에 정보 저장
        redisTemplate.opsForValue().set(redisKey, authCode, authCodeValidMinutes, TimeUnit.MINUTES);

        return new EmailAuthInfo(authCode, redisKey, limitKey);
    }

    /**
     * Redis에 저장된 인증코드를 검증한 후 검증이 완료되면 해당 코드 삭제
     *
     * @param email 검증 대상 사용자의 이메일 주소
     * @param authCode 사용자가 입력한 이메일 인증 코드
     * @param purpose 인증 코드 사용 목적
     * @throws BusinessException 인증코드 만료, 인증코드 불일치 시 발생
     */
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
            // 사용이 끝난 인증 코드 삭제
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

    // 이메일 발송 목적에 따른 제목과 본문을 관리하는 Enum
    private enum EmailTemplate {
        SIGNUP("[2026 ONE Expo] 회원가입 인증 번호 안내", "2026 ONE Expo 회원가입 인증 번호입니다.\n\n"),
        FIND_ID("[2026 ONE Expo] 아이디 찾기 인증 번호 안내", "2026 ONE Expo 아이디 찾기 인증 번호입니다.\n\n"),
        FIND_PASSWORD("[2026 ONE Expo] 비밀번호 찾기 인증 번호 안내", "2026 ONE Expo 비밀번호 찾기 인증 번호입니다.\n\n"),
        MYPAGE_USER_VERIFICATION("[2026 ONE Expo] 마이페이지 사용자 인증 번호 안내", "2026 ONE Expo 마이페이지 사용자 인증 번호입니다.\n\n");

        @Getter
        private final String subject;
        private final String content;

        EmailTemplate(String subject, String content) {
            this.subject = subject;
            this.content = content;
        }

        public String createContent(String authCode) {
            return content + "인증 번호는 [" + authCode + "] 입니다.\n" + "5분 이내에 입력하여 주세요.";
        }
    }

    private record EmailAuthInfo(
            String authCode,
            String redisKey,
            String limitKey
    ) {}
}
