package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class VideoDownloadMetrics {

    private final Counter totalDownloadRequests;
    private final Counter successfulDownloads;
    private final Counter failedDownloads;
    private final Timer downloadDurationSeconds;
    private final AtomicLong totalDownloadedFileSize = new AtomicLong(0);

    public VideoDownloadMetrics(MeterRegistry registry) {
        totalDownloadRequests = Counter.builder("video_download_requests_total")
                .description("Total de solicitações de download de vídeos")
                .register(registry);

        successfulDownloads = Counter.builder("video_download_success_total")
                .description("Total de downloads bem-sucedidos de vídeos")
                .register(registry);

        failedDownloads = Counter.builder("video_download_failure_total")
                .description("Total de falhas no download de vídeos")
                .register(registry);

        downloadDurationSeconds = Timer.builder("video_download_duration_seconds")
                .description("Duração do processo de download de vídeos em segundos")
                .register(registry);

        Gauge.builder("video_downloaded_file_size_bytes", totalDownloadedFileSize, AtomicLong::get)
                .description("Tamanho total dos arquivos baixados em bytes")
                .register(registry);
    }

    public void incrementDownloadRequests() {
        totalDownloadRequests.increment();
    }

    public void incrementSuccessfulDownloads() {
        successfulDownloads.increment();
    }

    public void incrementFailedDownloads() {
        failedDownloads.increment();
    }

    public Timer.Sample startDownloadTimer() {
        return Timer.start();
    }

    public void recordDownloadDuration(Timer.Sample sample) {
        sample.stop(downloadDurationSeconds);
    }

    public void addDownloadedFileSize(long fileSize) {
        totalDownloadedFileSize.addAndGet(fileSize);
    }
}
