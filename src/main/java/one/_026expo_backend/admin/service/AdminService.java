package one._026expo_backend.admin.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.admin.domain.Admin;
import one._026expo_backend.admin.dto.request.AdminLoginRequestDto;
import one._026expo_backend.admin.dto.request.AdminSignupRequestDto;
import one._026expo_backend.admin.dto.response.AdminLoginResponseDto;
import one._026expo_backend.admin.dto.response.AdminSignupResponseDto;
import one._026expo_backend.admin.repository.AdminRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.Role;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.global.security.JwtTokenProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 관리자 회원가입
     */
    @Transactional
    public AdminSignupResponseDto adminSignup(AdminSignupRequestDto request) {
        if (adminRepository.existsByAdminId(request.getAdminId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER);
        }
        String hashed = passwordEncoder.encode(request.getAdminPassword());
        Admin admin = request.toEntity(hashed);
        Admin savedAdmin = adminRepository.save(admin);

        return AdminSignupResponseDto.builder()
                .adminId(savedAdmin.getAdminId())
                .team(savedAdmin.getTeam())
                .createdDate(savedAdmin.getCreatedAt())
                .build();
    }

    /**
     * 관리자 아이디 중복 체크
     */
    public UseYnEnum isExistsAdminId(String adminId) {
        if (adminId == null || adminId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_ID);
        }
        return adminRepository.existsByAdminId(adminId) ? UseYnEnum.Y : UseYnEnum.N;
    }

    /**
     * 관리자 로그인 기능
     * 매 로그인 시마다
     *
     * @param request 로그인 요청 정보를 담고 있는 dto
     * @return
     */
    @Transactional
    public AdminLoginResponseDto adminLogin(AdminLoginRequestDto request) {
        // 아이디 조회
        Admin admin = adminRepository.findByAdminId(request.getAdminLoginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        // 비밀번호 불일치
        if(!passwordEncoder.matches(request.getAdminPassword(), admin.getAdminPassword()))
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);

        String adminAccessToken = jwtTokenProvider.createAccessToken(admin.getId(), Role.ADMIN);
        String adminRefreshToken = jwtTokenProvider.createRefreshToken(admin.getId(), Role.ADMIN);

        // 리프레시 토큰 교체
        LocalDateTime expiryDate = LocalDateTime.now().plusDays(7);
        admin.updateRefreshToken(adminRefreshToken, expiryDate);

        return AdminLoginResponseDto.builder()
                .adminLoginId(admin.getAdminId())
                .team(admin.getTeam())
                .adminAccessToken(adminAccessToken)
                .adminRefreshToken(adminRefreshToken)
                .build();
    }

}
