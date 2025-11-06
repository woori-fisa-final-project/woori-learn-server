package dev.woori.wooriLearn.domain.edubankapi.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

@Entity
@Table(name = "auto_payment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class AutoPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "educational_account_id", nullable = false)
    private EducationalAccount educationalAccount;

    @Column(name = "counterparty_name", nullable = false, length = 100)
    private String counterpartyName;

    @Column(name = "counterparty_account", nullable = false, length = 20)
    private String counterpartyAccount;

    @Column(nullable = false)
    private Integer amount;

    @Column(name = "transfer_day", nullable = false)
    private Integer transferDay;

    @Column(name = "display_name", nullable = false, length = 50)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_status", nullable = false, length = 20)
    @Builder.Default
    private AutoPaymentStatus processingStatus = AutoPaymentStatus.ACTIVE;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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