package one._026expo_backend.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 마이페이지 사용자 인증 메일 발송을 담당합니다.
 *
 * 메일 제목/본문 조합과 SMTP 예외 처리를 분리해 두면, 인증 코드 저장 로직과 메일 발송 실패 처리를 독립적으로 관리할 수 있습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserVerificationMailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 사용자 인증 번호 메일을 발송합니다.
     *
     * @param email 수신자 이메일
     * @param verificationCode 발송할 6자리 인증 번호
     * @param validMinutes 인증 번호 유효 시간(분)
     */
    public void sendVerificationCode(String email, String verificationCode, int validMinutes) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(email);
        message.setSubject("[2026 ONE] 사용자 인증번호 안내");
        message.setText(createContent(verificationCode, validMinutes));

        try {
            mailSender.send(message);
        } catch (Exception e) {
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

            log.error("사용자 인증 메일 발송 중 시스템 오류 - 대상: {}, 이유: {}", email, e.getMessage());
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String createContent(String verificationCode, int validMinutes) {
        return "마이페이지 사용자 인증을 위한 인증번호입니다.\n\n"
                + "인증번호: " + verificationCode + "\n\n"
                + "인증번호는 " + validMinutes + "분간 유효합니다.\n"
                + "본인이 요청하지 않았다면 이 메일을 무시해 주세요.";
    }
}
