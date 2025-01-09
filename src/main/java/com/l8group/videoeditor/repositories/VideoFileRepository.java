package com.l8group.videoeditor.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.l8group.videoeditor.models.VideoFile;

@Repository
public interface VideoFileRepository extends JpaRepository<VideoFile, UUID> {

    @Query("SELECT v.fileName, v.uploadedAt, v.status FROM VideoFile v")
    List<Object[]> findFileNamesAndUploadDates();
}
