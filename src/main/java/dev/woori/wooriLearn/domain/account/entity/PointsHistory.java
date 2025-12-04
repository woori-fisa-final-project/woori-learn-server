package dev.woori.wooriLearn.domain.account.entity;

import dev.woori.wooriLearn.config.BaseEntity;
import dev.woori.wooriLearn.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "points_history")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PointsHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "account_num")
    private String accountNumber;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private PointsHistoryType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PointsStatus status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "fail_reason")
    private PointsFailReason failReason;

    public void markProcessing() {
        this.status = PointsStatus.PROCESSING;
    }

    public void markSuccess(LocalDateTime processedAt) {
        this.status = PointsStatus.SUCCESS;
        this.processedAt = processedAt;
    }

    public void markFailed(PointsFailReason reason, LocalDateTime processedAt) {
        this.status = PointsStatus.FAILED;
        this.failReason = reason;
        this.processedAt = processedAt;
    }
}
