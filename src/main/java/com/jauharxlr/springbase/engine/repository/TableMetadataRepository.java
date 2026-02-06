package com.jauharxlr.springbase.engine.repository;

import com.jauharxlr.springbase.engine.entity.TableMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TableMetadataRepository extends JpaRepository<TableMetadata, UUID> {
    Optional<TableMetadata> findByTableNameAndProjectRef(String tableName, String projectRef);
}
