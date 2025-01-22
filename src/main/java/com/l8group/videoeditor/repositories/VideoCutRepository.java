package com.l8group.videoeditor.repositories;

import com.l8group.videoeditor.models.VideoCut;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface VideoCutRepository extends JpaRepository<VideoCut, UUID> {
}
