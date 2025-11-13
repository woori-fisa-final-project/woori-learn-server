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
import dev.woori.wooriLearn.domain.scenario.entity.Quiz;
import dev.woori.wooriLearn.domain.scenario.entity.Scenario;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioCompleted;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioProgressList;
import dev.woori.wooriLearn.domain.scenario.entity.ScenarioStep;
import dev.woori.wooriLearn.domain.scenario.model.AdvanceStatus;
import dev.woori.wooriLearn.domain.scenario.model.StepType;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioCompletedRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioProgressListRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioRepository;
import dev.woori.wooriLearn.domain.scenario.repository.ScenarioStepRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 시나리오 진행(재개/다음 스텝/체크포인트 저장) 비즈니스 로직을 담당하는 서비스
 *
 * 주요기능
 * 사용자-시나리오 기준 현재 스텝 조회(재개)
 * 다음 스텝으로 진행(퀴즈 게이트: 미제출/오답/정답 처리)
 * 체크포인트(현재 스텝) 저장 및 진행률 계산
 * 완료 처리 및 진행 이력 제거
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
     * 시나리오 진행 재개: 저장된 이력이 있으면 해당 스텝, 없으면 시작 스텝 반환
     * @param user          사용자
     * @param scenarioId    시나리오 ID
     * @return              현재 렌더링해야 할 스텝 정보 DTO
     */
    @Transactional(readOnly = true)
    public ProgressResumeResDto resume(Users user, Long scenarioId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        // 진행 이력이 있으면 해당 스텝, 없으면 시작 스텝
        ScenarioStep step = progressRepository.findByUserAndScenario(user, scenario)
                .map(ScenarioProgressList::getStep)
                .orElseGet(() -> stepRepository.findStartStepOrFail(scenarioId));

        return mapStep(step);
    }

    /**
     * 다음 스텝으로 진행(퀴즈 게이트 포함)
     * 퀴즈 있고 정답 미제출 - QUIZ_REQUIRED
     * 퀴즈 있고 오답 제출 - QUIZ_WORNG
     * 정답 또는 퀴즈 없음 - nextStep으로 이동
     * 마지막 스텝 - COMPLETED + 진행 이력 삭제
     *
     * @param user          사용자
     * @param scenarioId    시나리오 ID
     * @param nowStepId     현재 스텝 ID
     * @param answer        (퀴즈 스텝에서) 사용자가 제출한 인덱스
     * @return              진행 결과/다음스텝/퀴즈 정보 DTO
     */
    @Transactional
    public AdvanceResDto advance(Users user, Long scenarioId, Long nowStepId, Integer answer) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));

        ScenarioStep current = stepRepository.findByIdAndScenarioId(nowStepId, scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않거나 시나리오와 불일치. stepId=" + nowStepId));

        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgressList.builder()
                        .user(user).scenario(scenario).step(current).progressRate(0.0).build());

        if (current.getType() == StepType.CHOICE) {
            if (answer == null) {
                // 선택 전: 현재 스텝 유지
                progress.moveToStep(current);
                progressRepository.save(progress);
                return new AdvanceResDto(AdvanceStatus.CHOICE_REQUIRED, mapStep(current), null);
            }
            var choice = parseChoice(current, answer); // {good, nextStepId}

            if (!choice.good()) {
                // 잘못된 선택 → 잘못된 경로로 진입
                Long nextId = choice.nextStepId();
                if (nextId == null) {
                    progress.moveToStep(current);
                    progressRepository.save(progress);
                    return new AdvanceResDto(AdvanceStatus.BAD_ENDING, mapStep(current), null);
                }
                ScenarioStep nextWrong = stepRepository.findByIdAndScenarioId(nextId, scenarioId)
                        .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "잘못된 경로 next가 존재하지 않습니다. id=" + nextId));

                // 진행 동결: progress는 CHOICE 스텝으로 유지(업데이트/진행률 증가 없음)
                progress.moveToStep(nextWrong);
                progressRepository.save(progress);

                return new AdvanceResDto(AdvanceStatus.ADVANCED_FROZEN, mapStep(nextWrong), null);
            }

            // 정루트 선택 → next로 정상 진행
            Long nextId = (choice.nextStepId() != null)
                    ? choice.nextStepId()
                    : (current.getNextStep() != null ? current.getNextStep().getId() : null);

            if (nextId == null) {
                // 정루트인데 다음이 없으면 완료
                if (!completedRepository.existsByUserAndScenario(user, scenario)) {
                    completedRepository.save(ScenarioCompleted.builder().user(user).scenario(scenario).build());
                }
                double rate = monotonicRate(progress, 100.0);
                progress.moveToStep(current, rate); // 마지막 스텝 + 100% 기록
                progressRepository.save(progress);
                return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
            }

            ScenarioStep next = stepRepository.findByIdAndScenarioId(nextId, scenarioId)
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "다음 스텝이 존재하지 않습니다. id=" + nextId));

            double computed = computeProgressRate(scenarioId, next.getId());
            double rate = monotonicRate(progress, computed);
            progress.moveToStep(next, rate);
            progressRepository.save(progress);
            return new AdvanceResDto(AdvanceStatus.ADVANCED, mapStep(next), null);
        }

        if (isBadBranch(current)) {
            if (isBadEnding(current)) {
                // 배드엔딩 도달: progress는 여전히 CHOICE 스텝(동결 유지)
                ScenarioStep anchorChoice = findChoiceAnchorForBadBranch(scenarioId, current.getId());
                if (anchorChoice == null) {
                    // 앵커를 못 찾았으면 시작 스텝으로 복귀(보수적)
                    anchorChoice = stepRepository.findStartStepOrFail(scenarioId);
                }
                progress.moveToStep(anchorChoice);     // 재개 시 선택지로 복귀
                progressRepository.save(progress);
                return new AdvanceResDto(AdvanceStatus.BAD_ENDING, mapStep(current), null);
            }
            // 중간 스텝이면 다음 잘못된 스텝으로 계속 진행 (동결)
            ScenarioStep next = current.getNextStep();
            if (next == null) {
                ScenarioStep anchorChoice = findChoiceAnchorForBadBranch(scenarioId, current.getId());
                if (anchorChoice == null) anchorChoice = stepRepository.findStartStepOrFail(scenarioId);
                progress.moveToStep(anchorChoice);
                progressRepository.save(progress);
                return new AdvanceResDto(AdvanceStatus.BAD_ENDING, mapStep(current), null);
            }
            ScenarioStep nextWrong = stepRepository.findByIdAndScenarioId(next.getId(), scenarioId)
                    .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "잘못된 경로 next가 존재하지 않습니다. id=" + next.getId()));

            // progress 변경/진행률 증가 없음
            progress.moveToStep(nextWrong);           // 마지막으로 본 화면 스텝 저장
            progressRepository.save(progress);
            return new AdvanceResDto(AdvanceStatus.ADVANCED_FROZEN, mapStep(nextWrong), null);
        }

        // 1) 퀴즈 게이트
        Quiz quiz = current.getQuiz();
        if (quiz != null) {
            boolean isCorrect = answer != null && Objects.equals(quiz.getAnswer(), answer);
            if (!isCorrect) {
                // 미제출/오답 -> 현재 스텝 유지 + 상태 반환
                progress.moveToStep(current);
                progressRepository.save(progress);
                AdvanceStatus status = (answer == null) ? AdvanceStatus.QUIZ_REQUIRED : AdvanceStatus.QUIZ_WRONG;
                return new AdvanceResDto(status, mapStep(current), mapQuiz(quiz));
            }
        }

        // 2) 다음 스텝 이동 or 완료 처리
        ScenarioStep next = current.getNextStep();
        if (next == null) {
            // 마지막 스텝 -> 완료 기록 후 진행 이력 삭제
            if (!completedRepository.existsByUserAndScenario(user, scenario)) {
                completedRepository.save(
                        ScenarioCompleted.builder().user(user).scenario(scenario).build()
                );
            }
            double rate = monotonicRate(progress, 100.0);
            progress.moveToStep(current, rate); // 마지막 스텝 + 100% 유지
            progressRepository.save(progress);
            return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        }

        // 진행률 계산 + 이동 저장
        double computed = computeProgressRate(scenarioId, next.getId());
        double rate = monotonicRate(progress, computed);
        progress.moveToStep(next, rate);
        progressRepository.save(progress);
        return new AdvanceResDto(AdvanceStatus.ADVANCED, mapStep(next), null);
    }

    /**
     * 현재 스텝(체크포인트) 저장 및 진행률 갱신
     * - 이미 완료한 사용자라면 진행률 100.0으로 고정
     * - 미완료 사용자는 현재 스텝 기준으로 계산
     *
     * @param user          사용자
     * @param scenarioId    시나리오 ID
     * @param nowStepId     현재 스텝 ID
     * @return              저장 결과 DTO
     */
    @Transactional
    public ProgressSaveResDto saveCheckpoint(Users user, Long scenarioId, Long nowStepId) {
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "시나리오가 존재하지 않습니다. id=" + scenarioId));
        ScenarioStep step = stepRepository.findByIdAndScenarioId(nowStepId, scenarioId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않거나 시나리오와 불일치. stepId=" + nowStepId));
        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgressList.builder()
                        .user(user).scenario(scenario).progressRate(0.0).build());

        // 잘못된 경로에서는 진행률/스텝 업데이트 동결
        if (isBadBranch(step)) {
            progress.moveToStep(step);
            progressRepository.save(progress);
            return new ProgressSaveResDto(scenarioId, step.getId(), progress.getProgressRate());
        }

        double computed = computeProgressRate(scenarioId, nowStepId);
        double rate = monotonicRate(progress, computed);
        progress.moveToStep(step, rate);
        progressRepository.save(progress);

        return new ProgressSaveResDto(scenarioId, nowStepId, rate);
    }

    private double monotonicRate(ScenarioProgressList progress, double candidate) {
        double prev = (progress.getProgressRate() == null) ? 0.0 : progress.getProgressRate();
        double bounded = Math.min(100.0, Math.max(0.0, candidate));
        return Math.max(prev, bounded);
    }

    /**
     * 진행률 계산(0.0 ~ 100.0)
     * - 'findByScenarioIdWithNextStep'로 모든 스텝(+nextStep)을 한 번에 로딩(N+1 방지)
     * - 다른 스텝의 nextStep으로 참조되지 않은 스텝을 시작 스텝으로 간주
     * - start부터 nextStep 체인을 따라가며 총 길이와 현재 스텝의 인덱스(0-based)를 계산
     * - 진행률 = (현재 인덱스 + 1) / 총 스텝 수 * 100, 결과는 0 ~ 100 범위
     *
     * @param scenarioId    시나리오 ID
     * @param nowStepId     현재 스텝 ID
     * @return              진행률 퍼센트
     */
    private double computeProgressRate(Long scenarioId, Long nowStepId) {
        // 1) nextStep JOIN FETCH로 일괄 로드
        List<ScenarioStep> steps = stepRepository.findByScenarioIdWithNextStep(scenarioId);
        if (steps.isEmpty()) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "스텝이 비어있습니다. scenarioId=" + scenarioId);
        }

        // id -> step 맵 (LinkedHashMap으로 순서 보존)
        Map<Long, ScenarioStep> byId = new LinkedHashMap<>();
        for (ScenarioStep s : steps) {
            byId.putIfAbsent(s.getId(), s);
        }

        // 2) 시작 스텝 추론: 어떤 스텝의 nextStep으로도 참조되지 않은 스텝
        Long startStepId = stepRepository.findStartStepOrFail(scenarioId).getId();
        ScenarioStep start = byId.get(startStepId);

        if (start == null) {
            // 로드된 스텝 목록에 시작 스텝이 없는 예외적인 상황
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenarioId);
        }

        // 3)
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
                    if (root.isArray() && root.size() > 0) root = root.get(0);
                    JsonNode choices = root.get("choices");
                    if (choices != null && choices.isArray()) {
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
            cur = nextId != null ? byId.get(nextId) : null;
        }

        // nowStepId가 정루트에 있으면 그 기준으로, 아니면 id 오름차순 fallback
        int total = Math.max(mainChain.size(), 1);
        int idx = mainChain.indexOf(nowStepId);
        if (idx >= 0) {
            double pct = ((idx + 1) * 100.0) / total;
            return Math.min(100.0, Math.max(0.0, pct));
        }

        // fallback (정루트 바깥 스텝: 예를 들어 배드 브랜치 등)
        List<Long> ordered = byId.keySet().stream().sorted().toList();
        int pos = ordered.indexOf(nowStepId);
        if (pos < 0) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "현재 스텝 ID가 시나리오의 스텝 목록에 존재하지 않습니다. stepId=" + nowStepId);
        }
        double pct = ((pos + 1) * 100.0) / Math.max(ordered.size(), 1);
        return Math.min(100.0, Math.max(0.0, pct));
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

    private ChoiceInfo parseChoice(ScenarioStep step, int answerIndex) {
        JsonNode root;
        try {
            root = objectMapper.readTree(step.getContent());
        } catch (JsonProcessingException e) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "CHOICE content 파싱 실패. stepId=" + step.getId());
        }
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray()) {
            throw new CommonException(ErrorCode.INTERNAL_SERVER_ERROR, "CHOICE 스텝에 choices 배열이 없습니다. stepId=" + step.getId());
        }
        if (answerIndex < 0 || answerIndex >= choices.size()) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "선택 인덱스 범위 초과. index=" + answerIndex);
        }
        JsonNode choice = choices.get(answerIndex);
        boolean good = choice.has("good") && choice.get("good").asBoolean(false);
        Long next = (choice.has("next") && !choice.get("next").isNull())
                ? choice.get("next").asLong()
                : null;
        return new ChoiceInfo(good, next);
    }

    /** 잘못된 경로 여부: content.meta.branch == "bad" */
    private boolean isBadBranch(ScenarioStep step) {
        try {
            JsonNode root = objectMapper.readTree(step.getContent());
            if (root.isArray() && root.size() > 0) root = root.get(0);
            JsonNode meta = root.get("meta");
            return meta != null && "bad".equalsIgnoreCase(meta.path("branch").asText(null));
        } catch (JsonProcessingException e) {
            // 파싱 실패는 잘못된 정의 → 안전하게 false
            return false;
        }
    }

    /** 배드엔딩 여부: content.meta.badEnding == true */
    private boolean isBadEnding(ScenarioStep step) {
        try {
            JsonNode root = objectMapper.readTree(step.getContent());
            if (root.isArray() && root.size() > 0) root = root.get(0);
            JsonNode meta = root.get("meta");
            return meta != null && meta.path("badEnding").asBoolean(false);
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private ScenarioStep findChoiceAnchorForBadBranch(Long scenarioId, Long badStepId) {
        List<ScenarioStep> steps = stepRepository.findByScenarioIdWithNextStep(scenarioId);
        if (steps.isEmpty()) return null;

        Map<Long, ScenarioStep> byId = new HashMap<>();
        for (ScenarioStep s : steps) byId.putIfAbsent(s.getId(), s);

        for (ScenarioStep s : steps) {
            if (s.getType() != StepType.CHOICE) continue;
            try {
                JsonNode root = objectMapper.readTree(s.getContent());
                if (root.isArray() && root.size() > 0) root = root.get(0);
                JsonNode choices = root.get("choices");
                if (choices == null || !choices.isArray()) continue;

                for (JsonNode c : choices) {
                    boolean good = c.path("good").asBoolean(false);
                    if (good) continue;
                    if (!c.hasNonNull("next")) continue;
                    long start = c.get("next").asLong();

                    // nextStep 체인 따라가며 badStepId 도달 여부 확인
                    Set<Long> visited = new HashSet<>();
                    ScenarioStep cur = byId.get(start);
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

