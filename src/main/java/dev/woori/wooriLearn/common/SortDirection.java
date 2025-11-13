package dev.woori.wooriLearn.common;

import org.springframework.data.domain.Sort;

public enum SortDirection {
    ASC, DESC;

    public Sort toSort(String... properties) {
        return this == ASC ? Sort.by(properties).ascending() : Sort.by(properties).descending();
    }
}

