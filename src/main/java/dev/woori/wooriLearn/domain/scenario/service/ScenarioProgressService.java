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
 *
 * 핵심 기능 요약
 * - resume: 저장된 진행 이력이 있으면 해당 스텝, 없으면 시작 스텝 반환
 * - advance: 현재 스텝에서 다음 스텝으로 이동(퀴즈/CHOICE/배드브랜치 처리 포함)
 *      - CHOICE: 선택지 미선택 -> CHOICE_REQUIRED,
 *                선택지 선택(오루트) -> ADVANCED_FROZEN 또는 BAD_ENDING,
 *                선택지 선택(정루트) -> 정상 진행
 *      - 배드브랜치: content.meta.branch == "bad"로 판단
 *                  배드엔딩 도달 시 재개 앵커(해당 브랜치를 시작시킨 CHOICE)로 복귀 저장
 *      - 정루트 마지막: 완료 처리 후 다음 재개는 시작 스텝부터
 * - saveCheckpoint: 현재 스텝과 진행률(0 ~ 100) 저장(배드브랜치에서는 진행률 동결)
 */
@Service
@RequiredArgsConstructor
public class ScenarioProgressService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioStepRepository stepRepository;
    private final ScenarioProgressListRepository progressRepository;
    private final ScenarioCompletedRepository completedRepository;
    private final ObjectMapper objectMapper;

    /**
     * 진행 재개
     * - 사용자/시나리오 기준 진행 이력이 있으면 해당 스텝, 없으면 시작 스텝 반환
     */
    @Transactional(readOnly = true)
    public ProgressResumeResDto resume(Users user, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        // 진행 이력이 있으면 해당 스텝, 없으면 시작 스텝
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
     * - CHOICE: 선택지 미선택/오루트/정루트에 따라 상태 반환 및 이동
     * - 배드브랜치: 진행률 동결, 배드엔딩 도달 시 재개 앵커(브랜치를 시작시킨 CHOICE)로 복귀 저장
     * - 정루트 마지막: 완료 처리 후 재개는 시작 스텝부터
     * - 진행률은 최대값 유지
     */
    @Transactional
    public AdvanceResDto advance(Users user, Long scenarioId, Long nowStepId, Integer answer) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        // 1) 시나리오 스텝을 한 번에 로딩
        Map<Long, ScenarioStep> byId = preloadStepsAsMap(scenarioId);

        // 2) 현재 스텝 유효성 검증
        ScenarioStep current = byId.get(nowStepId);
        if (current == null || !Objects.equals(current.getScenario().getId(), scenarioId)) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않거나 시나리오와 불일치. stepId=" + nowStepId);
        }

        // 3) 사용자 진행 이력 로딩(없으면 초기값 생성)
        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgressList.builder()
                        .user(user).scenario(scenario).step(current).progressRate(0.0).build());

        // CHOICE 처리
        if (current.getType() == StepType.CHOICE) {
            if (answer == null) {
                // 선택지 미선택 → 현재 스텝 유지 + CHOICE_REQUIRED
                progress.moveToStep(current);
                progressRepository.save(progress);
                return new AdvanceResDto(AdvanceStatus.CHOICE_REQUIRED, mapStep(current), null);
            }

            var choice = parseChoice(current, answer); // {good, nextStepId}

            if (!choice.good()) {
                // 오루트 → 배드 브랜치 진입(진행률 동결). next가 없으면 즉시 BAD_ENDING
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

                // 사용자의 진행 상황 저장(재개용) + 진행률은 증가시키지 않음
                progress.moveToStep(nextWrong);
                progressRepository.save(progress);

                return new AdvanceResDto(AdvanceStatus.ADVANCED_FROZEN, mapStep(nextWrong), null);
            }

            // 정루트 → 다음 스텝으로 정상 진행
            Long nextId = (choice.nextStepId() != null)
                    ? choice.nextStepId()
                    : (current.getNextStep() != null ? current.getNextStep().getId() : null);

            if (nextId == null) {
                // 정루트 마지막 → 완료 처리 후, 재개 스텝은 '시작 스텝'
                ensureCompletedOnce(user, scenario);
                double rate = monotonicRate(progress, 100.0);

                Long startId = inferStartStepId(byId);
                ScenarioStep start = byId.get(startId);
                if (start == null) {
                    throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenarioId);
                }

                progress.moveToStep(start, rate); // 100% 유지 + 재개는 시작 스텝부터
                progressRepository.save(progress);
                return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
            }

            ScenarioStep next = byId.get(nextId);
            if (next == null) {
                throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "다음 스텝이 존재하지 않습니다. id=" + nextId);
            }

            double computed = computeProgressRate(byId, scenarioId, next.getId());
            double rate = monotonicRate(progress, computed);
            progress.moveToStep(next, rate);
            progressRepository.save(progress);
            return new AdvanceResDto(AdvanceStatus.ADVANCED, mapStep(next), null);
        }

        // 배드브랜치 처리
        if (isBadBranch(current)) {
            if (isBadEnding(current)) {
                // 배드엔딩 → 재개 앵커(해당 브랜치를 시작시킨 CHOICE)로 복귀 저장
                ScenarioStep anchorChoice = findChoiceAnchorForBadBranch(byId, current.getId());
                if (anchorChoice == null) {
                    anchorChoice = byId.get(inferStartStepId(byId));
                }
                progress.moveToStep(anchorChoice);
                progressRepository.save(progress);
                return new AdvanceResDto(AdvanceStatus.BAD_ENDING, mapStep(current), null);
            }

            // 중간 배드브랜치 → 다음 배드 스텝 그대로 진행(진행률 동결)
            ScenarioStep next = current.getNextStep() != null ? byId.get(current.getNextStep().getId()) : null;
            if (next == null) {
                ScenarioStep anchorChoice = findChoiceAnchorForBadBranch(byId, current.getId());
                if (anchorChoice == null) anchorChoice = byId.get(inferStartStepId(byId));
                progress.moveToStep(anchorChoice);
                progressRepository.save(progress);
                return new AdvanceResDto(AdvanceStatus.BAD_ENDING, mapStep(current), null);
            }

            progress.moveToStep(next); // 사용자의 진행 상황 저장
            progressRepository.save(progress);
            return new AdvanceResDto(AdvanceStatus.ADVANCED_FROZEN, mapStep(next), null);
        }

        // 퀴즈 처리
        Quiz quiz = current.getQuiz();
        if (quiz != null) {
            boolean isCorrect = answer != null && Objects.equals(quiz.getAnswer(), answer);
            if (!isCorrect) {
                // 미제출/오답 → 현재 스텝 유지 + 퀴즈 재노출
                progress.moveToStep(current);
                progressRepository.save(progress);
                AdvanceStatus status = (answer == null) ? AdvanceStatus.QUIZ_REQUIRED : AdvanceStatus.QUIZ_WRONG;
                return new AdvanceResDto(status, mapStep(current), mapQuiz(quiz));
            }
        }

        // 일반 next 진행
        ScenarioStep next = current.getNextStep() != null ? byId.get(current.getNextStep().getId()) : null;
        if (next == null) {
            // 정루트 마지막
            ensureCompletedOnce(user, scenario);
            double rate = monotonicRate(progress, 100.0);

            Long startId = inferStartStepId(byId);
            ScenarioStep start = byId.get(startId);
            if (start == null) {
                throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenarioId);
            }
            progress.moveToStep(start, rate); // 완료 후 재개는 시작 스텝
            progressRepository.save(progress);
            return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        }

        double computed = computeProgressRate(byId, scenarioId, next.getId());
        double rate = monotonicRate(progress, computed);
        progress.moveToStep(next, rate);
        progressRepository.save(progress);
        return new AdvanceResDto(AdvanceStatus.ADVANCED, mapStep(next), null);
    }

    /**
     * 체크포인트 저장
     *
     * - 배드브렌치에서는 사용자의 진행 상황 저장(진행률은 동결)
     * - 그 외에는 nowStepId 기준으로 진행률을 계산하여 최대값 유지
     */
    @Transactional
    public ProgressSaveResDto saveCheckpoint(Users user, Long scenarioId, Long nowStepId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        Map<Long, ScenarioStep> byId = preloadStepsAsMap(scenarioId);

        ScenarioStep step = byId.get(nowStepId);
        if (step == null) {
            throw new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않거나 시나리오와 불일치. stepId=" + nowStepId);
        }

        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgressList.builder()
                        .user(user).scenario(scenario).progressRate(0.0).build());

        // 배드 브랜치에서는 진행률 동결 (진행 상황 저장은 허용)
        if (isBadBranch(step)) {
            progress.moveToStep(step);
            progressRepository.save(progress);
            return new ProgressSaveResDto(scenarioId, step.getId(), progress.getProgressRate());
        }

        double computed = computeProgressRate(byId, scenarioId, nowStepId);
        double rate = monotonicRate(progress, computed);
        progress.moveToStep(step, rate);
        progressRepository.save(progress);

        return new ProgressSaveResDto(scenarioId, nowStepId, rate);
    }

    /**
     * 시나리오의 모든 스텝을 한 번에 로드하여 Map으로 반환
     * - 반복 호출을 피하기 위한 용도
     */
    private Map<Long, ScenarioStep> preloadStepsAsMap(Long scenarioId) {
        List<ScenarioStep> steps = stepRepository.findByScenarioIdWithNextStep(scenarioId);
        if (steps.isEmpty()) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "스텝이 비어있습니다. scenarioId=" + scenarioId);
        }
        Map<Long, ScenarioStep> byId = new LinkedHashMap<>();
        for (ScenarioStep s : steps) byId.putIfAbsent(s.getId(), s);
        return byId;
    }

    /**
     * 시작 스텝 추론
     * - 다른 스텝의 nextStep으로 참조되지 않은 스텝 중 가장 작은 id 선택
     */
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

    /** 이전 저장값과 비교하여 더 큰 값을 반환 */
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

    /** 진행률 계산(정루트 기준 체인) - 사전 로드된 byId 사용 */
    private double computeProgressRate(Map<Long, ScenarioStep> byId, Long scenarioId, Long nowStepId) {
        Long startId = inferStartStepId(byId);
        ScenarioStep start = byId.get(startId);
        if (start == null) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenarioId);
        }

        // 정루트(good=true) 기준 메인 체인 탐색
        Set<Long> visited = new HashSet<>();
        List<Long> mainChain = new ArrayList<>();
        ScenarioStep cur = start;

        while (cur != null && !visited.contains(cur.getId())) {
            visited.add(cur.getId());
            mainChain.add(cur.getId());

            Long nextId = null;
            if (cur.getType() == StepType.CHOICE) {
                try {
                    JsonNode root = objectMapper.readTree(cur.getContent());
                    JsonNode choices = findChoicesArray(root);
                    if (choices != null) {
                        for (JsonNode c : choices) {
                            if (c.path("good").asBoolean(false) && c.hasNonNull("next")) {
                                nextId = c.get("next").asLong();
                                break;
                            }
                        }
                    }
                } catch (JsonProcessingException ignored) {}
            }
            if (nextId == null && cur.getNextStep() != null) {
                nextId = cur.getNextStep().getId();
            }
            cur = (nextId != null) ? byId.get(nextId) : null;
        }

        // 메인 체인 상 진행률
        int total = Math.max(mainChain.size(), 1);
        int idx = mainChain.indexOf(nowStepId);
        if (idx >= 0) {
            double pct = ((idx + 1) * 100.0) / total;
            return Math.min(100.0, Math.max(0.0, pct));
        }

        // 메인 체인 외부(배드 브랜치 등)는 id 오름차순 기준 Fallback
        List<Long> ordered = new ArrayList<>(byId.keySet());
        Collections.sort(ordered);
        int pos = ordered.indexOf(nowStepId);
        if (pos < 0) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "현재 스텝 ID가 시나리오의 스텝 목록에 존재하지 않습니다. stepId=" + nowStepId);
        }
        double pct = ((pos + 1) * 100.0) / Math.max(ordered.size(), 1);
        return Math.min(100.0, Math.max(0.0, pct));
    }

    /** 엔드포인트 응답을 위한 스텝 매핑(JSON 파싱 실패 시 서버 오류 반환). */
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

    /** 퀴즈 엔티티 → DTO 변환(JSON 옵션 파싱 실패 시 서버 오류). */
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

    /** CHOICE 파싱 결과 레코드(정/오 분기 및 next 스텝 ID). */
    private record ChoiceInfo(boolean good, Long nextStepId) {}

    /** 다양한 위치/형태의 choices 배열을 찾아 반환 */
    private JsonNode findChoicesArray(JsonNode root) {
        if (root == null) return null;

        // Object 루트: {"choices":[...]}
        if (root.isObject()) {
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray()) return choices;
        }

        // Array 루트:
        if (root.isArray()) {
            // ["choices", [ ... ]] 패턴
            for (int i = 0; i < root.size() - 1; i++) {
                JsonNode cur = root.get(i);
                JsonNode nxt = root.get(i + 1);
                if (cur.isTextual()
                        && "choices".equalsIgnoreCase(cur.asText())
                        && nxt != null && nxt.isArray()) {
                    return nxt;
                }
            }
            // 배열 요소 중 {"choices":[...]}가 포함된 오브젝트
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

    /** 배드브랜치 여부 판단: content.meta.branch == "bad" */
    private boolean isBadBranch(ScenarioStep step) {
        try {
            JsonNode root = objectMapper.readTree(step.getContent());
            JsonNode meta = (root.isArray() && root.size() > 0) ? root.get(0).get("meta") : root.get("meta");
            return meta != null && "bad".equalsIgnoreCase(meta.path("branch").asText(null));
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /** 배드엔딩 여부 판단: content.meta.badEnding == true */
    private boolean isBadEnding(ScenarioStep step) {
        try {
            JsonNode root = objectMapper.readTree(step.getContent());
            JsonNode meta = (root.isArray() && root.size() > 0) ? root.get(0).get("meta") : root.get("meta");
            return meta != null && meta.path("badEnding").asBoolean(false);
        } catch (JsonProcessingException e) {
            return false;
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
                    if (good) continue;
                    if (!c.hasNonNull("next")) continue;

                    long startId = c.get("next").asLong();

                    // bad 경로 체인을 따라가며 badStepId 도달 여부 확인
                    Set<Long> visited = new HashSet<>();
                    ScenarioStep cur = byId.get(startId);
                    while (cur != null && !visited.contains(cur.getId())) {
                        if (Objects.equals(cur.getId(), badStepId)) {
                            return s; // 이 CHOICE가 앵커
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
