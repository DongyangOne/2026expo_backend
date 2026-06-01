package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.auth.dto.response.SocialLoginResponseDto;
import one._026expo_backend.auth.dto.request.KakaoLoginRequestDto;
import one._026expo_backend.auth.service.KakaoOAuthClient.KakaoProfile;
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

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtProvider;
    private final KakaoOAuthClient kakaoOAuthClient;

    /**
     * KAKAO 로그인을 처리한다.
     * 카카오 계정 식별자로 기존 유저를 조회하고, 없으면 신규 회원으로 생성한다.
     */
    @Transactional
    public SocialLoginResponseDto kakaoLogin(KakaoLoginRequestDto requestDto) {
        KakaoProfile kakaoProfile = kakaoOAuthClient.fetchProfile(requestDto.getCode(), requestDto.getRedirectUri());

        Users user = userRepository.findBySocialTypeAndSocialProviderId(SocialType.KAKAO, kakaoProfile.providerId())
                .map(existingUser -> {
                    if (existingUser.getIsDeleted() != UseYnEnum.N) {
                        throw new BusinessException(ErrorCode.DELETED_USER);
                    }
                    return existingUser;
                })
                .orElseGet(() -> userRepository.save(Users.builder()
                        .username(resolveKakaoUsername(kakaoProfile.nickname()))
                        .loginId(null)
                        .password(null)
                        .email(kakaoProfile.email())
                        .emailVerified(UseYnEnum.Y)
                        .rememberMe(requestDto.getRememberMe())
                        .termsAgreed(UseYnEnum.Y)
                        .socialProviderId(kakaoProfile.providerId())
                        .socialType(SocialType.KAKAO)
                        .isDeleted(UseYnEnum.N)
                        .deletedAt(null)
                        .build()));

        user.updateRememberMe(requestDto.getRememberMe());

        String accessToken = jwtProvider.createAccessToken(user.getId(), Role.USER);
        String refreshToken = createAndStoreRefreshToken(user, Role.USER);

        return SocialLoginResponseDto.builder()
                .userId(user.getId())
            .socialProviderId(user.getSocialProviderId())
            .socialType(user.getSocialType())
                .username(user.getUsername())
                .rememberMe(user.getRememberMe())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * 카카오에서 받아온 닉네임을 서버에 저장할 username으로 정리한다.
     * 카카오 로그인/회원가입 처리하는 kakaoLogin() 메소드에서 사용.
     *
     * @param nickname 카카오에서 받아온 닉네임
     * @return 정리된 username
     */
    private String resolveKakaoUsername(String nickname) {
        if (nickname == null || nickname.isBlank()) { 
            return KAKAO_DEFAULT_USERNAME;// 닉네임이 비어있으면 기본값 "카카오회원" 으로 저장
        }

        String resolvedNickname = nickname.trim();
        if (resolvedNickname.length() < 2) {
            return KAKAO_DEFAULT_USERNAME; // 이름이 두 글자 미만인 경우에도 "카카오회원" (username 규칙 통일)
        }

        if (resolvedNickname.length() > 8) {
            return resolvedNickname.substring(0, 8);// 이름이 8자 초과인 경우 8자까지만 잘라서 저장
        }

        return resolvedNickname;
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
