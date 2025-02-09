package com.l8group.videoeditor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class VideoEditorApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(VideoEditorApiApplication.class, args);
	}

}
