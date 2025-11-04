package dev.woori.wooriLearn.domain.edubankapi.entity;

import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "auto_payment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AutoPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "withdrawal_account_id", nullable = false)
    private EducationalAccount withdrawalAccount;

    @Column(name = "deposit_number", nullable = false, length = 20)
    private String depositNumber;

    @Column(name = "deposit_bank_code", nullable = false, length = 10)
    private String depositBankCode;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "counterparty_name", nullable = false, length = 30)
    private String counterpartyName;

    @Column(name = "display_name", length = 30)
    private String displayName;

    @Column(name = "transfer_cycle", nullable = false)
    private Integer transferCycle;

    @Column(name = "designated_date", nullable = false)
    private Integer designatedDate;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "expiration_date", nullable = false)
    private LocalDate expirationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false)
    private AutoPaymentStatus processingStatus;

    public enum AutoPaymentStatus {
        ACTIVE, CANCELLED
    }
}
