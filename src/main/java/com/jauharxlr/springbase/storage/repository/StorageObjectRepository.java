package com.jauharxlr.springbase.storage.repository;

import com.jauharxlr.springbase.storage.entity.StorageObject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StorageObjectRepository extends JpaRepository<StorageObject, UUID> {
    List<StorageObject> findByBucketNameAndProjectRef(String bucketName, String projectRef);
    Optional<StorageObject> findByNameAndBucketNameAndProjectRef(String name, String bucketName, String projectRef);
}
