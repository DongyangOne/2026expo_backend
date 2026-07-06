package one._026expo_backend.user.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.domain.Withdraw;
import one._026expo_backend.user.dto.request.UserWithdrawRequestDto;
import one._026expo_backend.user.dto.response.UserWithdrawResponseDto;
import one._026expo_backend.user.enums.WithdrawReasonType;
import one._026expo_backend.user.repository.UserRepository;
import one._026expo_backend.user.repository.WithdrawRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserWithdrawalService {

    private final UserRepository userRepository;
    private final WithdrawRepository withdrawRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    /**
     * 로그인한 사용자의 비밀번호를 재확인한 뒤 탈퇴 사유를 남기고 계정을 soft delete 처리한다.
     *
     * Access Token은 stateless JWT 구조라 서버에서 즉시 회수하지 못하므로,
     * Refresh Token을 함께 제거해 재발급 경로를 차단하는 방식으로 세션 종료 효과를 맞춘다.
     *
     * @param userId 인증된 사용자 식별자
     * @param requestDto 회원탈퇴 요청 정보
     * @return 회원탈퇴 완료 메시지
     */
    @Transactional
    public UserWithdrawResponseDto softDelete(Long userId, UserWithdrawRequestDto requestDto) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        validateWithdrawableUser(user);
        validatePassword(requestDto.getPassword(), user.getPassword());

        String withdrawReasonDetail = normalizeWithdrawReasonDetail(
                requestDto.getWithdrawReason(),
                requestDto.getWithdrawReasonDetail()
        );

        withdrawRepository.save(Withdraw.create(
                user.getId(),
                requestDto.getWithdrawReason(),
                withdrawReasonDetail
        ));

        user.clearRefreshToken();
        user.softDelete();

        return UserWithdrawResponseDto.of("탈퇴가 완료되었습니다. 이용해 주셔서 감사합니다.");
    }

    private void validateWithdrawableUser(Users user) {
        if (user.getIsDeleted() == UseYnEnum.Y || user.getDeletedAt() != null) {
            throw new BusinessException(ErrorCode.DELETED_USER);
        }
    }

    private void validatePassword(String rawPassword, String encodedPassword) {
        if (!StringUtils.hasText(encodedPassword) || !passwordEncoder.matches(rawPassword, encodedPassword)) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }
    }

    private String normalizeWithdrawReasonDetail(WithdrawReasonType withdrawReason, String withdrawReasonDetail) {
        if (withdrawReason != WithdrawReasonType.ETC) {
            return null;
        }

        if (!StringUtils.hasText(withdrawReasonDetail)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return withdrawReasonDetail.trim();
    }
}
