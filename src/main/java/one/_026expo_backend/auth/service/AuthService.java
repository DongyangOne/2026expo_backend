package one._026expo_backend.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.auth.dto.request.KakaoLoginRequestDto;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.Role;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.auth.dto.SignupResponseDto;
import one._026expo_backend.auth.dto.SignupRequestDto;
import one._026expo_backend.auth.dto.LoginRequestDto;
import one._026expo_backend.auth.dto.LoginResponseDto;
import one._026expo_backend.auth.dto.RefreshTokenRequestDto;
import one._026expo_backend.auth.dto.RefreshTokenResponseDto;
import one._026expo_backend.auth.service.KakaoOAuthClient.KakaoProfile;
import one._026expo_backend.global.security.JwtTokenProvider;
import one._026expo_backend.user.repository.UserRepository;
import one._026expo_backend.user.enums.SocialType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final KakaoOAuthClient kakaoOAuthClient;
    private final String REFRESH = "REFRESH"; // 토큰 타입 상수 설정
    private final String KAKAO_DEFAULT_USERNAME = "카카오회원"; // 카카오 회원 이름 상수 설정 

    /**
     * loginId의 중복 여부를 확인한다.
     *
     * @param loginId 중복 여부를 조회할 로그인 아이디
     * @return 중복(존재) 여부를 나타내는 UseYnEnum 값 (Y: 이미 존재함, N: 존재하지 않음)
     * @throws BusinessException loginId가 null이거나 공백인 경우 발생
     */
    public UseYnEnum isExistsLoginId(String loginId) {
        // 공통 응답으로 반환하기 위해 서비스 레이어에서 Blank 검증
        if (loginId == null || loginId.isBlank()) { 
            throw new BusinessException(ErrorCode.INVALID_LOGIN_ID);
        }
        return userRepository.existsByLoginId(loginId) ? UseYnEnum.Y : UseYnEnum.N;
    }

    /**
     * LOCAL 회원가입을 처리한다.
     *
     * @param request 회원가입 요청 정보
     * @return 저장된 사용자 정보를 담은 응답 DTO
     * @throws BusinessException 아이디 또는 이메일이 중복된 경우, 또는 약관 동의가 Y가 아닌 경우 발생
     */
    @Transactional
    public SignupResponseDto signup(SignupRequestDto request) {
        // 중복 아이디 체크
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER);
        }

        // 중복 이메일 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // agreeTerms가 N으로 들어왔을 때를 가정하여 예외 처리
        if (request.getAgreeTerms() != UseYnEnum.Y) {
            throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
        }

        String hashed = passwordEncoder.encode(request.getPassword());

        Users user = request.toEntity(hashed, request.getAgreeTerms());

        Users saved = userRepository.save(user);
        return SignupResponseDto.builder()
            .username(saved.getUsername())
            .loginId(saved.getLoginId())
            .email(saved.getEmail())
            .createdDate(saved.getCreatedAt())
            .build();
    }

    /**
     * LOCAL 로그인을 처리한다.
     * Refresh 토큰을 함께 저장한다.
     * 
     * @param requestDto
     * @return
     */
    @Transactional
    public LoginResponseDto login(LoginRequestDto requestDto) {
        if (!userRepository.existsByLoginId(requestDto.getLoginId())) {
            // 아예 존재하지 않는 로그인 아이디
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        Users user = userRepository.findByLoginIdAndIsDeleted(requestDto.getLoginId(), UseYnEnum.N) // 삭제되지 않은 계정을 아이디로 조회
                .orElseThrow(() -> new BusinessException(ErrorCode.DELETED_USER)); // 위에서 미존재 아이디를 잡았으므로 삭제 계정

        if (user.getSocialType() != SocialType.LOCAL) {
            // 아이디는 일치하지만 LOCAL 계정이 아닌 경우
            throw new BusinessException(ErrorCode.SOCIAL_LOGIN_REQUIRED);
        }

        if (user.getEmailVerified() != UseYnEnum.Y) {
            // 이메일 인증이 완료되지 않은 경우
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        if (user.getPassword() == null || !passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            // 아이디는 존재하지만 비밀번호가 일치하지 않는 경우
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        user.updateRememberMe(requestDto.getRememberMe());

        // 토큰 생성 (일반 유저 로그인이므로 Role.USER 직접 넣음)
        String accessToken = jwtProvider.createAccessToken(user.getId(), Role.USER);
        String refreshToken = createAndStoreRefreshToken(user, Role.USER);

        return LoginResponseDto.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .username(user.getUsername())
                .rememberMe(user.getRememberMe())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * KAKAO 로그인을 처리한다.
     * 카카오 계정 식별자로 기존 유저를 조회하고, 없으면 신규 회원으로 생성한다.
     */
    @Transactional
    public LoginResponseDto kakaoLogin(KakaoLoginRequestDto requestDto) {
        // 인가 코드로 카카오 사용자 정보를 가져옴
        KakaoProfile kakaoProfile = kakaoOAuthClient.fetchProfile(requestDto.getCode(), requestDto.getRedirectUri());

        Users user = userRepository.findBySocialTypeAndSocialProviderId(SocialType.KAKAO, kakaoProfile.providerId())
                .map(existingUser -> {
                    // 탈퇴한 계정이면 로그인하지 못함
                    if (existingUser.getIsDeleted() != UseYnEnum.N) {
                        throw new BusinessException(ErrorCode.DELETED_USER);
                    }
                    return existingUser;
                })
                // 처음 들어온 사용자면 새 계정으로 저장
                .orElseGet(() -> userRepository.save(Users.builder()
                        .username(resolveKakaoUsername(kakaoProfile.nickname()))
                        .loginId(null)
                        .password(null)
                        .email(kakaoProfile.email())
                        .emailVerified(UseYnEnum.Y) // 이메일 인증 Y
                        .rememberMe(requestDto.getRememberMe())
                        .termsAgreed(UseYnEnum.Y)   // 약관 동의 Y로 가정
                        .socialProviderId(kakaoProfile.providerId())    // 소셜 로그인 식별 번호
                        .socialType(SocialType.KAKAO)
                        .isDeleted(UseYnEnum.N)
                        .deletedAt(null)
                        .build()));

        // 로그인 유지 값 갱신
        user.updateRememberMe(requestDto.getRememberMe());

        // 카카오 로그인 성공 후 JWT 토큰 발급
        String accessToken = jwtProvider.createAccessToken(user.getId(), Role.USER);
        String refreshToken = createAndStoreRefreshToken(user, Role.USER);

        return LoginResponseDto.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .username(user.getUsername())
                .rememberMe(user.getRememberMe())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Refresh Token을 검증한 뒤 Access Token과 Refresh Token을 재발급한다.
     */
    @Transactional
    public RefreshTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        String refreshToken = request.getRefreshToken();

        if (refreshToken == null || refreshToken.isBlank()) {
            // 요청의 refreshToken이 비어있는 경우
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Claims claims;
        try {
            claims = jwtProvider.parseClaims(refreshToken);
        } catch (ExpiredJwtException e) { // 리프레시 토큰 만료
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException e) { // 서명 오류, 형식 오류, 손상된 토큰 등
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        String tokenType = claims.get("token_type", String.class);
        if (!REFRESH.equals(tokenType)) {  // 토큰 타입이 "REFRESH"가 아닌 경우
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId = Long.parseLong(claims.getSubject());

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN)); 
                // userId로 유저를 찾을 수 없는 경우지만, 토큰이 유효하지 않은 것으로 간주하여 INVALID_TOKEN 처리
                // 현재 흐름이 토큰 안의 userId와 DB 상태를 비교하는 것이기 때문

        if (user.getIsDeleted() != UseYnEnum.N) {
            // 탈퇴한 계정인 경우
            throw new BusinessException(ErrorCode.DELETED_USER);
        }

        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(refreshToken)) {
            // DB에 저장된 리프레시 토큰과 다른 경우
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (user.getRefreshExpiredAt() != null && user.getRefreshExpiredAt().isBefore(LocalDateTime.now())) {
            // DB 기준 만료 시각이 지난 경우
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        Role role = Role.valueOf(claims.get("role", String.class));
        String newAccessToken = jwtProvider.createAccessToken(user.getId(), role);
        String newRefreshToken = createAndStoreRefreshToken(user, role);

        return RefreshTokenResponseDto.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
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
        String resolvedNickname;

        if (nickname == null || nickname.isBlank()) {
            return KAKAO_DEFAULT_USERNAME; // 닉네임이 비어있으면 기본값 "카카오회원" 으로 저장
        } else {
            resolvedNickname = nickname.trim();
        }

        if (resolvedNickname.length() < 2) { // 이름이 두 글자 미만인 경우에도 "카카오회원" (username 규칙 통일)
            return KAKAO_DEFAULT_USERNAME;
        }

        String finalUsername;
        if (resolvedNickname.length() > 8) {
            finalUsername = resolvedNickname.substring(0, 8); // 이름이 8자 초과인 경우 8자까지만 잘라서 저장
        } else {
            finalUsername = resolvedNickname;
        }

        return finalUsername;
    }

        /**
         * Refresh Token을 생성하고 DB에 만료 시각과 함께 저장 후 발급된 Refresh Token을 반환한다.
         * AuthService에서만 사용한다.
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
