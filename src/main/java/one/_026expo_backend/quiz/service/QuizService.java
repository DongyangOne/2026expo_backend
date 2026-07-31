package one._026expo_backend.quiz.service;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import one._026expo_backend.character.domain.Character;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import one._026expo_backend.character.domain.UserCharacter;
import one._026expo_backend.character.enums.LevelPolicy;
import one._026expo_backend.character.repository.CharacterRepository;
import one._026expo_backend.character.repository.UserCharacterRepository;
import one._026expo_backend.global.enums.ErrorCode;
import one._026expo_backend.global.exception.BusinessException;
import one._026expo_backend.quiz.dto.request.StartQuizRequestDto;
import one._026expo_backend.quiz.dto.response.QuizResultResponseDto;
import one._026expo_backend.quiz.dto.response.StartQuizResponseDto;
import one._026expo_backend.global.enums.UseYnEnum;
import one._026expo_backend.quiz.domain.Quiz;
import one._026expo_backend.quiz.domain.QuizRecord;
import one._026expo_backend.quiz.dto.redis.QuizListSessionDto;
import one._026expo_backend.quiz.dto.request.NextQuizRequestDto;
import one._026expo_backend.quiz.dto.response.NextQuizResponseDto;
import one._026expo_backend.quiz.enums.QuizResultMessage;
import one._026expo_backend.quiz.repository.QuizRecordRepository;
import one._026expo_backend.quiz.repository.QuizRepository;
import one._026expo_backend.quiz.repository.QuizSessionRedisRepository;
import one._026expo_backend.user.repository.UserRepository;
import one._026expo_backend.user.domain.Users;
import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.QuizProfileInfo;
import static one._026expo_backend.user.dto.response.UserDashboardResponseDto.WrongQuizInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {
    private static final int QUIZ_CORRECT_EXP = 2;
    private static final int QUIZ_MAX_EXP = 20;
    private static final Pattern UUID_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final QuizRepository quizRepository;
    private final UserRepository userRepository;
    private final QuizSessionRedisRepository quizSessionRedisRepository;
    private final QuizRecordRepository quizRecordRepository;
    private final UserCharacterRepository userCharacterRepository;
    private final CharacterRepository characterRepository;
    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.url-expiry-hours}")
    private int urlExpiryHours;

    /**
     * 퀴즈 시작 로직
     *
     * @param userId 퀴즈를 시작하는 사용자의 고유 아이디
     * @param requestDto 퀴즈 개수를 포함하고 있는 dto
     * @return startQuizResponseDto 생성된 sessionId와 첫번째 문제 정보를 포함하고 있는 dto
     */
    public StartQuizResponseDto startQuiz(Long userId, StartQuizRequestDto requestDto) {
        //유저 존재여부 예외처리
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        var allQuizIds = new ArrayList<>(quizRepository.findAllQuizIds());

        //db에 퀴즈가 하나도 없을 때 예외처리
        if (allQuizIds.isEmpty()) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_FOUND);
        }
        //db에 있는 퀴즈 개수보다 유저가 요청한 개수가 더 많을 때 예외처리
        if (allQuizIds.size() < requestDto.getQuantity()) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_QUIZ);
        }

        Collections.shuffle(allQuizIds);
        var selectedQuizIds = allQuizIds.stream()
                .limit(requestDto.getQuantity())
                .toList();

        var firstQuizId = selectedQuizIds.get(0);
        var firstQuiz = quizRepository.findById(firstQuizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND)); //첫번재 퀴즈에 대한 db정보가 삭제된 경우 예외처리

        //session 데이터 생성 및 session id 저장
        String sessionId = quizSessionRedisRepository.save(userId, selectedQuizIds);

        return StartQuizResponseDto.of(sessionId, firstQuiz);
    }

    /**
     * 퀴즈 정답 제출 및 다음 문제 조회 로직
     *
     * @param userId 퀴즈 정답을 제출하는 사용자의 고유 아이디
     * @param sessionId 현재 진행 중인 퀴즈의 세션 아이디
     * @param requestDto 현재 퀴즈 id와 정답을 포함하고 있는 dto
     * @return NextQuizResponseDto 현재 퀴즈에 대한 정답여부와 피드백, 세션의 퀴즈 종료여부, 다음 퀴즈 정보를 포함하고 있는 dto
     */
    @Transactional
    public NextQuizResponseDto moveOnQuiz(Long userId, String sessionId, NextQuizRequestDto requestDto) {
        //요청 값 검증 진행
        requestDto.validate();
        validateSessionId(sessionId);

        //유저 존재여부 예외처리
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Redis에서 현재 사용자의 퀴즈 진행 상태를 조회합니다.
        QuizListSessionDto session = quizSessionRedisRepository.find(userId, sessionId);
        List<Long> quizIds = session.getQuizIds();
        int nextIndex = session.getNextIndex();

        // startQuiz에서 첫 문제를 이미 내려줬으므로, 현재 채점할 문제는 nextIndex - 1 위치입니다.
        int currentIndex = nextIndex - 1;

        // 잘못된 범위의 index값일 시 예외처리
        if (currentIndex < 0 || currentIndex >= quizIds.size()) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_STATE_CONFLICT);
        }

        // 서버 기준으로 현재 풀어야 하는 퀴즈 id
        Long expectedQuizId = quizIds.get(currentIndex);

        // 서버 기준 현재 퀴즈 id와 요청받은 현재 id가 다를 시 예외처리
        if (!expectedQuizId.equals(requestDto.getCurrentQuizId())) {
            throw new BusinessException(ErrorCode.INVALID_QUIZ_SEQUENCE);
        }

        // 서버 세션 기준으로 검증된 퀴즈 id로 현재 문제를 조회합니다.
        Quiz nowQuiz = quizRepository.findById(expectedQuizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        // 같은 퀴즈 세션 안에서 이미 제출한 문제인지 확인합니다.
        if (quizRecordRepository.existsByUsersAndQuizAndSessionId(user, nowQuiz, session.getSessionId())) {
            throw new BusinessException(ErrorCode.ALREADY_SOLVED_QUIZ);
        }

        //퀴즈 채점
        boolean isCorrect = nowQuiz.getAnswer().equals(requestDto.getAnswer());

        Integer earnedPoint = isCorrect ? QUIZ_CORRECT_EXP : 0;

        QuizRecord quizRecord = QuizRecord.builder()
                .users(user)
                .quiz(nowQuiz)
                .sessionId(session.getSessionId())
                .selectedAnswer(requestDto.getAnswer())
                .isCorrect(isCorrect ? UseYnEnum.Y : UseYnEnum.N)
                .earnedPoint(earnedPoint)
                .answeredAt(LocalDateTime.now())
                .build();
        try {
            quizRecordRepository.save(quizRecord);
        } catch (DataIntegrityViolationException e) {
            // 동시에 같은 세션의 같은 문제를 제출하면 DB unique 제약으로 중복 저장을 막습니다.
            throw new BusinessException(ErrorCode.ALREADY_SOLVED_QUIZ);
        }

        // 다음 요청에서 채점할 문제 위치를 한 칸 앞으로 이동합니다.
        int updatedNextIndex = nextIndex + 1;
        boolean finished = updatedNextIndex > quizIds.size();

        if (finished) { // 마지막 문제까지 풀이 완료 시, nextQuiz는 null로 반환합니다.
            quizSessionRedisRepository.complete(
                    userId,
                    session,
                    quizIds.size()
            );
            return NextQuizResponseDto.of(nowQuiz, null, isCorrect);
        }

        Long nextQuizId = quizIds.get(nextIndex);
        Quiz nextQuiz = quizRepository.findById(nextQuizId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUIZ_NOT_FOUND));

        //세션 정보(value) 업데이트
        quizSessionRedisRepository.updateNextIndex(
                userId,
                session,
                updatedNextIndex
        );

        return NextQuizResponseDto.of(nowQuiz, nextQuiz, isCorrect);
    }

    /**
     * 로그인한 사용자가 맞춘 문제 수와 전체 문제 수를 반환
     *
     * @param userId 퀴즈 기록을 조회하려는 사용자 고유 아이디
     * @return 사용자가 맞춘 문제 수와 푼 전체 문제 수
     */
    @Transactional(readOnly = true)
    public QuizProfileInfo getQuizProfileInfo(long userId) {
        int totalSolvedCount = quizRecordRepository.countByUsersId(userId);

        // 푼 문제가 0이라면 맞힌 것도 0일 것이므로 추가 쿼리를 날리지 않게 하기 위함
        if (totalSolvedCount == 0)
            return QuizProfileInfo.of(0, 0);

        int correctSolvedCount = quizRecordRepository.countByUsersIdAndIsCorrect(userId, UseYnEnum.Y);
        return QuizProfileInfo.of(correctSolvedCount, totalSolvedCount);

    }

    /**
     * 퀴즈 종료 및 결과 정산 로직
     *
     * @param userId 퀴즈를 종료하고 정산받는 사용자의 고유 아이디
     * @param sessionId 종료할 퀴즈 sessionId
     * @return QuizResultResponseDto 종료한 퀴즈 관련 정산 정보를 포함하는 dto
     */
    @Transactional
    public QuizResultResponseDto resultQuiz(Long userId, String sessionId){
        //요청 값 검증
        validateSessionId(sessionId);

        //유저 존재 여부 예외처리
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        //세션 정보 가져오기
        QuizListSessionDto session = quizSessionRedisRepository.find(userId, sessionId);

        //중복 요청 시 경험치 중복 추가 방지
        if (!quizSessionRedisRepository.lockReward(sessionId)) {
            throw new BusinessException(ErrorCode.QUIZ_SESSION_COMPLETE_FAILED);
        }

        //해당 퀴즈가 끝나지 않은 상태일 시 예외처리
        if (!Boolean.TRUE.equals(session.getFinished())) {
            throw new BusinessException(ErrorCode.QUIZ_NOT_FINISHED);
        }

        //퀴즈 문제 개수와 기록 개수를 비교하여 다를 경우 예외처리
        int totalCount = session.getQuizIds().size();
        long recordCount = quizRecordRepository.countByUsersAndSessionId(user, sessionId);
        if (recordCount != totalCount) {
            throw new BusinessException(ErrorCode.QUIZ_RESULT_RECORD_NOT_MATCHED);
        }

        //정답인 문제 개수를 세고 경험치로 환산
        int correctCount = (int) quizRecordRepository.countByUsersAndSessionIdAndIsCorrect(
                user,
                sessionId,
                UseYnEnum.Y
        );

        // DB에 저장된 이번 퀴즈 세션의 모든 earnedPoint를 합산해서 획득 경험치로 사용
        int totalEarnedExp = quizRecordRepository.sumEarnedPointByUsersAndSessionId(
                user,
                sessionId
        );
        int earnedExp = Math.min(totalEarnedExp, QUIZ_MAX_EXP);

        // 유저 캐릭터 정보 가져오기
        UserCharacter userCharacter = userCharacterRepository.findFirstByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_CHARACTER_NOT_FOUND));

        int beforeLevel = userCharacter.getCurrentLevel();
        int beforeExp = userCharacter.getCurrentExp();

        // 파라미터 1개짜리 addExp 호출 (Enum에서 레벨업 요구치 자동 계산)
        userCharacter.addExp(earnedExp);
        syncCharacterWithLevel(userCharacter);

        // 결과 정산 후, 현재 유저의 새로운 레벨 기준 최대 경험치를 Enum에서 조회
        int currentMaxExp = LevelPolicy.getMaxExpForLevel(userCharacter.getCurrentLevel());
        //결과에 따른 격려 및 칭찬 메세지 가져오기
        String resultMessage = QuizResultMessage.pick(correctCount, totalCount);

        //db커밋 성공 후 삭제하도록 예약
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        quizSessionRedisRepository.delete(userId);
                    }
                }
        );

        Character currentCharacter = userCharacter.getCharacter();

        return QuizResultResponseDto.of(
                totalCount,
                correctCount,
                earnedExp,
                resultMessage,
                userCharacter.getUserCharacterId(),
                currentCharacter.getCharacterId(),
                currentCharacter.getCharacterName(),
                getMinioImageUrl(currentCharacter.getImageUrl()),
                currentCharacter.getEvolutionStage(),
                beforeLevel,
                beforeExp,
                userCharacter.getCurrentLevel(),
                userCharacter.getCurrentExp(),
                currentMaxExp
        );
    }

    /**
     * 유저의 틀린 문제들 중 가장 최근의 것을 반환
     *
     * @param userId 틀린 문제를 반환할 사용자의 고유 ID
     * @return 푼 문제 중 틀린 문제가 있으면 가장 최근의 것을 반환, 없으면 null을 반환
     */
    @Transactional(readOnly = true)
    public WrongQuizInfo getLatestWrongQuiz(Long userId) {

        return quizRecordRepository
                .findFirstByUsersIdAndIsCorrectOrderByAnsweredAtDesc(userId, UseYnEnum.N)
                .map(record -> WrongQuizInfo.of(record.getQuiz()))
                .orElse(null);
    }

    private void syncCharacterWithLevel(UserCharacter userCharacter) {
        Character character = characterRepository
                .findFirstByEvolutionLevelLessThanEqualOrderByEvolutionLevelDesc(userCharacter.getCurrentLevel())
                .orElse(userCharacter.getCharacter());

        userCharacter.changeCharacter(character);
    }

    private void validateSessionId(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new BusinessException(ErrorCode.MISSING_SESSION_ID);
        }
        if (!UUID_PATTERN.matcher(sessionId).matches()) {
            throw new BusinessException(ErrorCode.INVALID_SESSION_FORMAT);
        }
    }

    private String getMinioImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(imageUrl)
                            .expiry(urlExpiryHours, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.IMAGE_URL_GENERATION_FAILED);
        }
    }
}
