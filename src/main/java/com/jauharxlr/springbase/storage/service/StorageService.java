package com.jauharxlr.springbase.storage.service;

import com.jauharxlr.springbase.storage.entity.Bucket;
import com.jauharxlr.springbase.storage.entity.StorageObject;
import com.jauharxlr.springbase.storage.repository.BucketRepository;
import com.jauharxlr.springbase.storage.repository.StorageObjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final BucketRepository bucketRepository;
    private final StorageObjectRepository objectRepository;
    private final Path rootPath = Paths.get("storage-data");

    public Bucket createBucket(String name, boolean isPublic, String projectRef, UUID ownerId) {
        if (bucketRepository.findByNameAndProjectRef(name, projectRef).isPresent()) {
            throw new RuntimeException("Bucket already exists");
        }

        Bucket bucket = Bucket.builder()
                .name(name)
                .isPublic(isPublic)
                .projectRef(projectRef)
                .ownerId(ownerId)
                .createdAt(LocalDateTime.now())
                .build();

        return bucketRepository.save(bucket);
    }

    public List<Bucket> listBuckets(String projectRef) {
        return bucketRepository.findByProjectRef(projectRef);
    }

    public void deleteBucket(String name, String projectRef, UUID userId, String role) {
        Bucket bucket = bucketRepository.findByNameAndProjectRef(name, projectRef)
                .orElseThrow(() -> new RuntimeException("Bucket not found"));

        if (!role.equals("SERVICE_ROLE") && !bucket.getOwnerId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete bucket");
        }

        // Delete objects and files
        List<StorageObject> objects = objectRepository.findByBucketNameAndProjectRef(name, projectRef);
        for (StorageObject obj : objects) {
            deleteObject(name, obj.getName(), projectRef, userId, role);
        }

        bucketRepository.delete(bucket);
    }

    public StorageObject uploadObject(String bucketName, String name, MultipartFile file, String projectRef, UUID ownerId) throws IOException {
        Bucket bucket = bucketRepository.findByNameAndProjectRef(bucketName, projectRef)
                .orElseThrow(() -> new RuntimeException("Bucket not found"));

        Path bucketPath = rootPath.resolve(projectRef).resolve(bucketName);
        if (!Files.exists(bucketPath)) {
            Files.createDirectories(bucketPath);
        }

        Path filePath = bucketPath.resolve(name);
        Files.copy(file.getInputStream(), filePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        StorageObject obj = objectRepository.findByNameAndBucketNameAndProjectRef(name, bucketName, projectRef)
                .orElse(StorageObject.builder()
                        .name(name)
                        .bucketName(bucketName)
                        .projectRef(projectRef)
                        .ownerId(ownerId)
                        .createdAt(LocalDateTime.now())
                        .build());

        obj.setContentType(file.getContentType());
        obj.setSize(file.getSize());
        obj.setCreatedAt(LocalDateTime.now()); // Update timestamp on overwrite

        return objectRepository.save(obj);
    }

    public byte[] downloadObject(String bucketName, String name, String projectRef, UUID userId, String role) throws IOException {
        Bucket bucket = bucketRepository.findByNameAndProjectRef(bucketName, projectRef)
                .orElseThrow(() -> new RuntimeException("Bucket not found"));

        StorageObject obj = objectRepository.findByNameAndBucketNameAndProjectRef(name, bucketName, projectRef)
                .orElseThrow(() -> new RuntimeException("Object not found"));

        if (!bucket.isPublic()) {
            if (userId == null || (!role.equals("SERVICE_ROLE") && !obj.getOwnerId().equals(userId))) {
                throw new RuntimeException("Unauthorized to access object");
            }
        }

        Path filePath = rootPath.resolve(projectRef).resolve(bucketName).resolve(name);
        return Files.readAllBytes(filePath);
    }

    public void deleteObject(String bucketName, String name, String projectRef, UUID userId, String role) {
        StorageObject obj = objectRepository.findByNameAndBucketNameAndProjectRef(name, bucketName, projectRef)
                .orElseThrow(() -> new RuntimeException("Object not found"));

        if (!role.equals("SERVICE_ROLE") && !obj.getOwnerId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete object");
        }

        Path filePath = rootPath.resolve(projectRef).resolve(bucketName).resolve(name);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("Failed to delete file: {}", filePath, e);
        }

        objectRepository.delete(obj);
    }

    public List<StorageObject> listObjects(String bucketName, String projectRef) {
        return objectRepository.findByBucketNameAndProjectRef(bucketName, projectRef);
    }

    public Optional<StorageObject> getObjectMetadata(String bucketName, String name, String projectRef) {
        return objectRepository.findByNameAndBucketNameAndProjectRef(name, bucketName, projectRef);
    }
}
