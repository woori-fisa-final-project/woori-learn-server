package dev.woori.wooriLearn.domain.scenario.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.service.PointsDepositService;
import dev.woori.wooriLearn.domain.scenario.content.StepMeta;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressResumeResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressSaveResDto;
import dev.woori.wooriLearn.domain.scenario.dto.QuizResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ScenarioRewardResDto;
import dev.woori.wooriLearn.domain.scenario.entity.*;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import dev.woori.wooriLearn.domain.scenario.model.ChoiceInfo;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioProgressRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioStepRepository;
import dev.woori.wooriLearn.domain.scenario.service.processor.ContentInfo;
import dev.woori.wooriLearn.domain.scenario.service.processor.StepProcessor;
import dev.woori.wooriLearn.domain.scenario.service.processor.StepProcessorResolver;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 시나리오 진행(재개/다음 스텝/체크포인트 저장)을 담당하는 서비스
 *
 * 진행률 계산 규칙:
 * - Scenario.totalNormalSteps : "정상 루트"에 포함되는 스텝 개수
 * - ScenarioStep.normalIndex  : "정상 루트"에서의 순번(1-base)
 * - 진행률 : 0 ~ 100 사이의 수, 소수점 첫째자리까지
 *
 * normalIndex == null 인 스텝은 정상 루트에 포함되지 않음
 *
 * 배드 브랜치 규칙:
 * - meta.branch == "bad" 인 스텝은 배드 브랜치로 간주
 * - meta.badEnding == true 인 스텝은 배드 엔딩으로 간주
 * - 배드 브랜치에 있는 동안은 진행률을 "동결" (증가시키지 않음)
 * - 배드 엔딩 스텝의 nextStep 은 복귀 지점(보통 CHOICE 스텝)으로 설정
 */
@Service
@RequiredArgsConstructor
public class ScenarioProgressService {
    private final UserRepository userRepository;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioStepRepository stepRepository;
    private final ScenarioProgressRepository progressRepository;
    private final ScenarioCompletedRepository completedRepository;
    private final PointsDepositService pointsDepositService;

    private static final int SCENARIO_COMPLETION_REWARD_POINTS = 1000;
    private static final int ALL_SCENARIOS_COMPLETION_REWARD_POINTS = 10000;
    // 스텝 타입/상태에 따라 적절한 StepProcessor를 찾아주는 Resolver
    private final StepProcessorResolver stepProcessorResolver;

    // content JSON 파싱/DTO 매핑 전담 서비스
    private final ScenarioStepContentService contentService;

    /**
     * 시나리오 진행 재개
     *
     * - 사용자의 ScenarioProgress가 존재하면 해당 진행 스텝에서 재개
     * - 없다면 시나리오의 시작 스텝부터 시작
     */
    /**
     * 시나리오 진행 재개
     * - 사용자의 ScenarioProgress가 존재하면 해당 진행 스텝에서 재개
     * - 없다면 시나리오의 시작 스텝부터 시작
     *
     * @param user          현재 요청을 보낸 사용자
     * @param scenarioId    재개할 시나리오 ID
     * @return 현재 스텝 정보를 담은 DTO
     */
    @Transactional(readOnly = true)
    public ProgressResumeResDto resume(Users user, Long scenarioId) {
        // 1) 시나리오 존재 여부 검증
        Scenario scenario = getScenarioOrThrow(scenarioId);

        // 2) 유저의 진행 이력 조회 -> 있으면 해당 스텝, 없으면 시작 스텝 계산
        ScenarioStep step = progressRepository.findByUserAndScenario(user, scenario)
                .map(ScenarioProgress::getStep)
                .orElseGet(() -> stepRepository.findStartStepOrFail(scenarioId));

        // 3) 현재 스텝을 클라이언트 응답 DTO로 매핑
        return mapStep(step);
    }

