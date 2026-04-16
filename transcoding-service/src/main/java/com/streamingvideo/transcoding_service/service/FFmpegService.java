package com.streamingvideo.transcoding_service.service;

import com.streamingvideo.common.exception.AppException;
import com.streamingvideo.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Service implement FFmpeg commands to convert video
 * Explain:
 * 1. Thumbnail extraction
 * ffmpeg -i input.mp4 -ss 00:00:05 -vframes 1 thumbnail.jpg
 * -> Take the frame at the 5-second mark as the thumbnail.
 * 2. HLS Transcoding (720p):
 * ffmpeg -i input.mp4 \
 * -vf scale=-2:720 \           # Scale video to 720p (keep ratio)
 * -c:v libx264 \               # Video codec: H.264
 * -preset fast \                # Encode speed (fast vs slow)
 * -crf 23 \                     # quality (18=high, 23=medium, 28=low)
 * -c:a aac \                    # Audio codec: AAC
 * -b:a 128k \                   # Audio bitrate
 * -hls_time 10 \               #  1 segment = 10 s
 * -hls_playlist_type vod \     # Video on Demand (no live)
 * -hls_segment_filename "seg_%03d.ts" \
 * playlist.m3u8
 */
@Slf4j
@Service
public class FFmpegService {
    @Value("${transcoding.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Value("${transcoding.segment-duration:10}")
    private int segmentDuration;

    @Value("${transcoding.timeout-minutes:60}")
    private int timeoutMinutes;

    public void extractThumbnail(Path inputFile, Path outputFile) throws Exception {
        String[] command = {
                ffmpegPath,
                "-i", inputFile.toString(),
                "-ss", "00:00:05",
                "-vframes", "1",             // Take only 1 frame
                "-vf", "scale=640:360",      // Scale thumbnail to 640x360
                "-q:v", "2",                 // Quality JPEG (1=best, 31=worst)
                "-y",                        // Overwrite output
                outputFile.toString()
        };
        executeCommand(command, "Thumbnail extraction");
    }

    /**
     * Transcode video to HLS with specific quality
     * @param inputFile Path to file original video
     * @param outputDir Directory contain output HLS files
     * @param height (360,720,1080)
     * @param videoBitrate (800k", "2500k", "5000k")
     * @param audioBitrate ("96k", "128k", "192k")
     * @throws Exception
     */
    public void transcodeToHls(Path inputFile, Path outputDir,
                               int height, String videoBitrate,
                               String audioBitrate) throws Exception {
        // Create dir output if not exists
        Files.createDirectories(outputDir);

        String segmentPattern = outputDir.resolve("segment_%03d.ts").toString();
        String playlistFile = outputDir.resolve("playlist.m3u8").toString();

        String[] command = {
                ffmpegPath,
                "-i", inputFile.toString(),

                // Video settings
                "-vf", "scale=-2:" + height,     // Scale keep ratio
                "-c:v", "libx264",               // H.264 codec
                "-preset", "fast",               // Balance speed/quality
                "-crf", "23",                    // Constant Rate Factor
                "-b:v", videoBitrate,            // Max video bitrate
                "-maxrate", videoBitrate,
                "-bufsize", videoBitrate,

                // Audio settings
                "-c:a", "aac",
                "-b:a", audioBitrate,
                "-ar", "44100",                  // Sample rate

                // HLS settings
                "-hls_time", String.valueOf(segmentDuration),
                "-hls_playlist_type", "vod",
                "-hls_segment_filename", segmentPattern,

                // Output
                "-y",
                playlistFile
        };

        executeCommand(command, "HLS transcode " + height + "p");
    }

    /**
     * Create master playlist (.m3u8) connect all variant quality
     * Master playlist allows clients to select the appropriate quality
     * based on network bandwidth
     */
    public void createMasterPlaylist(Path outputDir,String videoId) throws IOException{
        StringBuilder content = new StringBuilder();
        content.append("#EXTM3U\n");
        content.append("#EXT-X-VERSION:3\n\n");

        // 360p variant
        content.append("#EXT-X-STREAM-INF:BANDWIDTH=800000,RESOLUTION=640x360\n");
        content.append("360p/playlist.m3u8\n\n");

        // 720p variant
        content.append("#EXT-X-STREAM-INF:BANDWIDTH=2500000,RESOLUTION=1280x720\n");
        content.append("720p/playlist.m3u8\n\n");

        // 1080p variant
        content.append("#EXT-X-STREAM-INF:BANDWIDTH=5000000,RESOLUTION=1920x1080\n");
        content.append("1080p/playlist.m3u8\n");

        Path masterPlaylist = outputDir.resolve("master.m3u8");
        Files.writeString(masterPlaylist, content.toString());

        log.info("Created master playlist: {}", masterPlaylist);
    }

    /**
     * Get duration video (second) by ffprobe
     */
    public int getVideoDuration(Path inputFile) throws Exception{
        String[] command = {
                "ffprobe",
                "-v", "error",
                "-show_entries", "format=duration",
                "-of", "default=noprint_wrappers=1:nokey=1",
                inputFile.toString()
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        String output = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor(30, TimeUnit.SECONDS);

        try {
            return (int) Double.parseDouble(output);
        } catch (NumberFormatException e) {
            log.warn("Could not parse video duration: {}", output);
            return 0;
        }
    }

    /**
     * Get resolution of original video
     * @param inputFile
     * @return
     * @throws Exception
     */
    public String getVideoResolution(Path inputFile) throws Exception {
        String[] command = {
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=width,height",
                "-of", "csv=s=x:p=0",
                inputFile.toString()
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();

        String output = new String(process.getInputStream().readAllBytes()).trim();
        process.waitFor(30, TimeUnit.SECONDS);

        return output;  // "1920x1080"
    }

    // ============================== Helper ==============================
    private void executeCommand(String[] command, String taskName) throws Exception {
        log.info("Executing FFmpeg: {} | Command: {}", taskName, String.join(" ", command));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("[FFmpeg] {}", line);
            }
        }
        boolean completed = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            throw new AppException(ErrorCode.TRANSCODE_TIMEOUT);
        }
        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new AppException(ErrorCode.TRANSCODE_FAILED);
        }
        log.info("FFmpeg completed successfully: {}", taskName);
    }
}
