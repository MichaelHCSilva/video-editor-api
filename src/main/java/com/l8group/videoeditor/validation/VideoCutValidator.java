package com.l8group.videoeditor.validation;

import com.l8group.videoeditor.exceptions.InvalidCutTimeException;
import com.l8group.videoeditor.models.VideoFile;
import com.l8group.videoeditor.utils.VideoDurationUtils;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class VideoCutValidator {

    public static void validateCutTimes(int startTime, int endTime, VideoFile videoFile) {
        int videoDurationInSeconds;
        String videoDurationFormatted;
        String videoFilePath = videoFile != null ? videoFile.getVideoFilePath() : "N/A";

        try {
            videoDurationInSeconds = VideoDurationUtils.getVideoDurationInSeconds(videoFile);
            videoDurationFormatted = VideoDurationUtils.formatSecondsToTime(videoDurationInSeconds);
        } catch (IOException e) {
            log.error("Erro ao obter a duração do vídeo: {}", videoFilePath, e);
            throw new RuntimeException("Erro ao obter a duração do vídeo.", e);
        }

        if (startTime == 0 && endTime == 0) {
            throw new InvalidCutTimeException("A duração do corte precisa ser maior que zero.");
        }

        if (startTime < 0) {
            throw new InvalidCutTimeException(String.format("O tempo de início (%s) não pode ser negativo.",
                                                          VideoDurationUtils.formatSecondsToTime(startTime)));
        }

        if (endTime < 0) {
            throw new InvalidCutTimeException(String.format("O tempo de término (%s) não pode ser negativo.",
                                                        VideoDurationUtils.formatSecondsToTime(endTime)));
        }

        if (startTime == endTime) {
            throw new InvalidCutTimeException(String.format("O tempo de início (%s) e o tempo de término (%s) não podem ser iguais.",
                                                          VideoDurationUtils.formatSecondsToTime(startTime),
                                                          VideoDurationUtils.formatSecondsToTime(endTime)));
        }

        if (startTime > endTime) {
            throw new InvalidCutTimeException(String.format("O tempo de início (%s) não pode ser maior que o tempo de término (%s).",
                                                          VideoDurationUtils.formatSecondsToTime(startTime),
                                                          VideoDurationUtils.formatSecondsToTime(endTime)));
        }

        if (startTime > videoDurationInSeconds) {
            throw new InvalidCutTimeException(String.format("O tempo de início (%s) excede a duração do vídeo (%s).",
                                                          VideoDurationUtils.formatSecondsToTime(startTime),
                                                          videoDurationFormatted));
        }

        if (endTime > videoDurationInSeconds) {
            throw new InvalidCutTimeException(String.format("O tempo de término (%s) excede a duração do vídeo (%s).",
                                                        VideoDurationUtils.formatSecondsToTime(endTime),
                                                        videoDurationFormatted));
        }

        log.info("Validação de tempo de corte concluída com sucesso: start={}s, end={}s, duração total do vídeo={}s",
                 startTime, endTime, videoDurationInSeconds);
    }
}