    /**
     * 다음 스텝으로 진행
     * - 사용자의 진행 엔티티 조회/생성
     * - 현재 스텝의 메타 정보를 한 번만 파싱하여 배트 브랜치/배드 엔딩 여부 확인
     * - stepContext를 구성해 stepProcessorResolver에 전달
     * @param user          현재 사용자
     * @param scenarioId    진행 중인 시나리오 ID
     * @param nowStepId     사용자가 머무르고 있는 현재 스텝 ID
     * @param answer        사용자가 제출한 답(퀴즈/선택지)
     * @return 다음 상태/스텝/퀴즈 정보를 담은 AdvanceResDto
     */
    @Transactional
    public AdvanceResDto advance(Users user, Long scenarioId, Long nowStepId, Integer answer) {
        // 공통 로딩 로직
        StepRuntime runtime = loadStepRuntime(user, scenarioId, nowStepId);

        Scenario scenario = runtime.scenario();
        Map<Long, ScenarioStep> byId = runtime.byId();
        ScenarioStep current = runtime.current();
        ScenarioProgress progress = runtime.progress();

        // 시작 스텝 ID
        Long startStepId = stepRepository.findStartStepOrFail(scenarioId).getId();

        // Processor에 넘길 Context 구성
        StepContext ctx = new StepContext(user, scenario, current, answer, byId, progress, runtime.badBranch(), runtime.badEnding(), startStepId, runtime.hasChoices());

        StepProcessor processor = stepProcessorResolver.resolve(ctx);
        return processor.process(ctx, this);
    }

    /**
     * 현재 스텝을 체크포인트로 저장
     * ScenarioProgress 조회/생성
     * 해당 스텝이 배드 브랜치 또는 배드 엔딩이면 진행률 동결
     * 그 외의 경우 normalIndex / totalNormalSteps 기반으로 진행률 업데이트
     * @param user          현재 사용자
     * @param scenarioId    체크포인트를 저장할 시나리오 ID
     * @param nowStepId     사용자가 머무르고 있는 스텝 ID
     * @return 저장된 스텝 ID와 최종 진행률을 포함한 DTO
     */
    @Transactional
    public ProgressSaveResDto saveCheckpoint(Users user, Long scenarioId, Long nowStepId) {
        // 공통 로딩 로직
        StepRuntime runtime = loadStepRuntime(user, scenarioId, nowStepId);

        Scenario scenario = runtime.scenario();
        ScenarioStep current = runtime.current();
        ScenarioProgress progress = runtime.progress();

        boolean forceFreeze = runtime.isBad();

        double finalRate = updateProgressAndSave(progress, current, scenario, forceFreeze);
        return new ProgressSaveResDto(scenarioId, current.getId(), finalRate);
    }

    /**
     * 진행률 계산 + 단조 증가 보장 + 위치 저장을 한 번에 처리하는 헬퍼 메서드
     *
     * @param progress      사용자 진행 엔티티
     * @param newStep       이동할 스텝
     * @param scenario      시나리오(진행률 계산에 필요). forceFreeze=true 이면 null 가능
     * @param forceFreeze   true면 진행률을 변경하지 않고 위치만 저장
     * @return 최종 progressRate 값
     */
    public double updateProgressAndSave(ScenarioProgress progress,
                                 ScenarioStep newStep,
                                 Scenario scenario,
                                 boolean forceFreeze) {

        double finalRate;
        if (forceFreeze || scenario == null) {
            // 진행률 동결 : 위치만 갱신
            progress.moveToStep(newStep);
            progressRepository.save(progress);
            finalRate = (progress.getProgressRate() == null) ? 0.0 : progress.getProgressRate();
            return finalRate;
        }

        // 정상 루트 상에서의 진행률 계산
        Double computed = computeProgressRateOnNormalPath(scenario, newStep);
        if (computed == null) {
            // 정상 루트에 속하지 않는 스텝(배드/연습용 등) -> 진행률 동결
            progress.moveToStep(newStep);
            progressRepository.save(progress);
            finalRate = (progress.getProgressRate() == null) ? 0.0 : progress.getProgressRate();
        } else {
            // 진행률 후보 값과 기존 값 중 더 큰 값으로 단조 증가 보장
            double rate = monotonicRate(progress, computed);
            progress.moveToStep(newStep, rate);
            progressRepository.save(progress);
            finalRate = rate;
        }
        return finalRate;
    }

    /**
     * normalIndex / totalNormalSteps 기반 진행률 계산
     * - totalNormalSteps 또는 normalIndex 가 null/0 이면 정상 루트가 아니라고 보고 null 반환
     * @param scenario  진행 중인 시나리오
     * @param step      진행률을 계산할 대상 스텝
     * @return 진행률(%)
     */
    Double computeProgressRateOnNormalPath(Scenario scenario, ScenarioStep step) {
        if (scenario == null || step == null) {
            return null;
        }
        Integer total = scenario.getTotalNormalSteps();
        Integer idx = step.getNormalIndex();

        if (total == null || total <= 0 || idx == null || idx <= 0) {
            return null; // 정상 루트에 포함되지 않는 스텝
        }

        double pct = (idx * 100.0) / total;
        return normalizeProgress(pct);
    }

