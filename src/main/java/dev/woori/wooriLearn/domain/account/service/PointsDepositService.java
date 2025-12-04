package dev.woori.wooriLearn.domain.account.service;

import dev.woori.wooriLearn.config.exception.CommonException;
import dev.woori.wooriLearn.config.exception.ErrorCode;
import dev.woori.wooriLearn.domain.account.dto.request.PointsDepositRequestDto;
import dev.woori.wooriLearn.domain.account.dto.response.PointsDepositResponseDto;
import dev.woori.wooriLearn.domain.account.entity.PointsHistory;
import dev.woori.wooriLearn.domain.account.entity.PointsHistoryType;
import dev.woori.wooriLearn.domain.account.entity.PointsStatus;
import dev.woori.wooriLearn.domain.account.repository.PointsHistoryRepository;
import dev.woori.wooriLearn.domain.user.entity.Users;
import dev.woori.wooriLearn.domain.user.repository.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.cache.CacheManager;

@Service
@RequiredArgsConstructor
public class PointsDepositService {

    private final UserRepository userRepository;
    private final PointsHistoryRepository pointsHistoryRepository;
    private final CacheManager cacheManager;
    private static final String DEFAULT_DEPOSIT_MESSAGE = "포인트 적립 완료";

    /**
     * 처리 순서
     * 1) 사용자 행 잠금 조회 (for update)로 동시성 이슈 방지
     * 2) 포인트 증액 (엔티티 검증 포함)
     * 3) 포인트 이력 저장 (DEPOSIT/SUCCESS)
     * 4) 응답 DTO 구성 후 반환
     */
    @Transactional
    public PointsDepositResponseDto depositPoints(String username, PointsDepositRequestDto dto) {
        // 1) 사용자 행 잠금 조회
        Users user = userRepository.findByUserIdForUpdate(username)
                .orElseThrow(() -> new CommonException(ErrorCode.ENTITY_NOT_FOUND, "사용자를 찾을 수 없습니다. userId=" + username));

        // 2) 포인트 증액
        user.addPoints(dto.amount());

        // 3) 포인트 이력 저장
        PointsHistory history = pointsHistoryRepository.save(
                PointsHistory.builder()
                        .user(user)
                        .amount(dto.amount())
                        .type(PointsHistoryType.DEPOSIT)
                        .status(PointsStatus.SUCCESS)
                        .build()
        );

        // Evict user info cache after points change
        var userInfoCache = cacheManager.getCache("userInfo");
        if (userInfoCache != null) userInfoCache.evict(user.getUserId());

        // 4) 응답 DTO 구성 및 반환
        return PointsDepositResponseDto.builder()
                .userId(user.getId())
                .addedPoint(dto.amount())
                .currentBalance(user.getPoints())
                .status(PointsStatus.SUCCESS)
                .message(dto.reason() != null ? dto.reason() : DEFAULT_DEPOSIT_MESSAGE)
                .createdAt(history.getCreatedAt())
                .build();
    }
}

