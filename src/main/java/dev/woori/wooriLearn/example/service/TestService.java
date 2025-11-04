package dev.woori.wooriLearn.example.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.example.dto.TestDto;
import dev.woori.wooriLearn.example.entity.TestEntity;
import dev.woori.wooriLearn.example.repository.TestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class TestService {

    private final TestRepository testRepository;

    public Long create(TestDto req) {
        TestEntity entity = TestEntity.builder()
                .title(req.title())
                .content(req.content())
                .build();

        TestEntity saved = testRepository.save(entity);
        return saved.getId();
    }

    public TestDto find(long id){
        TestEntity entity = testRepository.findById(id).orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND));
        return TestDto.builder()
                .title(entity.getTitle())
                .content(entity.getContent())
                .build();
    }
}
