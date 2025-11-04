package dev.woori.wooriLearn.example.repository;

import dev.woori.wooriLearn.example.entity.TestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TestRepository extends JpaRepository<TestEntity, Long> {
    // optional 써서 or
    Optional<TestEntity> findById(Long id);
}
