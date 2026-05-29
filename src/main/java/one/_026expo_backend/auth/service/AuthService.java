package one._026expo_backend.auth.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.Role;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.auth.dto.SignupResponseDto;
import one._026expo_backend.auth.dto.SignupRequestDto;
import one._026expo_backend.auth.dto.LoginRequestDto;
import one._026expo_backend.auth.dto.LoginResponseDto;
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
        String refreshToken = jwtProvider.createRefreshToken(user.getId(), Role.USER);
        
        // 리프레시 토큰 만료 시간 계산 후 저장
        Date refreshTokenExpiration = jwtProvider.getTokenExpirationTime(refreshToken);
        LocalDateTime refreshExpiredAt = refreshTokenExpiration.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        user.updateRefreshToken(refreshToken, refreshExpiredAt);

        return LoginResponseDto.builder()
                .userId(user.getId())
                .loginId(user.getLoginId())
                .username(user.getUsername())
                .rememberMe(user.getRememberMe())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
