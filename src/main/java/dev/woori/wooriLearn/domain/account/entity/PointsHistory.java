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
public class PointsHistory extends BaseEntity {  // ✅ 추가됨

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

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
