package com.hyetaekon.hyetaekon.common.s3bucket.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.hyetaekon.hyetaekon.common.s3bucket.service.S3BucketService;

import java.util.List;

@RestController
@RequestMapping("/api/s3/bucket")
@RequiredArgsConstructor
public class S3BucketController {
    private final S3BucketService s3BucketService;

    @PostMapping("/upload")
    public ResponseEntity<List<String>> uploadFiles(
        @RequestParam("files") List<MultipartFile> files,
        @RequestParam("path") String path
    ) {
        List<String> fileUrls = s3BucketService.upload(files, path);
        return ResponseEntity.ok(fileUrls);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteFiles(@RequestBody List<String> urls) {
        s3BucketService.remove(urls);
        return ResponseEntity.noContent().build();
    }
}

