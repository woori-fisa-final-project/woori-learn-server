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
public class PointsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @CreatedDate
    @Column(name = "payment_date", updatable = false, nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime paymentDate;

    @Column(name = "payment_amount", nullable = false)
    private Integer paymentAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PointsExchangeStatus status;
    @PrePersist
    protected void onCreate() {
        this.paymentDate = LocalDateTime.now();
    }
}
