package one._026expo_backend.user.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.dto.response.UserProfileResponseDto;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;

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

        return UserProfileResponseDto.from(user);
    }
}
