/*package com.l8group.videoeditor.services;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoStorageService {

    private final VideoRepository videoRepository;

    @Value("${video.upload.dir}")
    private String uploadDir;

    public Video saveOrReplaceVideo(MultipartFile file, String videoId, boolean persistInDb) {
        try {
            String extension = getExtension(file);
            Path uploadPath = Paths.get(uploadDir);
            Path videoFilePath = uploadPath.resolve(videoId + extension);

            Video video;

            Optional<Video> optionalVideo = persistInDb
                    ? videoRepository.findByVideoId(videoId)
                    : Optional.empty();

            if (optionalVideo.isPresent()) {
                video = optionalVideo.get();
                log.info("Vídeo existente encontrado. Sobrescrevendo: {}", videoId);
                video.setUpdatedTimes(video.getUpdatedTimes() + 1);
            } else {
                video = new Video();
                video.setVideoId(videoId);
                video.setCreatedTime(LocalDateTime.now());
                video.setUpdatedTimes(1);
            }

            video.setFileName(file.getOriginalFilename());
            video.setStatus(VideoStatus.PENDING);
            video.setUploadTime(LocalDateTime.now());

            Files.copy(file.getInputStream(), videoFilePath, StandardCopyOption.REPLACE_EXISTING);

            if (persistInDb) {
                videoRepository.save(video);
            }

            log.info("Vídeo salvo em: {}", videoFilePath);
            return video;

        } catch (IOException e) {
            log.error("Erro ao salvar vídeo", e);
            throw new RuntimeException("Erro ao salvar vídeo", e);
        }
    }

    private String getExtension(MultipartFile file) {
        String name = file.getOriginalFilename();
        return name != null && name.contains(".") ? name.substring(name.lastIndexOf(".")) : ".mp4";
    }
}

*/