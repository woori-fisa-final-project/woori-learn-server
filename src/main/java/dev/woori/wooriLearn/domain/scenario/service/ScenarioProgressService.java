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
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import dev.woori.wooriLearn.domain.scenario.model.StepType;
import dev.woori.wooriLearn.domain.scenario.repository.*;
import dev.woori.wooriLearn.domain.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 시나리오 진행(재개/다음 스텝/체크포인트 저장)을 담당하는 서비스
 */
@Service
@RequiredArgsConstructor
public class ScenarioProgressService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioStepRepository stepRepository;
    private final ScenarioProgressListRepository progressRepository;
    private final ScenarioCompletedRepository completedRepository;
    private final ObjectMapper objectMapper;

    /** 진행 재개: 저장된 진행 이력이 있으면 해당 스텝, 없으면 시작 스텝 */
    @Transactional(readOnly = true)
    public ProgressResumeResDto resume(Users user, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        // 진행 이력이 있으면 저장된 스텝으로, 없으면 시작 스텝으로
        ScenarioStep step = progressRepository.findByUserAndScenario(user, scenario)
                .map(ScenarioProgressList::getStep)
                .orElseGet(() -> {
                    Map<Long, ScenarioStep> byId = preloadStepsAsMap(scenarioId);
                    Long startId = inferStartStepId(byId);
                    ScenarioStep start = byId.get(startId);
                    if (start == null) {
                        throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenarioId);
                    }
                    return start;
                });

        return mapStep(step);
    }

    /**
     * 다음 스텝으로 진행
     *
     * CHOICE 스텝
     *      - 미선택 -> CHOICE_REQUIRED(선택 필요)
     *      - 오루트 -> next가 없으면 BAD_ENDING, 있으면 그 next로 이동(진행률 동결)
     *      - 정루트 -> next(or 일반 nextStep)로 이동(진행률 갱신)
     *      - 마지막 -> COMPLETED(진행률 100), 재개 지점은 시작 스텝
     *
     * 배드브랜치
     *      - 배드엔딩(meta.badEnding==true) → BAD_ENDING
     *      - 그 외 → 다음 배드 스텝으로 이동(있다면), 없으면 BAD_ENDING
     *      - 배드 브랜치에서는 항상 진행률 동결
     *
     * 퀴즈 스텝
     *      - 미제출/오답 → QUIZ_REQUIRED/QUIZ_WRONG (현재 스텝 유지)
     *      - 정답 → 일반 next로 진행(진행률 갱신)
     *
     * 일반 스텝
     *      - next가 없으면 COMPLETED(진행률 100) 처리
     *      - next가 있으면 진행률 갱신 후 이동
     */
    @Transactional
    public AdvanceResDto advance(Users user, Long scenarioId, Long nowStepId, Integer answer) {
        // 시나리오/스텝 선검증
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        Map<Long, ScenarioStep> byId = preloadStepsAsMap(scenarioId);

        ScenarioStep current = byId.get(nowStepId);
        if (current == null || !Objects.equals(current.getScenario().getId(), scenarioId)) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않거나 시나리오와 불일치. stepId=" + nowStepId);
        }

        // 사용자 진행 레코드 로드
        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgressList.builder()
                        .user(user).scenario(scenario).step(current).progressRate(0.0).build());

        // CHOICE
        if (current.getType() == StepType.CHOICE) {
            return handleChoiceStep(user, scenario, byId, current, answer, progress);
        }

        // 배드 브랜치
        if (isBadBranch(current)) {
            return handleBadBranchStep(byId, current, progress);
        }

        // 퀴즈 - 미제출/오답이면 여기서 바로 반환
        AdvanceResDto quizGateResult = handleQuizGateIfPresent(current, answer, progress);
        if (quizGateResult != null) {
            return quizGateResult;
        }

        // 일반 next
        return handleNormalStep(user, scenario, byId, current, progress);
    }

    /**
     * 체크포인트 저장
     * - 배드 브랜치: 진행률 동결(스텝만 저장)
     * - 정상: 정상 그래프 기반 % 계산, 최대값 유지
     */
    @Transactional
    public ProgressSaveResDto saveCheckpoint(Users user, Long scenarioId, Long nowStepId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        Map<Long, ScenarioStep> byId = preloadStepsAsMap(scenarioId);

        ScenarioStep step = byId.get(nowStepId);
        if (step == null) throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않거나 시나리오와 불일치. stepId=" + nowStepId);

        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgressList.builder()
                        .user(user).scenario(scenario).progressRate(0.0).build());

        if (isBadBranch(step)) {
            // 배드에서는 진행률 동결, 위치만 저장
            progress.moveToStep(step);
            progressRepository.save(progress);
            return new ProgressSaveResDto(scenarioId, step.getId(), progress.getProgressRate());
        }

        // 배드 브랜치면 진행률 동결
        boolean forceFreeze = isBadBranch(step);
        double finalRate = updateProgressAndSave(progress, step, byId, forceFreeze);

        return new ProgressSaveResDto(scenarioId, step.getId(), finalRate);
    }

    // =================
    //  스텝 유형별 핸들러
    // =================

    /** CHOICE 스텝 처리 */
    private AdvanceResDto handleChoiceStep(
            Users user,
            Scenario scenario,
            Map<Long, ScenarioStep> byId,
            ScenarioStep current,
            Integer answer,
            ScenarioProgressList progress
    ) {
        if (answer == null) {
            // 선택지 미제출 → 현재 스텝 유지
            progress.moveToStep(current);
            progressRepository.save(progress);
            return new AdvanceResDto(AdvanceStatus.CHOICE_REQUIRED, mapStep(current), null);
        }

        ChoiceInfo choice = parseChoice(current, answer);

        if (!choice.good()) {
            // 오루트 진입: next가 없으면 즉시 배드엔딩, 있으면 그 next로 이동(진행률 동결)
            Long nextId = choice.nextStepId();
            if (nextId == null) {
                progress.moveToStep(current);
                progressRepository.save(progress);
                return new AdvanceResDto(AdvanceStatus.BAD_ENDING, mapStep(current), null);
            }
            ScenarioStep nextWrong = byId.get(nextId);
            if (nextWrong == null) {
                throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "잘못된 경로 next가 존재하지 않습니다. id=" + nextId);
            }
            // 오루트에서는 진행률을 올리지 않음(동결)
            updateProgressAndSave(progress, nextWrong, byId, true);
            return new AdvanceResDto(AdvanceStatus.ADVANCED_FROZEN, mapStep(nextWrong), null);
        }

        // 정루트: 명시적 next가 있으면 우선, 없으면 일반 nextStep 사용
        Long nextId = (choice.nextStepId() != null)
                ? choice.nextStepId()
                : (current.getNextStep() != null ? current.getNextStep().getId() : null);

        if (nextId == null) {
            // 정루트의 마지막 → 완료 처리 + 다음 재개는 시작 스텝
            ensureCompletedOnce(user, scenario);
            double rate = monotonicRate(progress, 100.0);

            Long startId = inferStartStepId(byId);
            ScenarioStep start = byId.get(startId);
            if (start == null) throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenario.getId());

            progress.moveToStep(start, rate);
            progressRepository.save(progress);
            return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        }

        ScenarioStep next = byId.get(nextId);
        if (next == null) throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "다음 스텝이 존재하지 않습니다. id=" + nextId);

        // 정상 그래프 내부면 진행률 갱신, 외부면 동결(스텝만 이동)
        updateProgressAndSave(progress, next, byId, false);
        return new AdvanceResDto(AdvanceStatus.ADVANCED, mapStep(next), null);
    }

    /** 배드 브랜치 처리 */
    private AdvanceResDto handleBadBranchStep(
            Map<Long, ScenarioStep> byId,
            ScenarioStep current,
            ScenarioProgressList progress
    ) {
        if (isBadEnding(current)) {
            // 백트래킹 탐색으로 CHOICE 앵커를 찾고, 없으면 시작 스텝으로 복귀
            ScenarioStep anchorChoice = findChoiceAnchorForBadBranch(byId, current.getId());
            if (anchorChoice == null) anchorChoice = byId.get(inferStartStepId(byId));
            updateProgressAndSave(progress, anchorChoice, byId, true);
            return new AdvanceResDto(AdvanceStatus.BAD_ENDING, mapStep(current), null);
        }

        // 오루트 중간 스텝 → 다음 배드 스텝이 있으면 그쪽으로, 없으면 BAD_ENDING 동일 처리
        ScenarioStep next = current.getNextStep() != null ? byId.get(current.getNextStep().getId()) : null;
        if (next == null) {
            ScenarioStep anchorChoice = findChoiceAnchorForBadBranch(byId, current.getId());
            if (anchorChoice == null) anchorChoice = byId.get(inferStartStepId(byId));
            updateProgressAndSave(progress, anchorChoice, byId, true);
            return new AdvanceResDto(AdvanceStatus.BAD_ENDING, mapStep(current), null);
        }

        updateProgressAndSave(progress, next, byId, true);
        return new AdvanceResDto(AdvanceStatus.ADVANCED_FROZEN, mapStep(next), null);
    }

    /** 퀴즈 게이트 처리 */
    private AdvanceResDto handleQuizGateIfPresent(
            ScenarioStep current,
            Integer answer,
            ScenarioProgressList progress
    ) {
        Quiz quiz = current.getQuiz();
        if (quiz == null) return null;

        boolean isCorrect = answer != null && Objects.equals(quiz.getAnswer(), answer);
        if (!isCorrect) {
            // 미제출/오답 → 현재 스텝 유지
            updateProgressAndSave(progress, current, null, true);
            progressRepository.save(progress);
            AdvanceStatus status = (answer == null) ? AdvanceStatus.QUIZ_REQUIRED : AdvanceStatus.QUIZ_WRONG;
            return new AdvanceResDto(status, mapStep(current), mapQuiz(quiz));
        }
        return null;
    }

    /** 일반 next 처리 */
    private AdvanceResDto handleNormalStep(
            Users user,
            Scenario scenario,
            Map<Long, ScenarioStep> byId,
            ScenarioStep current,
            ScenarioProgressList progress
    ) {
        ScenarioStep next = current.getNextStep() != null ? byId.get(current.getNextStep().getId()) : null;
        if (next == null) {
            // 정루트 상의 마지막 → 완료
            ensureCompletedOnce(user, scenario);
            double rate = monotonicRate(progress, 100.0);

            Long startId = inferStartStepId(byId);
            ScenarioStep start = byId.get(startId);
            if (start == null) throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenario.getId());

            progress.moveToStep(start, rate);
            progressRepository.save(progress);
            return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        }

        updateProgressAndSave(progress, next, byId, false);
        return new AdvanceResDto(AdvanceStatus.ADVANCED, mapStep(next), null);
    }

    /**
     * 진행률 계산(정상 그래프 기반) + 단조 증가 보장 + 위치 저장을 한 번에 처리
     * @param progress   현재 사용자 진행 엔티티
     * @param newStep    이동할 스텝
     * @param byId       시나리오 내 모든 스텝 맵(정상 진행률 갱신 시 필요). 동결이면 null 가능
     * @param forceFreeze true면 진행률 갱신 없이 스텝만 이동(배드 브랜치/미제출 등)
     * @return 최종 progressRate
     */
    private double updateProgressAndSave(ScenarioProgressList progress,
                                         ScenarioStep newStep,
                                         Map<Long, ScenarioStep> byId,
                                         boolean forceFreeze) {

        double finalRate;
        if (forceFreeze) {
            // 진행률 변화 없이 위치만 저장
            progress.moveToStep(newStep);
            progressRepository.save(progress);
            finalRate = progress.getProgressRate() == null ? 0.0 : progress.getProgressRate();
            return finalRate;
        }

        // 정상 진행률 갱신 경로
        Double computed = computeProgressRateOnNormalGraph(byId, newStep.getId());
        if (computed == null) {
            // 정상 그래프 밖 → 동결
            progress.moveToStep(newStep);
            progressRepository.save(progress);
            finalRate = progress.getProgressRate() == null ? 0.0 : progress.getProgressRate();
        } else {
            double rate = monotonicRate(progress, computed);
            progress.moveToStep(newStep, rate);
            progressRepository.save(progress);
            finalRate = rate;
        }
        return finalRate;
    }

    // ===========
    //  진행률 계산
    // ===========

    /** 정상 경로 그래프 기반 진행률 계산 */
    private Double computeProgressRateOnNormalGraph(Map<Long, ScenarioStep> byId, Long nowStepId) {
        // 그래프 구축(정상 노드/엣지만)
        Graph g = buildNormalGraph(byId);

        if (!g.nodes.contains(nowStepId)) {
            return null; // 정상 경로 외 → 진행률 갱신하지 않음
        }

        // 시작 노드(진입차수 0) 집합
        Set<Long> starts = new HashSet<>();
        for (Long n : g.nodes) {
            if (g.indegree.getOrDefault(n, 0) == 0) starts.add(n);
        }
        if (starts.isEmpty()) {
            Long minId = g.nodes.stream().min(Long::compareTo).orElse(null);
            if (minId != null) starts.add(minId);
        }

        // BFS로 레벨(거리) 계산
        Map<Long, Integer> level = new HashMap<>();
        Deque<Long> dq = new ArrayDeque<>();
        for (Long s : starts) {
            level.put(s, 0);
            dq.add(s);
        }
        while (!dq.isEmpty()) {
            Long u = dq.poll();
            int lu = level.get(u);
            for (Long v : g.adj.getOrDefault(u, Collections.emptySet())) {
                int cand = lu + 1;
                if (!level.containsKey(v) || cand < level.get(v)) {
                    level.put(v, cand);
                    dq.add(v);
                }
            }
        }

        if (!level.containsKey(nowStepId)) {
            return null;
        }

        int maxLevel = 0;
        for (Long n : g.nodes) {
            Integer lv = level.get(n);
            if (lv != null) maxLevel = Math.max(maxLevel, lv);
        }

        // 레벨 기반 진행률 = (level(now)+1)/(maxLevel+1) * 100
        int nowLv = level.get(nowStepId);
        int denom = Math.max(1, maxLevel + 1);
        double pct = ((nowLv + 1) * 100.0) / denom;
        return Math.min(100.0, Math.max(0.0, pct));
    }

    /** 정상 경로 그래프 구성(배드 노드/엣지 제외, CHOICE.good 및 일반 nextStep만 포함) */
    private Graph buildNormalGraph(Map<Long, ScenarioStep> byId) {
        Set<Long> nodes = new HashSet<>();
        Map<Long, Set<Long>> adj = new HashMap<>();
        Map<Long, Integer> indegree = new HashMap<>();

        // 정상 노드 수집
        for (ScenarioStep s : byId.values()) {
            if (isBadBranch(s)) continue; // 배드 노드는 제외
            nodes.add(s.getId());
        }

        // 엣지 구성
        for (ScenarioStep s : byId.values()) {
            if (!nodes.contains(s.getId())) continue; // 배드 노드 제외

            if (s.getType() == StepType.CHOICE) {
                try {
                    JsonNode root = objectMapper.readTree(s.getContent());
                    JsonNode choices = findChoicesArray(root);
                    boolean anyGoodEdge = false;
                    if (choices != null) {
                        for (JsonNode c : choices) {
                            boolean good = c.path("good").asBoolean(false);
                            if (!good || !c.hasNonNull("next")) continue;
                            long nxt = c.get("next").asLong();
                            ScenarioStep target = byId.get(nxt);
                            if (target != null && !isBadBranch(target)) {
                                addEdge(nodes, adj, indegree, s.getId(), target.getId());
                                anyGoodEdge = true;
                            }
                        }
                    }
                    // choices가 없거나 good edge가 1개도 없으면 nextStep로 대체
                    if (!anyGoodEdge && s.getNextStep() != null && !isBadBranch(s.getNextStep())) {
                        addEdge(nodes, adj, indegree, s.getId(), s.getNextStep().getId());
                    }
                } catch (JsonProcessingException ignored) {
                    // 파싱 실패 시에도 nextStep로 대체 시도
                    if (s.getNextStep() != null && !isBadBranch(s.getNextStep())) {
                        addEdge(nodes, adj, indegree, s.getId(), s.getNextStep().getId());
                    }
                }
            } else {
                if (s.getNextStep() != null && !isBadBranch(s.getNextStep())) {
                    addEdge(nodes, adj, indegree, s.getId(), s.getNextStep().getId());
                }
            }
        }

        // indegree 보정: 고립 노드/시작 노드 기록
        for (Long n : nodes) indegree.putIfAbsent(n, 0);

        return new Graph(nodes, adj, indegree);
    }

    // 엣지 추가 헬퍼
    private void addEdge(Set<Long> nodes,
                         Map<Long, Set<Long>> adj,
                         Map<Long, Integer> indegree,
                         long u, long v) {
        if (!nodes.contains(u) || !nodes.contains(v)) return;
        Set<Long> set = adj.computeIfAbsent(u, k -> new LinkedHashSet<>());
        set.add(v);
        indegree.put(v, indegree.getOrDefault(v, 0) + 1);
        indegree.putIfAbsent(u, indegree.getOrDefault(u, 0));
    }

    private record Graph(Set<Long> nodes, Map<Long, Set<Long>> adj, Map<Long, Integer> indegree) {}

    /** 모든 스텝을 한 번에 로드하여 Map으로 반환 (JOIN FETCH로 N+1 회피) */
    private Map<Long, ScenarioStep> preloadStepsAsMap(Long scenarioId) {
        List<ScenarioStep> steps = stepRepository.findByScenarioIdWithNextStep(scenarioId);
        if (steps.isEmpty()) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "스텝이 비어있습니다. scenarioId=" + scenarioId);
        }
        Map<Long, ScenarioStep> byId = new LinkedHashMap<>();
        for (ScenarioStep s : steps) byId.putIfAbsent(s.getId(), s);
        return byId;
    }

    /** 시작 스텝 추론: nextStep으로 참조되지 않은 스텝 중 id 최솟값 */
    private Long inferStartStepId(Map<Long, ScenarioStep> byId) {
        Set<Long> referenced = new HashSet<>();
        for (ScenarioStep s : byId.values()) {
            if (s.getNextStep() != null) referenced.add(s.getNextStep().getId());
        }
        return byId.keySet().stream()
                .filter(id -> !referenced.contains(id))
                .min(Long::compareTo)
                .orElseGet(() -> byId.keySet().stream().min(Long::compareTo)
                        .orElseThrow(() -> new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 계산할 수 없습니다.")));
    }

    /** 진행률 단조 증가(최대값 유지) */
    private double monotonicRate(ScenarioProgressList progress, double candidate) {
        double prev = (progress.getProgressRate() == null) ? 0.0 : progress.getProgressRate();
        double bounded = Math.min(100.0, Math.max(0.0, candidate));
        return Math.max(prev, bounded);
    }

    /** 완료 이력 1회 보장(없을 때만 저장) */
    private void ensureCompletedOnce(Users user, Scenario scenario) {
        if (!completedRepository.existsByUserAndScenario(user, scenario)) {
            completedRepository.save(ScenarioCompleted.builder().user(user).scenario(scenario).build());
        }
    }

    private ProgressResumeResDto mapStep(ScenarioStep step) {
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
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "스텝 content JSON 파싱 실패. stepId=" + step.getId());
        }
    }

    private QuizResDto mapQuiz(Quiz quiz) {
        try {
            var listType = TypeFactory.defaultInstance()
                    .constructCollectionType(List.class, String.class);
            List<String> opts = objectMapper.readValue(quiz.getOptions(), listType);
            return new QuizResDto(quiz.getId(), quiz.getQuestion(), opts);
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "퀴즈 options 파싱 실패. quizId=" + quiz.getId());
        }
    }

    private record ChoiceInfo(boolean good, Long nextStepId) {}

    /** 다양한 위치/형태의 choices 배열을 찾아 반환 */
    private JsonNode findChoicesArray(JsonNode root) {
        if (root == null) return null;

        if (root.isObject()) {
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray()) return choices;
        }

        if (root.isArray()) {
            // ["choices", [...]] 패턴
            for (int i = 0; i < root.size() - 1; i++) {
                JsonNode cur = root.get(i);
                JsonNode nxt = root.get(i + 1);
                if (cur.isTextual() && "choices".equalsIgnoreCase(cur.asText()) && nxt != null && nxt.isArray()) {
                    return nxt;
                }
            }
            // 배열 내 오브젝트의 {"choices":[...]} 패턴
            for (JsonNode el : root) {
                if (el.isObject()) {
                    JsonNode choices = el.get("choices");
                    if (choices != null && choices.isArray()) return choices;
                }
            }
        }
        return null;
    }

    private ChoiceInfo parseChoice(ScenarioStep step, int answerIndex) {
        JsonNode root;
        try {
            root = objectMapper.readTree(step.getContent());
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "CHOICE content 파싱 실패. stepId=" + step.getId());
        }

        JsonNode choices = findChoicesArray(root);
        if (choices == null || !choices.isArray()) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "CHOICE 스텝에 choices 배열이 없습니다. stepId=" + step.getId());
        }
        if (answerIndex < 0 || answerIndex >= choices.size()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "선택 인덱스 범위 초과. index=" + answerIndex);
        }

        JsonNode choice = choices.get(answerIndex);
        boolean good = choice.path("good").asBoolean(false);
        Long next = choice.hasNonNull("next") ? choice.get("next").asLong() : null;

        return new ChoiceInfo(good, next);
    }

    /** 배드브랜치 여부(meta.branch=="bad") */
    private boolean isBadBranch(ScenarioStep step) {
        return getMetaNode(step)
                .map(meta -> "bad".equalsIgnoreCase(meta.path("branch").asText(null)))
                .orElse(false);
    }

    /** 배드엔딩 여부(meta.badEnding==true) */
    private boolean isBadEnding(ScenarioStep step) {
        return getMetaNode(step)
                .map(meta -> meta.path("badEnding").asBoolean(false))
                .orElse(false);
    }

    private Optional<JsonNode> getMetaNode(ScenarioStep step) {
        try {
            JsonNode root = objectMapper.readTree(step.getContent());
            JsonNode meta = (root.isArray() && !root.isEmpty()) ? root.get(0).get("meta") : root.get("meta");
            return Optional.ofNullable(meta);
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    /** 배드 브랜치를 시작시킨 CHOICE(앵커) 탐색 */
    private ScenarioStep findChoiceAnchorForBadBranch(Map<Long, ScenarioStep> byId, Long badStepId) {
        for (ScenarioStep s : byId.values()) {
            if (s.getType() != StepType.CHOICE) continue;

            try {
                JsonNode root = objectMapper.readTree(s.getContent());
                JsonNode choices = findChoicesArray(root);
                if (choices == null || !choices.isArray()) continue;

                for (JsonNode c : choices) {
                    boolean good = c.path("good").asBoolean(false);
                    if (good) continue; // 앵커는 오루트 분기만
                    if (!c.hasNonNull("next")) continue;

                    long startId = c.get("next").asLong();

                    Set<Long> visited = new HashSet<>();
                    ScenarioStep cur = byId.get(startId);
                    while (cur != null && !visited.contains(cur.getId())) {
                        if (Objects.equals(cur.getId(), badStepId)) {
                            return s;
                        }
                        visited.add(cur.getId());
                        cur = (cur.getNextStep() != null) ? byId.get(cur.getNextStep().getId()) : null;
                    }
                }
            } catch (JsonProcessingException ignored) {}
        }
        return null;
    }
}
