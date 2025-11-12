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

        ScenarioStep current = stepRepository.findById(nowStepId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않습니다. id=" + nowStepId));
        if (!Objects.equals(current.getScenario().getId(), scenario.getId())) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "스텝이 해당 시나리오에 속하지 않습니다.");
        }

        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElse(ScenarioProgressList.builder()
                        .user(user)
                        .scenario(scenario)
                        .step(current)
                        .progressRate(0.0)
                        .build());

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

            progressRepository.deleteByUserAndScenario(user, scenario);
            return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        }

        // 진행률 계산 + 이동 저장
        double rate = computeProgressRate(scenarioId, next.getId());
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
        ScenarioStep step = stepRepository.findById(nowStepId)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "스텝이 존재하지 않습니다. id=" + nowStepId));

        if (!Objects.equals(step.getScenario().getId(), scenario.getId())) {
            throw new CommonException(ErrorCode.INVALID_REQUEST, "스텝이 시나리오와 일치하지 않습니다. stepId=" +
                    step.getId() + ", scenarioId=" + scenario.getId());
        }

        // 이미 완료했는지 확인
        boolean alreadyCompleted = completedRepository.existsByUserAndScenario(user, scenario);

        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgressList.builder()
                        .user(user)
                        .scenario(scenario)
                        .progressRate(0.0)
                        .build());

        // 시나리오를 완료한 사용자는 100.0 고정, 미완료자는 계산
        double rate = alreadyCompleted ? 100.0 : computeProgressRate(scenarioId, nowStepId);

        // 스텝 이동 + 진행률 갱신
        progress.moveToStep(step, rate);
        progressRepository.save(progress);

        return new ProgressSaveResDto(scenarioId, nowStepId, rate);
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
        Set<Long> nextIds = new HashSet<>();
        for (ScenarioStep s : steps) {
            if (s.getNextStep() != null) {
                nextIds.add(s.getNextStep().getId());
            }
        }
        ScenarioStep start = steps.stream()
                .filter(s -> !nextIds.contains(s.getId()))
                .min(Comparator.comparingLong(ScenarioStep::getId)) // 동률시 id가 작은 것
                .orElseGet(() ->
                        // 모든 스텝이 참조된 경우(비정상 체인)에는 최소 id 폴백
                        steps.stream().min(Comparator.comparingLong(ScenarioStep::getId))
                                .orElseThrow(() -> new CommonException(
                                        ErrorCode.INTERNAL_SERVER_ERROR, "시작 스텝을 계산할 수 없습니다. scenarioId=" + scenarioId))
                );

        // 3) start부터 체인 순회(루프 방지)하여 nowStepId 위치 계산
        Set<Long> visited = new HashSet<>();
        ScenarioStep cur = start;
        int total = 0;
        Integer foundIdx = null;

        while (cur != null && !visited.contains(cur.getId())) {
            visited.add(cur.getId());
            if (cur.getId().equals(nowStepId)) {
                foundIdx = total; // 0-based
            }
            total++;
            cur = (cur.getNextStep() != null) ? byId.get(cur.getNextStep().getId()) : null;
        }

        // 4) 체인에 포함되지 않은 경우: id 오름차순 대체 계산
        if (foundIdx == null) {
            List<Long> ordered = byId.keySet().stream().sorted().toList();
            int pos = ordered.indexOf(nowStepId);
            if (pos < 0) pos = 0;
            total = ordered.size();
            foundIdx = pos;
        }

        // 5) 퍼센트 산출 및 0~100 범위
        double pct = ((foundIdx + 1) * 100.0) / Math.max(total, 1);
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
}

