package dev.woori.wooriLearn.domain.account.entity;

import dev.woori.wooriLearn.domain.account.entity.PointsExchangeStatus;
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
    @Column(name = "payment_date", updatable = false, nullable = false)
    private LocalDateTime paymentDate;

    @Column(name = "payment_amount", nullable = false)
    private Integer paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PointsExchangeStatus status;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(name = "fail_reason")
    private String failReason;

    // ✅ Setter 필요한 것만 열기
    public void setStatus(PointsExchangeStatus status) {
        this.status = status;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public void setFailReason(String failReason) {
        this.failReason = failReason;
    }


}
