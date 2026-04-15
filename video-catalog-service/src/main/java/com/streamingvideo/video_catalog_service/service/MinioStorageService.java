package com.streamingvideo.video_catalog_service.service;

import com.streamingvideo.common.exception.AppException;
import com.streamingvideo.common.exception.ErrorCode;
import io.minio.*;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Service interact with MinIO Object Storage.
 * <p>
 * MinIO use concepts:
 * - Bucket: similar to the root folder
 * - Object: the file being stored
 * <p>
 * Bucket structure:
 * - video-raw/ -> Unprocessed original video
 * - videos-hls/ → Video converted to HLS (.m3u8 + .ts)
 * - thumbnails/ → Thumbnail images
 */
@Slf4j
@Service
public class MinioStorageService {
    private final MinioClient minioClient;
    @Value("${minio.bucket.raw:videos-raw}")
    private String rawBucket;

    @Value("${minio.bucket.hls:videos-hls}")
    private String hlsBucket;

    @Value("${minio.bucket.thumbnails:thumbnails}")
    private String thumbnailsBucket;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    /**
     * Create bucket if not exist
     */
    @jakarta.annotation.PostConstruct
    public void initBuckets() {
        try {
            createBucketIfNotExists(rawBucket);
            createBucketIfNotExists(hlsBucket);
            createBucketIfNotExists(thumbnailsBucket);
            log.info("MinIO buckets initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize MinIO buckets", e);
            throw new AppException(ErrorCode.MINIO_INIT_ERROR);
        }
    }

    /**
     * Upload original video file to MinIP
     *
     * @param file (File video from request)
     * @return objectName (path in bucket)
     */
    public String uploadRawVideo(MultipartFile file) {
        try {
            String extension = getFileExtension(file.getOriginalFilename());
            String objectName = UUID.randomUUID() + "." + extension;
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(rawBucket)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            log.info("Uploaded raw video: bucket={}, object={}, size={}",
                    rawBucket, objectName, file.getSize());

            return objectName;
        } catch (Exception e) {
            log.error("Failed to upload video to MinIO", e);
            throw new AppException(ErrorCode.MINIO_UPLOAD_ERROR);
        }
    }

    /**
     * Create a presigned URL for clients to access the file.
     * <p>
     * * The URL has a time limit (default: 7 days).
     */
    public String getPresignedUrl(String bucket, String objectName) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectName)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new AppException(ErrorCode.MINIO_GENERATE_URL_ERROR);
        }
    }

    /**
     * Get public url for hls file
     * format: http://minio-host:9000/videos-hls/{objectName}
     */
    public String getHlsPublicUrl(String objectName) {
        return getPresignedUrl(hlsBucket, objectName);
    }
    /**
     * Delete Object in bucket
     */
    public void deleteObject(String bucket, String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
            log.info("Deleted object: bucket={}, object={}", bucket, objectName);
        } catch (Exception e) {
            log.error("Failed to delete object from MinIO", e);
        }
    }

    // ── Helper methods ──
    private void createBucketIfNotExists(String bucketName) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
        );
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucketName).build()
            );
            log.info("Created MinIO bucket: {}", bucketName);
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "mp4";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "mp4";
    }
}
