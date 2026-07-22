package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.admin.domain.Admin;
import one._026expo_backend.admin.repository.AdminRepository;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.domain.FeedbackDetail;
import one._026expo_backend.feedback.dto.response.AdminFeedbackResponseDto;
import one._026expo_backend.feedback.dto.response.FeedbackDetailResponseDto;
import one._026expo_backend.feedback.repository.FeedbackDetailRepository;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.global.pagination.PageRequestDto;
import one._026expo_backend.global.pagination.PageResponseDto;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackDetailService {
    private final FeedbackDetailRepository feedbackDetailRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    /**
     * 피드백 상세 조회 로직
     *
     * @param userId 특정 분리수거 기록을 확인하고자 하는 사용자 고유 아이디
     * @param feedbackId 조회하고자 하는 특정 피드백 id
     * @return 특정 피드백의 성공 여부, 날짜/시간, 쓰레기 종류 및 분리수거 상세 가이드(영상 URL, 설명 내용)
     */
    public FeedbackDetailResponseDto getFeedbackDetail(Long userId, Long feedbackId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        FeedbackDetail detail = feedbackDetailRepository.findByWasteTypeAndGuidanceCode(feedback.getWasteType(), feedback.getGuidanceCode()).orElse(null);

        return FeedbackDetailResponseDto.of(feedback, detail);
    }

    public PageResponseDto<AdminFeedbackResponseDto> getFeedbacks(Long adminId, PageRequestDto pageRequestDto) {
        // 별도의 Admin 테이블에서 로그인한 관리자 조회
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ADMIN_NOT_FOUND));

        // 요청한 페이지 번호와 페이지 크기로 페이징 설정
        Pageable pageable = PageRequest.of(pageRequestDto.getPage(), pageRequestDto.getPageSize());

        //관리자와 team 값이 같은 사용자들의피드백만 최신순으로 조회
        Page<Feedback> feedbackPage = feedbackRepository.findAllByUser_TeamOrderByCreatedAtDesc(admin.getTeam(), pageable);

        Page<AdminFeedbackResponseDto> responsePage = feedbackPage.map(AdminFeedbackResponseDto::from);

        return PageResponseDto.from(responsePage);
    }
}
