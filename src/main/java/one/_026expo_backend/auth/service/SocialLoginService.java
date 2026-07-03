package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.auth.dto.KakaoProfileDto;
import one._026expo_backend.auth.dto.request.GoogleLoginRequestDto;
import one._026expo_backend.auth.dto.request.NaverLoginRequestDto;
import one._026expo_backend.auth.dto.request.SocialLoginRequestDto;
import one._026expo_backend.auth.service.GoogleOAuthClient.GoogleProfile;
import one._026expo_backend.auth.dto.response.SocialLoginResponseDto;
import one._026expo_backend.auth.service.NaverOAuthClient.NaverProfile;
import one._026expo_backend.character.domain.Character;
import one._026expo_backend.character.domain.UserCharacter;
import one._026expo_backend.character.repository.CharacterRepository;
import one._026expo_backend.character.repository.UserCharacterRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.Role;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.global.security.JwtTokenProvider;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.enums.SocialType;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SocialLoginService {
    private static final String KAKAO_DEFAULT_USERNAME = "카카오회원";
    private static final String GOOGLE_DEFAULT_USERNAME = "구글회원";
    private static final String NAVER_DEFAULT_USERNAME = "네이버회원";
    private static final Long DEFAULT_CHARACTER_ID = 1L; // 회원가입 시 기본 지급되는 캐릭터 id

    private final UserRepository userRepository;
    private final CharacterRepository characterRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final JwtTokenProvider jwtProvider;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final GoogleOAuthClient googleOAuthClient;
    private final NaverOAuthClient naverOAuthClient;

    /**
     * KAKAO 로그인을 처리한다.
     * 카카오 계정 식별자로 기존 유저를 조회하고, 없으면 신규 회원으로 생성한다.
     *
     * @param requestDto 카카오 로그인 요청 데이터
     * @return 사용자 정보와 토큰을 포함한 로그인 응답
     */
    @Transactional
    public SocialLoginResponseDto kakaoLogin(SocialLoginRequestDto requestDto) {
        KakaoProfileDto kakaoProfile = kakaoOAuthClient.fetchProfile(requestDto.getCode(), requestDto.getRedirectUri());
        SocialProfile profile = new SocialProfile(kakaoProfile.getProviderId(), kakaoProfile.getNickname(), kakaoProfile.getEmail());

        return processSocialLogin(profile, SocialType.KAKAO, KAKAO_DEFAULT_USERNAME, requestDto.getRememberMe());
    }

    /**
     * GOOGLE 로그인을 처리한다.
     * 구글 계정 식별자로 기존 유저를 조회하고, 없으면 신규 회원으로 생성한다.
     */
    @Transactional
    public SocialLoginResponseDto googleLogin(GoogleLoginRequestDto requestDto) {
        GoogleProfile googleProfile = googleOAuthClient.fetchProfile(requestDto.getCode(), requestDto.getRedirectUri());
        SocialProfile profile = new SocialProfile(googleProfile.providerId(), googleProfile.name(), googleProfile.email());

        return processSocialLogin(profile, SocialType.GOOGLE, GOOGLE_DEFAULT_USERNAME, requestDto.getRememberMe());
    }

    /**
     * NAVER 로그인을 처리한다.
     */
    @Transactional
    public SocialLoginResponseDto naverLogin(NaverLoginRequestDto requestDto) {
        NaverProfile naverProfile = naverOAuthClient.fetchProfile(requestDto.getCode(), requestDto.getRedirectUri());
        SocialProfile profile = new SocialProfile(naverProfile.providerId(), naverProfile.name(), naverProfile.email());

        return processSocialLogin(profile, SocialType.NAVER, NAVER_DEFAULT_USERNAME, requestDto.getRememberMe());
    }

    /**
     * 소셜 서비스에서 받아온 이름을 서버에 저장할 username으로 정리한다.
     */
    private String resolveUsername(String name, String defaultUsername) {
        if (name == null || name.isBlank()) {
            return defaultUsername; // 닉네임이 비어있으면 기본값으로 저장
        }

        String resolvedName = name.trim();
        if (resolvedName.length() < 2) {
            return defaultUsername; // 이름이 두 글자 미만인 경우에도 기본값으로 저장
        }

        if (resolvedName.length() > 8) {
            return resolvedName.substring(0, 8); // 이름이 8자 초과인 경우 8자까지만 잘라서 저장
        }

        return resolvedName;
    }

    private static class SocialProfile {
        private final String providerId;
        private final String name;
        private final String email;

        SocialProfile(String providerId, String name, String email) {
            this.providerId = providerId;
            this.name = name;
            this.email = email;
        }

        String providerId() { return providerId; }
        String name() { return name; }
        String email() { return email; }
    }

    private SocialLoginResponseDto processSocialLogin(SocialProfile profile, SocialType socialType, String defaultUsername, UseYnEnum rememberMe) {
        if (profile.email() == null || profile.email().isBlank()) { // 이메일이 없는 경우 예외 처리
            if (socialType == SocialType.GOOGLE) throw new BusinessException(ErrorCode.GOOGLE_EMAIL_REQUIRED);
            if (socialType == SocialType.NAVER) throw new BusinessException(ErrorCode.NAVER_EMAIL_REQUIRED);
            if (socialType == SocialType.KAKAO) throw new BusinessException(ErrorCode.KAKAO_EMAIL_REQUIRED);
        }

        Users user = userRepository.findBySocialTypeAndSocialProviderId(socialType, profile.providerId())
                .map(existingUser -> {
                    if (existingUser.getIsDeleted() != UseYnEnum.N) { // 삭제된 유저인 경우
                        throw new BusinessException(ErrorCode.DELETED_USER);
                    }
                    return existingUser;
                })
                .orElseGet(() -> {
                    Users newUser = userRepository.save(Users.builder() // 신규 회원으로 저장
                            .username(resolveUsername(profile.name(), defaultUsername))
                            .loginId(null)
                            .password(null)
                            .email(profile.email())
                            .emailVerified(UseYnEnum.Y)
                            .rememberMe(rememberMe)
                            .termsAgreed(UseYnEnum.Y)
                            .socialProviderId(profile.providerId())
                            .socialType(socialType)
                            .isDeleted(UseYnEnum.N)
                            .deletedAt(null)
                            .build());

                    assignDefaultCharacter(newUser); // 신규 회원가입 시 기본 캐릭터 생성

                    return newUser;
                });

        user.updateRememberMe(rememberMe); // 이미 존재하는 경우로 로그인하는 경우 rememberMe 상태 업데이트

        String accessToken = jwtProvider.createAccessToken(user.getId(), Role.USER);
        String refreshToken = createAndStoreRefreshToken(user, Role.USER);

        return SocialLoginResponseDto.of(
            user.getId(),
            user.getSocialProviderId(),
            user.getSocialType(),
            user.getUsername(),
            user.getRememberMe(),
            accessToken,
            refreshToken
        );
    }

    /**
     * 회원가입 시 신규 유저에게 기본 캐릭터를 레벨 1, 경험치 0으로 생성한다.
     *
     * @param user 신규 유저 객체
     */
    private void assignDefaultCharacter(Users user) {
        Character defaultCharacter = characterRepository.findById(DEFAULT_CHARACTER_ID)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHARACTER_NOT_FOUND));
        userCharacterRepository.save(UserCharacter.create(user, defaultCharacter));
    }

    /**
     * Refresh Token을 생성하고 DB에 만료 시각과 함께 저장 후 발급된 Refresh Token을 반환한다.
     */
    private String createAndStoreRefreshToken(Users user, Role role) {
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), role);

        // 리프레시 토큰 만료 시간 계산 후 저장
        Date refreshTokenExpiration = jwtProvider.getTokenExpirationTime(refreshToken);
        LocalDateTime refreshExpiredAt = refreshTokenExpiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        user.updateRefreshToken(refreshToken, refreshExpiredAt);
        return refreshToken;
    }
}
