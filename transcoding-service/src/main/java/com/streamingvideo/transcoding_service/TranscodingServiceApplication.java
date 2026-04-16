package com.streamingvideo.transcoding_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(com.streamingvideo.common.exception.GlobalExceptionHandler.class)
public class TranscodingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TranscodingServiceApplication.class, args);
    }
}
