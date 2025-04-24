package com.l8group.videoeditor.requests;

import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoFileRequest {

    @NotNull(message = "O arquivo de vídeo é obrigatório.")
    private MultipartFile file;
}