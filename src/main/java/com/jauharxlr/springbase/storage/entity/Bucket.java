package com.jauharxlr.springbase.storage.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sb_buckets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bucket {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String projectRef;

    @Column(nullable = false)
    private boolean isPublic;

    @Column(nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
