package dev.woori.wooriLearn.domain.scenario.content;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 모든 스텝 공통으로 쓸 수 있는 메타 정보
 * - branch: "bad" 면 배드 브랜치
 * - badEnding: true 면 배드 엔딩
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StepMeta(
        String branch,
        Boolean badEnding
) {}
