package com.streamingvideo.video_catalog_service;

import com.streamingvideo.common.exception.GlobalExceptionHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(GlobalExceptionHandler.class)
public class VideoCatalogServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoCatalogServiceApplication.class, args);
	}

}
