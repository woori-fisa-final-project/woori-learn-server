package dev.woori.wooriLearn.domain.edubankapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;

@Entity
@Table(name = "transaction_history")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Getter
public class TransactionHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private EducationalAccount account;

    @Column(name = "transaction_date", updatable = false, nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime transactionDate;

    @Column(name = "counterparty_name", nullable = false, length = 30)
    private String counterpartyName;

    @Column(name = "display_name", length = 30)
    private String displayName;

    @Column(nullable = false)
    private Integer amount;

    @Column(length = 50)
    private String description;

}

