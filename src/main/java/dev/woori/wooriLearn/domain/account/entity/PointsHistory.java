package dev.woori.wooriLearn.domain.account.entity;

import dev.woori.wooriLearn.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "points_history")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(org.springframework.data.jpa.domain.support.AuditingEntityListener.class)
public class PointsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    // 적립/출금 공통 금액
    @Column(name = "amount", nullable = false)
    private Integer amount;

    // DEPOSIT or WITHDRAW
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PointsHistoryType type;

    // 처리 상태 (적립은 항상 SUCCESS)
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PointsStatus status;

    // 환전일 때만 사용
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // 환전 실패 사유 (적립에는 null)
    @Column(name = "fail_reason")
    private String failReason;

    public void markSuccess(LocalDateTime processedAt) {
        this.status = PointsStatus.SUCCESS;
        this.processedAt = processedAt;
    }


    public void markFailed(String reason, LocalDateTime processedAt) {
        this.status = PointsStatus.FAILED;
        this.failReason = reason;
        this.processedAt = processedAt;
    }

}
