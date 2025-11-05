package dev.woori.wooriLearn.domain.account.entity;

import dev.woori.wooriLearn.domain.user.entity.Users;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_request")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PointsExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "exchange_amount", nullable = false)
    private Integer exchangeAmount;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PointsExchangeStatus status;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @PrePersist
    protected void onCreate() {
        this.requestDate = LocalDateTime.now();
    }
}
