package com.oracle.fundsapp.entities;
import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "fund")
@Getter
@Setter
@NoArgsConstructor
public class Fund {

    @Id
    @Column(name = "id", nullable = false)
    private int  Id; // Primary key

    @Column(name = "user_id", nullable = false)
    private int  userId; // Foreign key

    @Column(name = "fund_balance", nullable = false, precision = 15, scale = 2)
    private BigDecimal fundBalance;
}
