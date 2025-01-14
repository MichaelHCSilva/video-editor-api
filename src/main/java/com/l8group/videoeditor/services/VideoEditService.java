package com.l8group.videoeditor.services;

import com.l8group.videoeditor.dto.VideoEditDTO;
import com.l8group.videoeditor.models.VideoEdit;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.repositories.VideoEditRepository;
import com.l8group.videoeditor.repositories.VideoFileRepository;
import com.l8group.videoeditor.requests.VideoCutRequest;
import com.l8group.videoeditor.utils.FFmpegUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class VideoEditService {

    private final FFmpegUtils ffmpegUtils;
    private final VideoEditRepository videoEditRepository;
    private final VideoFileRepository videoFileRepository;

    @Autowired
    public VideoEditService(
            FFmpegUtils ffmpegUtils,
            VideoEditRepository videoEditRepository,
            VideoFileRepository videoFileRepository) {
        this.ffmpegUtils = ffmpegUtils;
        this.videoEditRepository = videoEditRepository;
        this.videoFileRepository = videoFileRepository;
    }

    @Value("${video.storage.path}")
    private String videoStoragePath;

    @Value("${video.output.path}")
    private String videoOutputPath;

    public VideoEditDTO cutVideo(VideoCutRequest request) {
        VideoFile videoFile = videoFileRepository.findById(request.getVideoId())
                .orElseThrow(() -> new IllegalArgumentException("Vídeo não encontrado para o ID fornecido."));

        Path videoPath = Paths.get(videoStoragePath, videoFile.getFileName());
        Path outputPath = Paths.get(videoOutputPath, videoFile.getFileName());  

        ffmpegUtils.cutVideo(videoPath.toFile(), outputPath.toFile(), request.getStartTime(), request.getEndTime());

        VideoEdit videoEdit = new VideoEdit();
        videoEdit.setVideoFile(videoFile);
        videoEdit.setStartTime(request.getStartTime());
        videoEdit.setEndTime(request.getEndTime());

        VideoEdit savedVideoEdit = videoEditRepository.save(videoEdit);

        return mapToDTO(savedVideoEdit);
    }

    private VideoEditDTO mapToDTO(VideoEdit videoEdit) {
        VideoEditDTO dto = new VideoEditDTO();
        dto.setId(videoEdit.getId());
        dto.setVideoId(videoEdit.getVideoFile().getId());
        dto.setStartTime(videoEdit.getStartTime());
        dto.setEndTime(videoEdit.getEndTime());
        return dto;
    }
}
