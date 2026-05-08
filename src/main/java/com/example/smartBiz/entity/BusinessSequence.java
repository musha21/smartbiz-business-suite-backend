package com.example.smartBiz.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "business_sequences",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_business_sequence_type",
                        columnNames = {"business_id", "sequence_type"}
                )
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BusinessSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "business_id", nullable = false)
    private Long businessId;

    @Column(name = "sequence_type", nullable = false, length = 50)
    private String sequenceType;

    @Column(name = "sequence_last_value", nullable = false)
    private Long lastValue;
}