package dev.woori.wooriLearn.domain.scenario.service.processor;

import dev.woori.wooriLearn.domain.scenario.content.StepMeta;

import java.util.Optional;

/**
 * 스텝 content(JSON)에서 한 번의 파싱으로 얻을 수 있는 정보 묶음
 */
public record ContentInfo(
        Optional<StepMeta> meta,
        boolean hasChoices
) {}
