package one._026expo_backend.feedback.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.admin.domain.Admin;
import one._026expo_backend.admin.repository.AdminRepository;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.domain.FeedbackDetail;
import one._026expo_backend.feedback.dto.response.AdminFeedbackResponseDto;
import one._026expo_backend.feedback.dto.response.FeedbackDetailResponseDto;
import one._026expo_backend.feedback.enums.WasteType;
import one._026expo_backend.feedback.repository.FeedbackDetailRepository;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.global.pagination.PageRequestDto;
import one._026expo_backend.global.pagination.PageResponseDto;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackDetailService {
    private final FeedbackDetailRepository feedbackDetailRepository;
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AdminRepository adminRepository;

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.feedback-folder}")
    private String feedbackFolder;

    @Value("${minio.url-expiry-hours}")
    private int urlExpiryHours;

    /**
     * 로그인 사용자의 피드백 상세정보 조회
     */
    public FeedbackDetailResponseDto getFeedbackDetail(Long userId, Long feedbackId) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FEEDBACK_NOT_FOUND));

        if (!feedback.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        FeedbackDetail detail = null;

        if (StringUtils.hasText(feedback.getGuidanceCode())) {
            detail = feedbackDetailRepository
                    .findByWasteTypeAndGuidanceCode(feedback.getWasteType(), feedback.getGuidanceCode())
                    .orElse(null);
        }

        String feedbackVideoUrl = createFeedbackVideoUrl(feedback.getWasteType(), feedback.getGuidanceCode());

        return FeedbackDetailResponseDto.of(feedback, detail, feedbackVideoUrl);
    }

    /**
     * 쓰레기 종류와 안내 코드에 맞는 영상 Presigned URL 생성
     */
    public String createFeedbackVideoUrl(WasteType wasteType, String guidanceCode) {
        if (wasteType == null) {
            return null;
        }

        Optional<String> exactFeedbackDetailVideo = findExactFeedbackDetailVideoAddr(wasteType, guidanceCode);
        if (exactFeedbackDetailVideo.isPresent()) {
            return createVideoUrl(exactFeedbackDetailVideo.get());
        }

        String fileName = resolveVideoFileName(wasteType, guidanceCode);
        if (StringUtils.hasText(fileName)) {
            return createPresignedVideoUrl(feedbackFolder + "/" + fileName);
        }

        return findFeedbackDetailVideoAddr(wasteType, guidanceCode)
                .map(this::createVideoUrl)
                .orElse(null);
    }

    private Optional<String> findExactFeedbackDetailVideoAddr(WasteType wasteType, String guidanceCode) {
        if (!StringUtils.hasText(guidanceCode)) {
            return Optional.empty();
        }

        String normalizedGuidanceCode = guidanceCode.trim().toUpperCase();
        return extractVideoAddr(feedbackDetailRepository
                .findByWasteTypeAndGuidanceCode(wasteType, normalizedGuidanceCode));
    }

    private Optional<String> findFeedbackDetailVideoAddr(WasteType wasteType, String guidanceCode) {
        if (StringUtils.hasText(guidanceCode)) {
            String normalizedGuidanceCode = guidanceCode.trim().toUpperCase();

            Optional<FeedbackDetail> sameGuidanceDetail = feedbackDetailRepository
                    .findFirstByGuidanceCodeOrderByFeedbackDetailIdAsc(normalizedGuidanceCode);
            Optional<String> sameGuidanceVideoAddr = extractVideoAddr(sameGuidanceDetail);
            if (sameGuidanceVideoAddr.isPresent()) {
                return sameGuidanceVideoAddr;
            }
        }

        Optional<FeedbackDetail> defaultWasteDetail = feedbackDetailRepository
                .findFirstByWasteTypeAndGuidanceCodeIsNull(wasteType);
        Optional<String> defaultWasteVideoAddr = extractVideoAddr(defaultWasteDetail);
        if (defaultWasteVideoAddr.isPresent()) {
            return defaultWasteVideoAddr;
        }

        Optional<FeedbackDetail> anyWasteDetail = feedbackDetailRepository
                .findFirstByWasteTypeOrderByFeedbackDetailIdAsc(wasteType);
        Optional<String> anyWasteVideoAddr = extractVideoAddr(anyWasteDetail);
        if (anyWasteVideoAddr.isPresent()) {
            return anyWasteVideoAddr;
        }

        Optional<FeedbackDetail> defaultDetail = feedbackDetailRepository
                .findFirstByGuidanceCodeIsNullOrderByFeedbackDetailIdAsc();
        Optional<String> defaultVideoAddr = extractVideoAddr(defaultDetail);
        if (defaultVideoAddr.isPresent()) {
            return defaultVideoAddr;
        }

        Optional<FeedbackDetail> minioDetail = feedbackDetailRepository
                .findFirstByFeedbackVideoAddrStartingWithOrderByFeedbackDetailIdAsc(feedbackFolder + "/");
        Optional<String> minioVideoAddr = extractVideoAddr(minioDetail);
        if (minioVideoAddr.isPresent()) {
            return minioVideoAddr;
        }

        return extractVideoAddr(feedbackDetailRepository.findFirstByOrderByFeedbackDetailIdAsc());
    }

    private Optional<String> extractVideoAddr(Optional<FeedbackDetail> detail) {
        return detail
                .map(FeedbackDetail::getFeedbackVideoAddr)
                .filter(this::isUsableVideoAddr);
    }

    private boolean isUsableVideoAddr(String videoAddr) {
        return StringUtils.hasText(videoAddr)
                && !videoAddr.trim().startsWith("https://example-video.com")
                && !videoAddr.trim().startsWith("http://example-video.com");
    }

    private String createVideoUrl(String videoAddr) {
        String trimmedVideoAddr = videoAddr.trim();
        if (trimmedVideoAddr.startsWith("http://") || trimmedVideoAddr.startsWith("https://")) {
            return trimmedVideoAddr;
        }

        String objectName = trimmedVideoAddr.contains("/")
                ? trimmedVideoAddr
                : feedbackFolder + "/" + trimmedVideoAddr;

        return createPresignedVideoUrl(objectName);
    }

    private String createPresignedVideoUrl(String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(urlExpiryHours, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new IllegalStateException("피드백 영상 Presigned URL 생성에 실패했습니다.", e);
        }
    }

    /**
     * AI 안내 결과에 맞는 영상 파일명 결정
     */
    private String resolveVideoFileName(WasteType wasteType, String guidanceCode) {
        if (!StringUtils.hasText(guidanceCode)) {
            return null;
        }

        String key = wasteType.name() + ":" + guidanceCode.trim().toUpperCase();

        return switch (key) {
            // 캔 내용물/무게 이상
            case "CAN:EMPTY_CONTENTS" -> "feedback_can_waterOff.mp4";

            // 캔 미압착
            case "CAN:COMPRESS" -> "feedback_can_dent.mp4";

            // 종이 무게 이상
            case "PAPER:EMPTY_CONTENTS" -> "feedback_paper_weight.mp4";

            // 플라스틱·페트 내용물/무게 이상
            case "PLASTIC:EMPTY_CONTENTS" -> "feedback_plastic_waterOff.mp4";

            // 플라스틱·페트 라벨 미제거
            case "PLASTIC:REMOVE_LABEL" -> "feedback_plastic-vinylOff.mp4";

            case "PLASTIC:REMOVE_FOREIGN_MATERIAL" -> "feedback_plastic_foreign.mp4";

            // 페트 미압착
            case "PLASTIC:COMPRESS" -> "feedback_plastic_dent.mp4";

            case "VINYL:EMPTY_CONTENTS" -> "feedback_vinyl_weight.mp4";

            default -> null;
        };
    }

    private String removeTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }

        return value;
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
