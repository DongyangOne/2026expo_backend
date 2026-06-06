package one._026expo_backend.user.service;

import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.dto.response.UserProfileResponseDto;
import one._026expo_backend.user.enums.SocialType;
import one._026expo_backend.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("활성 사용자라면 마이페이지 프로필을 조회한다.")
    void findOneProfile() {
        Long userId = 1L;
        Users user = Users.builder()
                .id(userId)
                .username("김민혁")
                .loginId("kmh0707")
                .email("kmh0707@naver.com")
                .socialType(SocialType.LOCAL)
                .isDeleted(UseYnEnum.N)
                .build();

        when(userRepository.findByIdAndIsDeletedAndDeletedAtIsNull(userId, UseYnEnum.N))
                .thenReturn(Optional.of(user));

        UserProfileResponseDto response = userService.findOneProfile(userId);

        assertEquals(userId, response.getUserId());
        assertNull(response.getProfileImageUrl());
        assertEquals("김민혁", response.getName());
        assertEquals("kmh0707", response.getLoginId());
        assertEquals("kmh0707@naver.com", response.getEmail());
    }

    @Test
    @DisplayName("인증 사용자 식별자가 없으면 인증 예외를 반환한다.")
    void findOneProfileWithNullUserId() {
        BusinessException exception = assertThrows(BusinessException.class, () -> userService.findOneProfile(null));

        assertEquals(ErrorCode.UNAUTHORIZED, exception.getErrorCode());
    }

    @Test
    @DisplayName("삭제되었거나 존재하지 않는 사용자는 조회할 수 없다.")
    void findOneProfileWhenUserDoesNotExist() {
        Long userId = 1L;

        when(userRepository.findByIdAndIsDeletedAndDeletedAtIsNull(userId, UseYnEnum.N))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () -> userService.findOneProfile(userId));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
