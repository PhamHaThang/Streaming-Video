package com.streamingvideo.transcoding_service.service;

import com.streamingvideo.common.exception.AppException;
import com.streamingvideo.common.exception.ErrorCode;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;


/**
 * Service interact with
 * - Download original video from bucket "videos-raw"
 * - Upload HLS file (.m3u8 + .ts) to bucket "videos-hls"
 * - Upload thumbnail to bucket "thumbnails"
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageService {
    private final MinioClient minioClient;

    /**
     * Download file from MinIO
     */
    public void downloadFile(String bucket, String objectName, Path outputPath) throws Exception {
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .build()
        )) {
            Files.copy(stream, outputPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Downloaded: {}/{} → {}", bucket, objectName, outputPath);
        }
    }
    /**
     * Upload 1 file to MInIO
     */
    public void uploadFile(String bucket, Path filePath, String objectName) throws Exception {
        String contentType = determineContenttype(objectName);
        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .filename(filePath.toString())
                        .contentType(contentType)
                        .build()
        );
        log.info("Uploaded: {} → {}/{}", filePath.getFileName(), bucket, objectName);
    }

    /**
     * Upload all dir to MinIO (Upload all HLS files)
     */
    public  void uploadDirectory(String bucket, Path localDir, String prefix) throws Exception {
        try (Stream<Path> paths = Files.walk(localDir)){
            paths.filter((Files::isRegularFile))
                    .forEach(filePath -> {
                        try {
                            String relativePath = localDir.relativize(filePath).toString()
                                    .replace("\\", "/");
                            String objectName = prefix + relativePath;
                            uploadFile(bucket,filePath,objectName);
                        } catch (Exception e) {
                            throw new AppException(ErrorCode.MINIO_UPLOAD_ERROR);
                        }
                    });
        }
        log.info("Uploaded directory: {} → {}/{}", localDir, bucket, prefix);
    }

    private String determineContenttype(String objectName){
        if (objectName.endsWith(".m3u8")) return "application/vnd.apple.mpegurl";
        if (objectName.endsWith(".ts")) return "video/mp2t";
        if (objectName.endsWith(".jpg") || objectName.endsWith(".jpeg")) return "image/jpeg";
        if (objectName.endsWith(".png")) return "image/png";
        return "application/octet-stream";
    }
}