    /**
     * 한 시나리오의 모든 스텝을 한 번에 로딩해서 Map 형태로 반환
     * - N+1 문제를 피하기 위해 findByScenarioIdWithNextStep (JOIN FETCH) 사용
     * @param scenarioId    대상 시나리오 ID
     * @return key: stepId, value: ScenarioStep 엔티티
     */
    Map<Long, ScenarioStep> preloadStepsAsMap(Long scenarioId) {
        List<ScenarioStep> steps = stepRepository.findByScenarioIdWithNextStep(scenarioId);
        if (steps.isEmpty()) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "스텝이 비어있습니다. scenarioId=" + scenarioId
            );
        }
        Map<Long, ScenarioStep> byId = new LinkedHashMap<>();
        for (ScenarioStep s : steps) {
            byId.putIfAbsent(s.getId(), s);
        }
        return byId;
    }

    /**
     * 진행률(0 ~ 100), 기존 진행률보다 뒤로 가지 않도록 하는 헬퍼
     * @param progress  현재 진행 엔티티
     * @param candidate 새로 계산된 진행률 후보값
     * @return 이전 값과 후보 값 중 더 큰 값
     */
    double monotonicRate(ScenarioProgress progress, double candidate) {
        double prev = (progress.getProgressRate() == null) ? 0.0 : progress.getProgressRate();
        double roundedCandidate = normalizeProgress(candidate);
        return Math.max(prev, roundedCandidate);
    }

    /** 동일 유저/시나리오에 대해 완료 이력을 1회만 저장하도록 보장 */
    boolean ensureCompletedOnce(Users user, Scenario scenario) {
        try {
            completedRepository.save(
                    ScenarioCompleted.builder()
                            .user(user)
                            .scenario(scenario)
                            .build()
            );
            return true;
        } catch (DataIntegrityViolationException e) {
            // 동시성 문제로 이미 완료 이력이 저장된 경우, false를 반환하여 정상 처리합니다.
            return false;
        }
    }

    /** 외부 Processor가 진행 엔티티 저장만 필요할 때 사용 */
    void saveProgress(ScenarioProgress progress) {
        progressRepository.save(progress);
    }
    private void grantCompletionRewards(Users user) {
        // 개별 시나리오 완료 보상
        pointsDepositService.depositPoints(
                user.getUserId(),
                new PointsDepositRequestDto(SCENARIO_COMPLETION_REWARD_POINTS, "시나리오 완료 보상")
        );
        // 전체 시나리오 완주 보상 (모든 시나리오 완료 시 1회)
        long totalScenarioCount = scenarioRepository.count();
        long userCompletedCount = completedRepository.countByUser(user);
        if (totalScenarioCount > 0 && userCompletedCount == totalScenarioCount) {
            pointsDepositService.depositPoints(
                    user.getUserId(),
                    new PointsDepositRequestDto(ALL_SCENARIOS_COMPLETION_REWARD_POINTS, "전체 시나리오 완주 보상")
            );
        }
    }
    @Transactional
    public AdvanceResDto handleScenarioCompletion(StepContext ctx) {

        // 🔒 user 객체 비관적 잠금 조회
        Users lockedUser = userRepository.findByIdForUpdate(ctx.user().getId())
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. id=" + ctx.user().getId()
                ));

        Scenario scenario = ctx.scenario();
        ScenarioProgress progress = ctx.progress();

        boolean newlyCompleted = ensureCompletedOnce(lockedUser, scenario);
        if (newlyCompleted) {
            grantCompletionRewards(lockedUser); // 중복 로직 제거된 리팩토링 메소드
        }

        double rate = monotonicRate(progress, 100.0);

        ScenarioStep start = ctx.startStep();
        if (start == null) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR,
                    "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenario.getId());
        }

        progress.moveToStep(start, rate);
        saveProgress(progress);

        return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
    }


    /**
     * 시나리오 완료 보상(포인트) 수동 지급
     * - ScenarioCompleted 기반으로 최초 1회만 지급
     */
    @Transactional
    public ScenarioRewardResDto claimScenarioReward(Users user, Long scenarioId) {

        // 🔒 user 비관적 잠금 조회
        Users lockedUser = userRepository.findByIdForUpdate(user.getId())
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. id=" + user.getId()
                ));

        Scenario scenario = getScenarioOrThrow(scenarioId);

        boolean newlyCompleted = ensureCompletedOnce(lockedUser, scenario);
        if (!newlyCompleted) {
            return new ScenarioRewardResDto(false, "이미 시나리오 보상을 받았습니다.");
        }

        grantCompletionRewards(lockedUser);

        return new ScenarioRewardResDto(true, "시나리오 완료 보상이 지급되었습니다.");
    }



    /**
     * ScenarioStep -> ProgressResumeResDto 매핑
     * - 실제 JSON 파싱/구조환는 ScenarioStepContentService에 위임
     * @param step 응답으로 내려줄 스텝 엔티티
     * @return 재개용 DTO
     */
    public ProgressResumeResDto mapStep(ScenarioStep step) {
        return contentService.mapStep(step);
    }

    /**
     * Quiz 엔티티 -> QuizResDto 매핑
     * - options 의 JSON 배열 문자열을 List<String> 으로 파싱
     */
    public QuizResDto mapQuiz(Quiz quiz) {
        return contentService.mapQuiz(quiz);
    }

    /**
     * CHOICE 스텝 content에서 사용자가 선택한 인덱스에 해당하는 ChoiceOption을 파싱하여, ChoiceInfo로 변환
     * @param step          CHOICE 타입 스텝
     * @param answerIndex   사용자가 선택한 인덱스
     * @return 선택 결과(정답 여부, 다음 스텝 ID)를 담은 ChoiceInfo
     */
    public ChoiceInfo parseChoice(ScenarioStep step, int answerIndex) {
        return contentService.parseChoice(step, answerIndex);
    }

    /**
     * 진행률 값(0 ~ 100), 소수점 첫째자리까지 반올림
     */
    private double normalizeProgress(double value) {
        double bounded = Math.min(100.0, Math.max(0.0, value));
        return Math.round(bounded * 10.0) / 10.0;
    }

    /**
     * 시나리오 존재 여부 검증 메서드
     */
    private Scenario getScenarioOrThrow(Long scenarioId) {
        return scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "시나리오가 존재하지 않습니다. id=" + scenarioId
                ));
    }

    private record StepRuntime(
            Scenario scenario,
            Map<Long, ScenarioStep> byId,
            ScenarioStep current,
            ScenarioProgress progress,
            Optional<StepMeta> metaOpt,
            boolean badBranch,
            boolean badEnding,
            boolean hasChoices
    ) {
        boolean isBad() {
            return badBranch || badEnding;
        }
    }

    private StepRuntime loadStepRuntime(Users user, Long scenarioId, Long nowStepId) {
        // 1) 시나리오 검증
        Scenario scenario = getScenarioOrThrow(scenarioId);

        // 2) 스텝 맵 로딩
        Map<Long, ScenarioStep> byId = preloadStepsAsMap(scenarioId);

        // 3) 현재 스텝 검증
        ScenarioStep current = byId.get(nowStepId);
        if (current == null || !Objects.equals(current.getScenario().getId(), scenarioId)) {
            throw new CommonException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "스텝이 존재하지 않거나 시나리오와 불일치. stepId=" + nowStepId
            );
        }

        // 4) 진행 엔티티 조회/생성
        ScenarioProgress progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgress.builder()
                        .user(user)
                        .scenario(scenario)
                        .step(current)
                        .progressRate(0.0)
                        .build());

        // 5) 메타 정보 한 번만 파싱하여 배드 브랜치/배드 엔딩 여부 확인
        ContentInfo info = contentService.parseContentInfo(current);
        Optional<StepMeta> metaOpt = info.meta();
        boolean hasChoices = info.hasChoices();

        boolean badBranch = metaOpt
                .map(meta -> "bad".equalsIgnoreCase(meta.branch()))
                .orElse(false);
        boolean badEnding = metaOpt
                .map(meta -> Boolean.TRUE.equals(meta.badEnding()))
                .orElse(false);

        boolean hasQuiz = current.getQuiz() != null;
        if (hasChoices && hasQuiz) {
            throw new CommonException(
                    ErrorCode.CONFLICT,
                    "시나리오 정의 오류: 하나의 스텝에 quiz와 choices가 동시에 존재할 수 없습니다. " +
                            "scenarioId=" + scenarioId + ", stepId=" + nowStepId
            );
        }

        return new StepRuntime(scenario, byId, current, progress, metaOpt, badBranch, badEnding, hasChoices);
    }
}
