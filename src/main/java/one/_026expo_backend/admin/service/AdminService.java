package one._026expo_backend.admin.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.admin.domain.Admin;
import one._026expo_backend.admin.dto.request.AdminSignupRequestDto;
import one._026expo_backend.admin.dto.response.AdminSignupResponseDto;
import one._026expo_backend.admin.repository.AdminRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {
    private final AdminRepository adminRepository;
    private final BCryptPasswordEncoder passwordEncoder;

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
}
