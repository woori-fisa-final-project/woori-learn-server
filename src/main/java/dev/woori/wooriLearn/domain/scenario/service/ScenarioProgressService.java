package dev.woori.wooriLearn.domain.scenario.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import dev.woori.wooriLearn.domain.scenario.dto.AdvanceResDto;
import dev.woori.wooriLearn.domain.scenario.dto.ProgressResumeResDto;
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

import java.util.List;
import java.util.Objects;

/**
 * 시나리오 진행/재개/완료 로직을 담당하는 서비스
 *
 * 주요기능
 *  - resume: 저장된 진행 위치가 있으면 그 스텝에서 재개, 없으면 시작 스텝 반환
 *  - advance: 다음 스텝 진행. 퀴즈가 있다면 정답 제출/검증 흐름 포함
 *  - saveCheckpoint: 현재 스텝을 임의로 저장(업서트)
 */
@Service
@RequiredArgsConstructor
public class ScenarioProgressService {

    private final ScenarioRepository scenarioRepository;
    private final ScenarioStepRepository stepRepository;
    private final ScenarioProgressListRepository progressRepository;
    private final ScenarioCompletedRepository completedRepository; // 완료 처리용
    private final ObjectMapper objectMapper;

    /**
     * 저장된 위치가 있으면 해당 위치에서 재개, 없으면 시작 스텝을 찾아 반환
     *
     * @param user          사용자
     * @param scenarioId    시나리오 ID
     * @return              현재 보여줄 스텝 정보
     */
    @Transactional(readOnly = true)
    public ProgressResumeResDto resume(Users user, Long scenarioId) {
        // 시나리오 유효성 검사
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다. id=" + scenarioId));

        // 진행 이력 조회 -> 없으면 시작 스텝 계산(루트) -> 그래도 없으면 최소 id 스텝
        ScenarioStep step = progressRepository.findByUserAndScenario(user, scenario)
                .map(ScenarioProgressList::getStep)
                .orElseGet(() ->
                        stepRepository.findStartStep(scenarioId)
                                .orElseGet(() -> stepRepository.findFirstByScenarioIdOrderByIdAsc(scenarioId)
                                        .orElseThrow(() -> new IllegalStateException("시작 스텝을 찾을 수 없습니다.")))
                );

        return mapStep(step);
    }

    /**
     * 다음으로 진행(퀴즈 게이트 포함)
     *
     * 동작 규칙
     *  - 현재 스텝에 퀴즈가 없으면 즉시 next로 이동
     *  - 현재 스텝에 퀴즈가 있으면
     *    - answer 미제출 -> QUIZ_REQUIRED + quiz 반환
     *    - 오답 -> QUIZ_WRONG + quiz 반환
     *    - 정답 -> next로 이동
     * - next 없음 -> COMPLETED (진행기록 삭제 & 완료 업서트)
     *
     * @param user          사용자
     * @param scenarioId    시나리오 ID
     * @param nowStepId     현재 스텝 ID
     * @param answer        정답 인덱스. 퀴즈 없으면 null
     * @return              진행 결과(다음 스텝/퀴즈/상태)
     */
    @Transactional
    public AdvanceResDto advance(Users user, Long scenarioId, Long nowStepId, Integer answer) {
        // 0) 시나리오/스텝 유효성
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다. id=" + scenarioId));

        ScenarioStep current = stepRepository.findById(nowStepId)
                .orElseThrow(() -> new IllegalArgumentException("스텝이 존재하지 않습니다. id=" + nowStepId));
        if (!Objects.equals(current.getScenario().getId(), scenario.getId())) {
            throw new IllegalArgumentException("스텝이 해당 시나리오에 속하지 않습니다.");
        }

        // 1) 진행 레코드 업서트(최소 현재 스텝까지는 기록)
        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElse(ScenarioProgressList.builder()
                        .user(user)
                        .scenario(scenario)
                        .step(current)
                        .progressRate(0.0)
                        .build());

        // 2) 퀴즈 처리(있을 때만)
        Quiz quiz = current.getQuiz();
        if (quiz != null) {
            boolean isCorrect = answer != null && Objects.equals(quiz.getAnswer(), answer);
            if (!isCorrect) {
                // 2-1) 정답이 아니거나 답을 제출하지 않은 경우 -> 퀴즈 다시/먼저 노출
                progress.moveToStep(current); // 위치 유지
                progressRepository.save(progress);
                AdvanceStatus status = (answer == null) ? AdvanceStatus.QUIZ_REQUIRED : AdvanceStatus.QUIZ_WRONG;
                return new AdvanceResDto(status, mapStep(current), mapQuiz(quiz));
            }
            // 2-2) 정답 -> 아래에서 next로 이동
        }

        // 3) 다음 스텝으로 이동
        ScenarioStep next = current.getNextStep();
        if (next == null) {
            // 마지막 스텝 -> 완료 처리
            completedRepository.findByUserAndScenario(user, scenario)
                    .orElseGet(() -> completedRepository.save(
                            ScenarioCompleted.builder().user(user).scenario(scenario).build()
                    ));

            // 진행 기록 정리(완료 시 삭제)
            progressRepository.deleteByUserAndScenario(user, scenario);
            return new AdvanceResDto(AdvanceStatus.COMPLETED, null, null);
        }

        // 4) 진행 위치 업데이트
        progress.moveToStep(next);
        progressRepository.save(progress);

        return new AdvanceResDto(AdvanceStatus.ADVANCED, mapStep(next), null);
    }

    /**
     * 진행 중인 스텝 저장
     * @param user          사용자
     * @param scenarioId    시나리오 ID
     * @param nowStepId     현재 스텝 ID
     */
    @Transactional
    public void saveCheckpoint(Users user, Long scenarioId, Long nowStepId) {
        // 시나리오/스텝 유효성
        Scenario scenario = scenarioRepository.findById(scenarioId)
                .orElseThrow(() -> new IllegalArgumentException("시나리오가 존재하지 않습니다. id=" + scenarioId));
        ScenarioStep step = stepRepository.findById(nowStepId)
                .orElseThrow(() -> new IllegalArgumentException("스텝이 존재하지 않습니다. id=" + nowStepId));

        if (!Objects.equals(step.getScenario().getId(), scenario.getId())) {
            throw new IllegalArgumentException("스텝이 시나리오에 속하지 않습니다. stepId=" +
                    step.getId() + ", scenarioId=" + scenario.getId());
        }

        // 업서트
        ScenarioProgressList progress = progressRepository.findByUserAndScenario(user, scenario)
                .orElseGet(() -> ScenarioProgressList.builder()
                        .user(user)
                        .scenario(scenario)
                        .progressRate(0.0)
                        .build());

        progress.moveToStep(step);
        progressRepository.save(progress);
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
        } catch (Exception e) {
            throw new IllegalStateException("스텝 content JSON 파싱 실패. stepId=" + step.getId(), e);
        }
    }

    private QuizResDto mapQuiz(Quiz quiz) {
        try {
            var listType = TypeFactory.defaultInstance()
                    .constructCollectionType(List.class, String.class);
            List<String> opts = objectMapper.readValue(quiz.getOptions(), listType);
            return new QuizResDto(quiz.getId(), quiz.getQuestion(), opts);
        } catch (Exception e) {
            throw new IllegalStateException("퀴즈 options 파싱 실패. quizId=" + quiz.getId(), e);
        }
    }
}
