package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressResumeResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressSaveResDto;
import dev.woori.wooriLearn.domain.scenario.dto.QuizResDto;
import dev.woori.wooriLearn.domain.scenario.entity.*;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioProgressRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioStepRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 시나리오 진행(재개/다음 스텝/체크포인트 저장)을 담당하는 서비스
 *
 * 진행률 계산 규칙:
 * - Scenario.totalNormalSteps : "정상 루트"에 포함되는 스텝 개수
 * - ScenarioStep.normalIndex  : "정상 루트"에서의 순번(1-base)
 * - 진행률 = (normalIndex / totalNormalSteps) * 100
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

    private final ScenarioRepository scenarioRepository;
    private final ScenarioStepRepository stepRepository;
    private final ScenarioProgressRepository progressRepository;
    private final ScenarioCompletedRepository completedRepository;
    private final ObjectMapper objectMapper;

    // 스텝 타입/상태에 따라 적절한 StepProcessor를 찾아주는 Resolver
    private final StepProcessorResolver stepProcessorResolver;

    /**
     * 시나리오 진행 재개
     *
     * - 사용자의 ScenarioProgress가 존재하면: 해당 진행 스텝에서 재개
     * - 없다면: 시나리오의 시작 스텝부터 시작
     */
    @Transactional(readOnly = true)
    public ProgressResumeResDto resume(Users user, Long scenarioId) {
        // 1) 시나리오 존재 여부 검증
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "시나리오가 존재하지 않습니다. id=" + scenarioId
                ));

        // 2) 유저의 진행 이력 조회 -> 있으면 해당 스텝, 없으면 시작 스텝 계산
        ScenarioStep step = progressRepository.findByUserAndScenario(user, scenario)
                .map(ScenarioProgress::getStep)
                .orElseGet(() -> {
                    Map<Long, ScenarioStep> byId = preloadStepsAsMap(scenarioId);
                    Long startId = inferStartStepId(byId);
                    ScenarioStep start = byId.get(startId);
                    if (start == null) {
                        throw new CommonException(
                                ErrorCode.INTERNAL_SERVER_ERROR,
                                "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenarioId
                        );
                    }
                    return start;
                });

        // 3) 현재 스텝을 클라이언트 응답 DTO로 매핑
        return mapStep(step);
    }

    /**
     * 다음 스텝으로 진행
     *
     * - StepProcessorResolver가 현재 스텝 타입/상태에 맞는 처리기를 선택하고
     * - 개별 Processor가 실제 로직(CHOICE/배드 브랜치/퀴즈/일반)을 수행
     */
    @Transactional
    public AdvanceResDto advance(Users user, Long scenarioId, Long nowStepId, Integer answer) {
        // 1) 시나리오 검증
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "시나리오가 존재하지 않습니다. id=" + scenarioId
                ));

        // 2) 해당 시나리오의 모든 스텝을 한 번에 로딩(Map 형태로 보관)
        Map<Long, ScenarioStep> byId = preloadStepsAsMap(scenarioId);

        // 3) 현재 스텝 검증
        ScenarioStep current = byId.get(nowStepId);
        if (current == null || !Objects.equals(current.getScenario().getId(), scenarioId)) {
            throw new CommonException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "스텝이 존재하지 않거나 시나리오와 불일치. stepId=" + nowStepId
            );
        }

        // 4) 사용자 진행 엔티티 조회(없으면 새로 생성)
        ScenarioProgress progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgress.builder()
                        .user(user)
                        .scenario(scenario)
                        .step(current)
                        .progressRate(0.0)
                        .build());

        // 현재 스텝이 배드 브랜치/배드 엔딩인지 미리 계산
        boolean badBranch = isBadBranch(current);
        boolean badEnding = isBadEnding(current);

        // 5) Processor 에 넘길 Context 구성
        StepContext ctx = new StepContext(user, scenario, current, answer, byId, progress, badBranch, badEnding);

        // 6) 스텝 타입/상태에 맞는 Processor 선택 & 실행
        StepProcessor processor = stepProcessorResolver.resolve(ctx);
        return processor.process(ctx, this);
    }

    /**
     * 현재 스텝을 "체크포인트"로 저장
     *
     * - 배드 브랜치/배드 엔딩이면: 진행률 동결, 위치만 저장
     * - 그 외: 시나리오의 normalIndex / totalNormalSteps 기반으로 진행률 계산
     */
    @Transactional
    public ProgressSaveResDto saveCheckpoint(Users user, Long scenarioId, Long nowStepId) {
        // 1) 시나리오 검증
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "시나리오가 존재하지 않습니다. id=" + scenarioId
                ));

        // 2) 스텝 맵 로딩
        Map<Long, ScenarioStep> byId = preloadStepsAsMap(scenarioId);

        // 3) 현재 스텝 검증
        ScenarioStep step = byId.get(nowStepId);
        if (step == null || !Objects.equals(step.getScenario().getId(), scenarioId)) {
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
                        .progressRate(0.0)
                        .build());

        // 배드 브랜치/엔딩이면 진행률 동결
        boolean forceFreeze = isBadBranch(step) || isBadEnding(step);
        double finalRate = updateProgressAndSave(progress, step, scenario, forceFreeze);

        return new ProgressSaveResDto(scenarioId, step.getId(), finalRate);
    }

    /**
     * 진행률 계산 + 단조 증가 보장 + 위치 저장을 한 번에 처리하는 헬퍼 메서드
     *
     * @param progress      사용자 진행 엔티티
     * @param newStep       이동할 스텝
     * @param scenario      시나리오(진행률 계산에 필요). forceFreeze=true 이면 null 가능
     * @param forceFreeze   true면 진행률을 변경하지 않고 "위치만" 저장
     * @return 최종 progressRate 값
     */
    double updateProgressAndSave(ScenarioProgress progress,
                                 ScenarioStep newStep,
                                 Scenario scenario,
                                 boolean forceFreeze) {

        double finalRate;
        if (forceFreeze || scenario == null) {
            // 진행률 동결 모드: 위치만 갱신
            progress.moveToStep(newStep);
            progressRepository.save(progress);
            finalRate = (progress.getProgressRate() == null) ? 0.0 : progress.getProgressRate();
            return finalRate;
        }

        // 정상 루트 상에서의 진행률 계산
        Double computed = computeProgressRateOnNormalPath(scenario, newStep);
        if (computed == null) {
            // 정상 루트에 속하지 않는 스텝(배드/연습용 등) → 진행률 동결
            progress.moveToStep(newStep);
            progressRepository.save(progress);
            finalRate = (progress.getProgressRate() == null) ? 0.0 : progress.getProgressRate();
        } else {
            // 진행률 후보 값과 기존 값 중 "더 큰 값"으로 단조 증가 보장
            double rate = monotonicRate(progress, computed);
            progress.moveToStep(newStep, rate);
            progressRepository.save(progress);
            finalRate = rate;
        }
        return finalRate;
    }

    /**
     * normalIndex / totalNormalSteps 기반 진행률 계산
     *
     * - totalNormalSteps 또는 normalIndex 가 null/0 이면 정상 루트가 아니라고 보고 null 반환
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
        return Math.min(100.0, Math.max(0.0, pct));
    }

    /**
     * 한 시나리오의 모든 스텝을 한 번에 로딩해서 Map 형태로 반환
     *
     * - N+1 문제를 피하기 위해 findByScenarioIdWithNextStep (JOIN FETCH) 사용
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
     * 시작 스텝 추론
     *
     * - 다른 스텝의 nextStep 으로 "참조되지 않는" 스텝 중 ID가 가장 작은 것을 시작점으로 선택
     * - 모든 스텝이 참조되어 있다면(이상 케이스), 단순히 ID 최솟값을 반환
     */
    Long inferStartStepId(Map<Long, ScenarioStep> byId) {
        Set<Long> referenced = new HashSet<>();
        for (ScenarioStep s : byId.values()) {
            if (s.getNextStep() != null) {
                referenced.add(s.getNextStep().getId());
            }
        }
        return byId.keySet().stream()
                .filter(id -> !referenced.contains(id))
                .min(Long::compareTo)
                .orElseGet(() -> byId.keySet().stream().min(Long::compareTo)
                        .orElseThrow(() -> new CommonException(
                                ErrorCode.INTERNAL_SERVER_ERROR,
                                "시작 스텝을 계산할 수 없습니다."
                        )));
    }

    /** 진행률을 0~100 범위로 자르고, 기존 진행률보다 "뒤로 가지 않도록" 하는 헬퍼 */
    double monotonicRate(ScenarioProgress progress, double candidate) {
        double prev = (progress.getProgressRate() == null) ? 0.0 : progress.getProgressRate();
        double bounded = Math.min(100.0, Math.max(0.0, candidate));
        return Math.max(prev, bounded);
    }

    /** 동일 유저/시나리오에 대해 완료 이력을 1회만 저장하도록 보장 */
    void ensureCompletedOnce(Users user, Scenario scenario) {
        if (!completedRepository.existsByUserAndScenario(user, scenario)) {
            completedRepository.save(
                    ScenarioCompleted.builder()
                            .user(user)
                            .scenario(scenario)
                            .build()
            );
        }
    }

    /** 외부 Processor 가 진행 엔티티 저장만 필요할 때 사용 */
    void saveProgress(ScenarioProgress progress) {
        progressRepository.save(progress);
    }

    /**
     * ScenarioStep → ProgressResumeResDto 매핑
     * - content JSON 문자열을 JsonNode 로 파싱해서 내려줌
     */
    ProgressResumeResDto mapStep(ScenarioStep step) {
        try {
            JsonNode contentNode = objectMapper.readTree(step.getContent());
            return new ProgressResumeResDto(
                    step.getScenario().getId(),
                    step.getId(),
                    step.getType(),
                    step.getQuiz() != null ? step.getQuiz().getId() : null,
                    contentNode
            );
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "스텝 content JSON 파싱 실패. stepId=" + step.getId()
            );
        }
    }

    /**
     * Quiz 엔티티 → QuizResDto 매핑
     * - options 의 JSON 배열 문자열을 List<String> 으로 파싱
     */
    QuizResDto mapQuiz(Quiz quiz) {
        try {
            var listType = TypeFactory.defaultInstance()
                    .constructCollectionType(List.class, String.class);
            List<String> opts = objectMapper.readValue(quiz.getOptions(), listType);
            return new QuizResDto(quiz.getId(), quiz.getQuestion(), opts);
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "퀴즈 options 파싱 실패. quizId=" + quiz.getId()
            );
        }
    }

    record ChoiceInfo(boolean good, Long nextStepId) {}

    JsonNode findChoicesArray(JsonNode root) {
        if (root == null) {
            return null;
        }

        if (root.isObject()) {
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray()) {
                return choices;
            }
        }

        if (root.isArray()) {
            // ["choices", [...]] 패턴
            for (int i = 0; i < root.size() - 1; i++) {
                JsonNode cur = root.get(i);
                JsonNode nxt = root.get(i + 1);
                if (cur.isTextual()
                        && "choices".equalsIgnoreCase(cur.asText())
                        && nxt != null
                        && nxt.isArray()) {
                    return nxt;
                }
            }
            // 배열 내 오브젝트의 {"choices":[...]} 패턴
            for (JsonNode el : root) {
                if (el.isObject()) {
                    JsonNode choices = el.get("choices");
                    if (choices != null && choices.isArray()) {
                        return choices;
                    }
                }
            }
        }
        return null;
    }

    /**
     * CHOICE 스텝의 content JSON 에서
     * answerIndex 에 해당하는 선택지를 파싱하여 ChoiceInfo 로 매핑
     */
    ChoiceInfo parseChoice(ScenarioStep step, int answerIndex) {
        JsonNode root;
        try {
            root = objectMapper.readTree(step.getContent());
        } catch (JsonProcessingException e) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "CHOICE content 파싱 실패. stepId=" + step.getId()
            );
        }

        JsonNode choices = findChoicesArray(root);
        if (choices == null || !choices.isArray()) {
            throw new CommonException(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    "CHOICE 스텝에 choices 배열이 없습니다. stepId=" + step.getId()
            );
        }
        if (answerIndex < 0 || answerIndex >= choices.size()) {
            throw new CommonException(
                    ErrorCode.INVALID_REQUEST,
                    "선택 인덱스 범위 초과. index=" + answerIndex
            );
        }

        JsonNode choice = choices.get(answerIndex);
        boolean good = choice.path("good").asBoolean(false);
        Long next = choice.hasNonNull("next") ? choice.get("next").asLong() : null;

        return new ChoiceInfo(good, next);
    }

    /**
     * 배드 브랜치 여부 판단
     * - content.meta.branch == "bad" 이면 true
     */
    boolean isBadBranch(ScenarioStep step) {
        return getMetaNode(step)
                .map(meta -> "bad".equalsIgnoreCase(meta.path("branch").asText(null)))
                .orElse(false);
    }

    /**
     * 배드 엔딩 여부 판단
     * - content.meta.badEnding == true 이면 true
     */
    boolean isBadEnding(ScenarioStep step) {
        return getMetaNode(step)
                .map(meta -> meta.path("badEnding").asBoolean(false))
                .orElse(false);
    }

    Optional<JsonNode> getMetaNode(ScenarioStep step) {
        try {
            JsonNode root = objectMapper.readTree(step.getContent());
            JsonNode meta = (root.isArray() && !root.isEmpty())
                    ? root.get(0).get("meta")
                    : root.get("meta");
            return Optional.ofNullable(meta);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }
}
