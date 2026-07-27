package one._026expo_backend.user.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import one._026expo_backend.auth.dto.response.EmailSendResponseDto;
import one._026expo_backend.auth.enums.EmailVerificationPurpose;
import one._026expo_backend.auth.service.EmailService;
import one._026expo_backend.character.domain.Character;
import one._026expo_backend.character.domain.UserCharacter;
import one._026expo_backend.character.repository.UserCharacterRepository;
import one._026expo_backend.feedback.service.FeedbackService;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.service.QuizService;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.dto.request.UserEmailVerificationConfirmRequestDto;
import one._026expo_backend.user.dto.request.UserProfileUpdateRequestDto;
import one._026expo_backend.user.dto.response.UserDashboardResponseDto;
import one._026expo_backend.user.dto.response.UserEmailVerificationConfirmResponseDto;
import one._026expo_backend.user.dto.response.UserProfileResponseDto;
import one._026expo_backend.user.dto.response.UserProfileUpdateResponseDto;
import one._026expo_backend.user.dto.response.UserVerificationEmailSendResponseDto;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.CharacterInfo;
import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.QuizProfileInfo;
import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.RecyclingLogInfo;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final UserCharacterRepository userCharacterRepository;
    private final QuizService quizService;
    private final FeedbackService feedbackService;
    private final BCryptPasswordEncoder passwordEncoder;

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url-expiry-hours}")
    private int urlExpiryHours;

    @Value("${minio.profile-image-object}")
    private String profileImageObject;

    @Value("${spring.mail.auth.code-ttl-minutes}")
    private int authCodeValidMinutes;

    /**
     * 로그인한 사용자의 마이페이지 프로필을 조회한다.
     *
     * 사용자 식별자를 인증 정보에서만 받아 처리해 다른 사용자의 프로필을 임의 조회하는 흐름을 만들지 않는다.
     *
     * @param userId 인증된 사용자 식별자
     * @return 마이페이지 프로필 응답 DTO
     */
    @Transactional(readOnly = true)
    public UserProfileResponseDto findOneProfile(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Users user = userRepository.findByIdAndIsDeletedAndDeletedAtIsNull(userId, UseYnEnum.N)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return UserProfileResponseDto.from(user, getMinioImageUrl(profileImageObject));
    }

    @Transactional
    public UserEmailVerificationConfirmResponseDto confirmEmailVerification(
            Long userId,
            UserEmailVerificationConfirmRequestDto requestDto
    ) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Users user = userRepository.findByIdAndIsDeletedAndDeletedAtIsNull(userId, UseYnEnum.N)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String requestedEmail = requestDto.getEmail().trim();
        if (!requestedEmail.equals(user.getEmail())) {
            throw new BusinessException(ErrorCode.AUTH_CODE_MISMATCH);
        }

        emailService.verifyCode(
                user.getId(),
                requestedEmail,
                requestDto.getVerificationCode(),
                EmailVerificationPurpose.MYPAGE_USER_VERIFICATION
        );

        return UserEmailVerificationConfirmResponseDto.of(true, "이메일 인증이 완료되었습니다.");
    }

    /**
     * 로그인한 사용자의 마이페이지 프로필 정보를 수정한다.
     *
     * 마이페이지 사용자 인증을 완료한 사용자만 수정할 수 있게 제한해,
     * 로그인 상태만으로 민감 정보가 바뀌지 않도록 한다.
     *
     * @param userId 인증된 사용자 식별자
     * @param requestDto 프로필 수정 요청 DTO
     * @return 수정된 마이페이지 프로필 응답 DTO
     */
    @Transactional
    public UserProfileUpdateResponseDto updateProfile(Long userId, UserProfileUpdateRequestDto requestDto) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Users user = userRepository.findByIdAndIsDeletedAndDeletedAtIsNull(userId, UseYnEnum.N)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        emailService.validateVerificationConfirmed(userId, EmailVerificationPurpose.MYPAGE_USER_VERIFICATION);
        validateProfileUpdateRequest(requestDto);

        updateLoginIdIfPresent(user, requestDto.getLoginId());
        updatePasswordIfPresent(user, requestDto.getPassword(), requestDto.getPasswordConfirm());

        return UserProfileUpdateResponseDto.from(user, getMinioImageUrl(profileImageObject));
    }

    /**
     * 로그인한 사용자의 계정 이메일로 마이페이지 사용자 인증 코드를 발송한다.
     *
     * 계정 이메일은 서버에서만 조회해 사용해, 클라이언트가 임의의 이메일 주소로 인증 메일을 보내지 못하게 한다.
     *
     * @param userId 인증된 사용자 식별자
     * @return 마스킹된 이메일 주소와 인증 코드 만료 시간 응답 DTO
     */
    @Transactional
    public UserVerificationEmailSendResponseDto sendVerificationEmail(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Users user = userRepository.findByIdAndIsDeletedAndDeletedAtIsNull(userId, UseYnEnum.N)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateVerificationEmail(user.getEmail());

        EmailSendResponseDto response = emailService.sendVerificationEmail(
                user.getEmail(),
                EmailVerificationPurpose.MYPAGE_USER_VERIFICATION
        );

        return UserVerificationEmailSendResponseDto.of(
                maskEmail(response.getEmail()),
                authCodeValidMinutes * 60
        );
    }

    /**
     * 로그인한 사용자의 종합 정보(캐릭터, 퀴즈, 분리수거 로그)를 하나의 DTO로 반환
     *
     * @param userId 정보를 확인하고자 하는 사용자의 고유 아이디
     */
    public UserDashboardResponseDto getUserDashboard(Long userId) {
        // 사용자의 캐릭터 정보
        CharacterInfo character = getMyCharacter(userId);
        // 사용자의 퀴즈 정보
        QuizProfileInfo quizProfile = quizService.getQuizProfileInfo(userId);
        // 사용자의 분리수거 로그 정보
        List<RecyclingLogInfo> recentRecyclingLogs = feedbackService.getRecentRecyclingLogs(userId);

        return UserDashboardResponseDto.of(character, quizProfile, recentRecyclingLogs);
    }

    /**
     * 로그인한 사용자의 고유 식별 아이디를 받아 해당 사용자의 레벨 별 이미지와 관련 정보를 반환
     *
     * @param userId 로그인한 사용자의 고유 식별 아이디
     */
    public CharacterInfo getMyCharacter(Long userId) {
        UserCharacter userCharacter = userCharacterRepository.findByUserIdWithCharacter(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_CHARACTER_NOT_FOUND));

        // 내부 캐릭터 정보가 null인 경우
        Character currentCharacter = userCharacter.getCharacter();

        // MinIO에 저장된 이미지 주소
        String imageUrl = getMinioImageUrl(currentCharacter.getImageUrl());

        return CharacterInfo.of(
                currentCharacter.getCharacterId(),
                currentCharacter.getCharacterName(),
                imageUrl,
                currentCharacter.getEvolutionStage()
        );
    }

    /**
     * 테이블에 저장된 이미지 주소를 받아 MinIO Presigned URL을 반환
     *
     * @param imageUrl user_character 테이블에 저장된 이미지 주소
     * @return MinIO Presigned URL
     */
    private String getMinioImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(imageUrl)
                            .expiry(urlExpiryHours, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 이미지 주소 생성 실패 - 파일 경로: {}, 이유: {}", imageUrl, e.getMessage());
            throw new BusinessException(ErrorCode.IMAGE_URL_GENERATION_FAILED);
        }
    }

    private void validateVerificationEmail(String email) {
        if (email == null || email.isBlank() || !email.contains("@")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateProfileUpdateRequest(UserProfileUpdateRequestDto requestDto) {
        boolean hasLoginId = StringUtils.hasText(requestDto.getLoginId());
        boolean hasPassword = StringUtils.hasText(requestDto.getPassword());

        if (!hasLoginId && !hasPassword) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (requestDto.getLoginId() != null && !hasLoginId) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (requestDto.getPassword() != null && !hasPassword) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private void updateLoginIdIfPresent(Users user, String loginId) {
        if (!StringUtils.hasText(loginId)) {
            return;
        }

        String normalizedLoginId = loginId.trim();
        if (normalizedLoginId.equals(user.getLoginId())) {
            return;
        }

        if (userRepository.existsByLoginId(normalizedLoginId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER);
        }

        user.changeLoginId(normalizedLoginId);
    }

    private void updatePasswordIfPresent(Users user, String password, String passwordConfirm) {
        if (!StringUtils.hasText(password)) {
            return;
        }

        if (!StringUtils.hasText(passwordConfirm)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        if (!password.equals(passwordConfirm)) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.changePassword(passwordEncoder.encode(password));
    }

    private String maskEmail(String email) {
        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domain = parts[1];

        if (localPart.length() == 1) {
            return "*@" + domain;
        }

        if (localPart.length() == 2) {
            return localPart.charAt(0) + "*@" + domain;
        }

        return localPart.charAt(0) + "****" + localPart.charAt(localPart.length() - 1) + "@" + domain;
    }
}
