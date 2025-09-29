package com.ebingo.backend.payment.entity;

import com.ebingo.backend.payment.enums.TransactionStatus;
import com.ebingo.backend.payment.enums.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("transaction")
public class Transaction {

    @Id
    private Long id;

    @Column("user_profile_id")
    private Long userProfileId;

    @Column("transfer_to")
    private Long transferTo;

    @Column("payment_method_id")
    private Long paymentMethodId;

    @Column("txn_type")
    private TransactionType txnType;

    @Column("txn_amount")
    private BigDecimal txnAmount;

    private TransactionStatus status;

    private String description;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;
}

