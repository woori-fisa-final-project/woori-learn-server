package dev.woori.wooriLearn.domain.edubankapi.autopayment.entity;

import dev.woori.wooriLearn.domain.edubankapi.entity.EducationalAccount;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

@Entity
@Table(name = "auto_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AutoPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_account_id", nullable = false)
    private EducationalAccount educationalAccount;

    @Column(name = "deposit_number", nullable = false, length = 20)
    private String depositNumber;

    @Column(name = "deposit_bank_code", nullable = false, length = 10)
    private String depositBankCode;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "counterparty_name", nullable = false, length = 30)
    private String counterpartyName;

    @Column(name = "display_name", nullable = false, length = 30)
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
    @Column(name = "processing_status", nullable = false, length = 20)
    @Builder.Default
    private AutoPaymentStatus processingStatus = AutoPaymentStatus.ACTIVE;

    @Getter
    @RequiredArgsConstructor
    public enum AutoPaymentStatus {
        ACTIVE("활성"),
        CANCELLED("해지됨");

        private final String description;

        public static String getAvailableValues() {
            return Arrays.stream(values())
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
        }
    }

}
