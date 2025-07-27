package com.hyetaekon.hyetaekon.common.s3bucket.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.CopyObjectResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.hyetaekon.hyetaekon.common.exception.GlobalException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.hyetaekon.hyetaekon.common.exception.ErrorCode.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3BucketService {
    private final AmazonS3 s3Client; // S3 클라이언트 주입

    @Value("${aws.s3.bucket.name}")
    private String bucketName;

    // 파일(이미지) 업로드
    public List<String> upload(List<MultipartFile> files, String path) {
        if (files == null || files.isEmpty()) {
            throw new GlobalException(INVALID_FILE_LIST); // 파일이 비어 있을 때 예외 처리
        }

        List<String> uploadedUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String fileName = generateValidPath(path) + createUniqueFileName(file);
            String fileUrl = "https://" + bucketName + ".s3.amazonaws.com/" + fileName;

            try {
                s3Client.putObject(bucketName, fileName, file.getInputStream(), createFileMetadata(file));
                uploadedUrls.add(fileUrl);
            } catch (IOException e) {
                throw new GlobalException(FILE_UPLOAD_FAILED);
            }
        }
        return uploadedUrls;
    }

    // 파일(이미지) 삭제
    public void remove(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) {
            throw new GlobalException(INVALID_FILE_LIST);
        }

        List<String> failedFiles = new ArrayList<>();

        for (String filename : filenames) {
            if (filename == null || filename.trim().isEmpty()) {
                throw new GlobalException(INVALID_FILE_PATH);
            }

            try {
                if (!s3Client.doesObjectExist(bucketName, filename)) {
                    throw new GlobalException(FILE_NOT_FOUND);
                }
                s3Client.deleteObject(bucketName, filename);
            } catch (Exception e) {
                failedFiles.add(filename + " (" + e.getMessage() + ")");
            }
        }

        if (!failedFiles.isEmpty()) {
            throw new GlobalException(FILE_UPLOAD_FAILED);
        }
    }


    // S3 URL에서 키 추출
    private String extractKeyFromUrl(String url) {
        return url.replace("https://" + bucketName + ".s3.amazonaws.com/", "");
    }

    // 고유 파일명 생성 (UUID 사용)
    private String createUniqueFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return UUID.randomUUID().toString() + extension;
    }

    // 경로 생성 및 검증
    private String generateValidPath(String path) {
        String trimmedPath = path.trim();

        // 빈 문자열인 경우
        if (trimmedPath.isEmpty()) {
            return "";
        }

        // 허용되지 않는 문자 패턴 검사
        String invalidChars = "[\\\\:*?\"<>|]";
        if (trimmedPath.matches(".*" + invalidChars + ".*")) {
            throw new GlobalException(INVALID_FILE_PATH);
        }

        // 경로 시작과 끝의 '/' 처리
        trimmedPath = trimmedPath.replaceAll("^/+|/+$", "");
        return trimmedPath + "/";
    }

    // 파일 메타데이터 생성
    private ObjectMetadata createFileMetadata(MultipartFile file) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(file.getContentType());
        metadata.setContentLength(file.getSize());
        return metadata;
    }

    public void moveObject(String sourceUrl, String destinationPath) {
        String sourceKey = extractKeyFromUrl(sourceUrl);
        String destinationKey = generateValidPath(destinationPath);

        // 원본 파일 존재 여부 확인
        if (!s3Client.doesObjectExist(bucketName, sourceKey)) {
            throw new GlobalException(FILE_NOT_FOUND);
        }

        try {
            CopyObjectRequest copyRequest = new CopyObjectRequest(
                bucketName,
                sourceKey,
                bucketName,
                destinationKey
            );

            CopyObjectResult copyResult = s3Client.copyObject(copyRequest);

            if (copyResult != null && copyResult.getETag() != null) {
                s3Client.deleteObject(bucketName, sourceKey);
            } else {
                throw new GlobalException(FILE_MOVE_FAILED);
            }
        } catch (Exception e) {
            throw new GlobalException(FILE_MOVE_FAILED);
        }
    }
}
