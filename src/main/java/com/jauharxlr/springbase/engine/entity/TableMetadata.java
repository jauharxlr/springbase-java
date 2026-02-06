package com.jauharxlr.springbase.engine.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "sb_table_metadata")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String tableName;

    @Column(nullable = false)
    private String projectRef;

    @Column(nullable = false)
    private boolean isPublicRead;
}
