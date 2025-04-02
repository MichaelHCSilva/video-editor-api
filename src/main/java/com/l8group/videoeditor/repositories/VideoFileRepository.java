package com.l8group.videoeditor.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.l8group.videoeditor.dtos.VideoFileListDTO;
import com.l8group.videoeditor.enums.VideoStatusEnum;
import com.l8group.videoeditor.models.VideoFile;

@Repository
public interface VideoFileRepository extends JpaRepository<VideoFile, UUID> {

    List<VideoFile> findByStatus(VideoStatusEnum status);

    @Query("SELECT new com.l8group.videoeditor.dtos.VideoFileListDTO(v.videoFileName, v.createdTimes, v.status) " +
            "FROM VideoFile v")
    List<VideoFileListDTO> findAllVideos();

}
