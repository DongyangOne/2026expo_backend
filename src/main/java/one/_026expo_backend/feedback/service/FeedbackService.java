package one._026expo_backend.feedback.service;

import lombok.RequiredArgsConstructor;
import one._026expo_backend.feedback.domain.AiDetection;
import one._026expo_backend.feedback.domain.Feedback;
import one._026expo_backend.feedback.dto.request.AiFeedbackRequestDto;
import one._026expo_backend.feedback.dto.response.FeedbackListResponseDto;
import one._026expo_backend.feedback.enums.DetectionStatus;
import one._026expo_backend.feedback.enums.WasteType;
import one._026expo_backend.feedback.repository.AiDetectionRepository;
import one._026expo_backend.feedback.repository.FeedbackRepository;
import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.RecyclingLogInfo;

import one._026expo_backend.global.enums.UseYnEnum;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.global.pagination.PageRequestDto;
import one._026expo_backend.global.pagination.PageResponseDto;
import one._026expo_backend.user.domain.Users;
import one._026expo_backend.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {
    private final FeedbackRepository feedbackRepository;
    private final UserRepository userRepository;
    private final AiDetectionRepository aiDetectionRepository;

    /**
     * 로그인한 사용자의 최근 분리수거 기록(~10개)을 반환
     *
     * @param userId 분리수거 기록을 확인하고자 하는 사용자 고유 아이디
     * @return 분리수거 기록 (최근 10개)
     */
    public List<RecyclingLogInfo> getRecentRecyclingLogs(long userId) {
        // 최근 10개까지만 조회
        Pageable pageable = PageRequest.of(0, 10);

        return feedbackRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .stream()
                .map(feedback -> RecyclingLogInfo.of(
                        feedback.getWasteType(),
                        feedback.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    public PageResponseDto<FeedbackListResponseDto> getFeedbackList(Long userId, PageRequestDto pageRequestDto) {
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Pageable pageable = PageRequest.of(pageRequestDto.getPage(), pageRequestDto.getPageSize());

        Page<Feedback> feedbackPage = feedbackRepository.findAllByUserOrderByCreatedAtDesc(user, pageable);

        Page<FeedbackListResponseDto> dtoPage = feedbackPage.map(FeedbackListResponseDto::from);

        return PageResponseDto.from(dtoPage);
    }
    /**
     * AI 서버에서 전달받은 쓰레기 판정 결과를 피드백으로 저장
     *
     * @param dto AI 서버가 전달한 판정 결과
     */
    @Transactional
    public void saveAiFeedback(AiFeedbackRequestDto dto) {

        // clientId를 이용해 검사 시작 기록 조회
        AiDetection detection = aiDetectionRepository
                .findByClientId(dto.getClientId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DETECTION_NOT_FOUND));

        /*
         * 쓰레기를 감지하지 못한 경우에는 WasteType을 결정할 수 없으므로
         * Feedback을 생성하지 않고 검사만 완료 처리
         */
        if (dto.getStatus() == DetectionStatus.NOT_DETECTED || dto.getClassification() == null || !StringUtils.hasText(dto.getClassification().getClassName())) {
            detection.complete();
            return;
        }

        // 검사 시작 당시 clientId와 연결한 로그인 사용자 조회
        Users user = detection.getUser();

        // AI가 반환한 class_name을 백엔드 WasteType으로 변환
        WasteType wasteType = convertWasteType(dto.getClassification().getClassName());

        // ALLOWED는 성공(N), 나머지 결과는 실패(Y)로 저장
        UseYnEnum isFailed = dto.getStatus() == DetectionStatus.ALLOWED ? UseYnEnum.N : UseYnEnum.Y;

        // AI 안내 메시지를 사용자에게 표시할 피드백 문구로 변환
        String feedbackText = createFeedbackText(dto);

        /*
         * 상세조회에서 대표 영상 하나를 선택할 때 사용할 안내 코드
         *
         * guidance가 여러 개여도 첫 번째 유효한 코드 하나만 저장
         * guidance가 없다면 null 저장
         */
        String guidanceCode = dto.getGuidance() == null ? null
                : dto.getGuidance().stream()
                .map(AiFeedbackRequestDto.GuidanceDto::getCode)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);

        // AI 판정 결과로 사용자 피드백 생성
        Feedback feedback = Feedback.builder()
                .user(user)
                .wasteType(wasteType)
                .isFailed(isFailed)
                .feedbackText(feedbackText)
                .guidanceCode(guidanceCode)
                .build();

        // 사용자 피드백 DB 저장
        feedbackRepository.save(feedback);

        // 검사 요청을 완료 상태로 변경
        detection.complete();
    }

    /**
     * AI가 전달한 쓰레기 분류명을 백엔드 WasteType으로 변환
     *
     * @param className AI가 판별한 쓰레기 분류명
     * @return 백엔드에서 사용하는 쓰레기 종류
     */
    private WasteType convertWasteType(String className) {

        // 쓰레기를 감지하지 못했거나 분류명이 없는 경우
        if (!StringUtils.hasText(className)) {
            throw new BusinessException(ErrorCode.WASTE_TYPE_NOT_FOUND);
        }

        return switch (className.toLowerCase()) {
            case "plastic", "pet" -> WasteType.PLASTIC;
            case "can" -> WasteType.CAN;
            case "paper" -> WasteType.PAPER;
            case "vinyl" -> WasteType.VINYL;
            case "glass" -> WasteType.GLASS;
            case "battery" -> WasteType.BATTERY;
            case "fluorescent" -> WasteType.FLUORESCENT;
            case "styrofoam" -> WasteType.STYROFOAM;
            default -> throw new BusinessException(ErrorCode.WASTE_TYPE_NOT_FOUND);
        };
    }

    /**
     * AI 판정 결과를 사용자에게 표시할 피드백 문구로 변환
     *
     * @param dto AI 쓰레기 분류 결과
     * @return 사용자에게 표시할 피드백 문구
     */
    private String createFeedbackText(AiFeedbackRequestDto dto) {

        // 올바르게 분리수거한 경우 실패 피드백 문구가 필요하지 않음
        if (dto.getStatus() == DetectionStatus.ALLOWED) {
            return null;
        }

        // 내용물 비우기, 라벨 제거, 압착 등의 재처리 안내가 존재하는 경우
        if (dto.getGuidance() != null && !dto.getGuidance().isEmpty()) {
            String guidanceText = dto.getGuidance().stream()
                    // 각 안내 항목에서 사용자 표시 문구 추출
                    .map(AiFeedbackRequestDto.GuidanceDto::getMessage)
                    // null 또는 빈 문구 제외
                    .filter(StringUtils::hasText)
                    // 안내가 여러 개라면 줄바꿈으로 연결
                    .collect(Collectors.joining("\n"));

            // 생성된 안내 문구가 있는 경우 반환
            if (StringUtils.hasText(guidanceText)) {
                return guidanceText;
            }
        }

        // 유리, 건전지, 형광등 등 완전 수거 거부 결과인 경우
        if (dto.getRejection() != null
                && StringUtils.hasText(dto.getRejection().getMessage())) {
            return dto.getRejection().getMessage();
        }

        // 비닐, 신뢰도 미달 등 일반쓰레기 결과인 경우
        if (dto.getGeneral() != null
                && StringUtils.hasText(dto.getGeneral().getMessage())) {
            return dto.getGeneral().getMessage();
        }

        // AI가 객체를 감지하지 못한 경우
        if (dto.getStatus() == DetectionStatus.NOT_DETECTED) {
            return "쓰레기를 인식하지 못했습니다. 다시 시도해 주세요.";
        }

        // 실패 상태이지만 구체적인 안내 문구가 없는 경우 기본 문구 반환
        return "분리수거 조건을 확인한 후 다시 시도해 주세요.";
    }
}
