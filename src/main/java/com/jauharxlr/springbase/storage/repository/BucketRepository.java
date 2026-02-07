package com.jauharxlr.springbase.storage.repository;

import com.jauharxlr.springbase.storage.entity.Bucket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BucketRepository extends JpaRepository<Bucket, UUID> {
    List<Bucket> findByProjectRef(String projectRef);
    Optional<Bucket> findByNameAndProjectRef(String name, String projectRef);
}
