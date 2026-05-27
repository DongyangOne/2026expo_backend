package one._026expo_backend.user.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.dto.UserSaveRequestDto;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 로그인 아이디 중복 여부를 확인합니다.
     */
    public boolean isExistsLoginId(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_ID);
        }
        return userRepository.existsByLoginId(loginId);
    }

    /**
     * 유저를 저장합니다.
     */
    @Transactional
    public Long save(UserSaveRequestDto request) {
        // 중복 아이디 체크
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USER);
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (request.getAgreeTerms() == null || request.getAgreeTerms().isBlank()) {
            throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
        }

        if (!"Y".equalsIgnoreCase(request.getAgreeTerms()) && !"N".equalsIgnoreCase(request.getAgreeTerms())) {
            throw new BusinessException(ErrorCode.INVALID_AGREE_TERMS);
        }

        if (!"Y".equalsIgnoreCase(request.getAgreeTerms())) {
            throw new BusinessException(ErrorCode.TERMS_NOT_AGREED);
        }

        String hashed = passwordEncoder.encode(request.getPassword());

        UseYnEnum terms = "Y".equalsIgnoreCase(request.getAgreeTerms()) ? UseYnEnum.Y : UseYnEnum.N;

        Users user = request.toEntity(hashed, terms);

        Users saved = userRepository.save(user);
        return saved.getId();
    }
}
