package com.jauharxlr.springbase.storage.controller;

import com.jauharxlr.springbase.storage.entity.Bucket;
import com.jauharxlr.springbase.storage.entity.StorageObject;
import com.jauharxlr.springbase.storage.service.StorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/storage/v1")
@RequiredArgsConstructor
@Tag(name = "Storage", description = "Object storage and bucket management")
@SecurityRequirement(name = "bearerAuth")
public class StorageController {

    private final StorageService storageService;

    @PostMapping("/buckets")
    public ResponseEntity<Bucket> createBucket(@RequestBody BucketRequest request, HttpServletRequest httpRequest) {
        String projectRef = (String) httpRequest.getAttribute("project_ref");
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);
        
        return ResponseEntity.ok(storageService.createBucket(request.getName(), request.isPublic(), projectRef, userId));
    }

    @GetMapping("/buckets")
    public ResponseEntity<List<Bucket>> listBuckets(HttpServletRequest httpRequest) {
        String projectRef = (String) httpRequest.getAttribute("project_ref");
        return ResponseEntity.ok(storageService.listBuckets(projectRef));
    }

    @DeleteMapping("/buckets/{name}")
    public ResponseEntity<Void> deleteBucket(@PathVariable String name, HttpServletRequest httpRequest) {
        String projectRef = (String) httpRequest.getAttribute("project_ref");
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = getUserId(userIdStr);
        String role = getRole();

        storageService.deleteBucket(name, projectRef, userId, role);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/object/{bucket}/{name}")
    public ResponseEntity<StorageObject> uploadObject(
            @PathVariable String bucket,
            @PathVariable String name,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) throws IOException {
        
        String projectRef = (String) httpRequest.getAttribute("project_ref");
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = UUID.fromString(userIdStr);

        return ResponseEntity.ok(storageService.uploadObject(bucket, name, file, projectRef, userId));
    }

    @GetMapping("/object/{bucket}/{name}")
    public ResponseEntity<byte[]> downloadObject(
            @PathVariable String bucket,
            @PathVariable String name,
            HttpServletRequest httpRequest) throws IOException {
        
        String projectRef = (String) httpRequest.getAttribute("project_ref");
        // We might not have a principal if it's a public bucket and anonymous access
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null ? 
                SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        
        UUID userId = (principal instanceof String && !principal.equals("anonymous")) ? UUID.fromString((String) principal) : null;
        String role = getRole();

        byte[] data = storageService.downloadObject(bucket, name, projectRef, userId, role);
        StorageObject metadata = storageService.getObjectMetadata(bucket, name, projectRef).orElse(null);
        
        String contentType = (metadata != null) ? metadata.getContentType() : "application/octet-stream";
        
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(data);
    }

    @DeleteMapping("/object/{bucket}/{name}")
    public ResponseEntity<Void> deleteObject(
            @PathVariable String bucket,
            @PathVariable String name,
            HttpServletRequest httpRequest) {
        
        String projectRef = (String) httpRequest.getAttribute("project_ref");
        String userIdStr = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UUID userId = getUserId(userIdStr);
        String role = getRole();

        storageService.deleteObject(bucket, name, projectRef, userId, role);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/object/list/{bucket}")
    public ResponseEntity<List<StorageObject>> listObjects(
            @PathVariable String bucket,
            HttpServletRequest httpRequest) {
        String projectRef = (String) httpRequest.getAttribute("project_ref");
        return ResponseEntity.ok(storageService.listObjects(bucket, projectRef));
    }

    private UUID getUserId(String userIdStr) {
        try {
            return UUID.fromString(userIdStr);
        } catch (Exception e) {
            // For anonymous or other cases, though delete usually requires auth
            return null;
        }
    }

    private String getRole() {
        if (SecurityContextHolder.getContext().getAuthentication() == null) return "ANON";
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("ANON");
    }

    @Data
    public static class BucketRequest {
        private String name;
        private boolean isPublic;
    }
}
