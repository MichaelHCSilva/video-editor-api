package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class VideoOverlayServiceMetrics {

    private final Counter overlayRequestsTotal;
    private final Counter overlaySuccessTotal;
    private final Counter overlayFailureTotal;
    private final Timer overlayDurationSeconds;
    private final AtomicLong processingQueueSize = new AtomicLong(0);
    private final AtomicLong overlayFileSize = new AtomicLong(0);

    public VideoOverlayServiceMetrics(MeterRegistry registry) {
        overlayRequestsTotal = Counter.builder("video_overlay_requests_total")
                .description("Total de solicitações de overlay de vídeo")
                .register(registry);

        overlaySuccessTotal = Counter.builder("video_overlay_success_total")
                .description("Total de overlays de vídeo bem-sucedidos")
                .register(registry);

        overlayFailureTotal = Counter.builder("video_overlay_failure_total")
                .description("Total de overlays de vídeo com falha")
                .register(registry);

        overlayDurationSeconds = Timer.builder("video_overlay_duration_seconds")
                .description("Duração dos overlays de vídeo em segundos")
                .register(registry);

        Gauge.builder("video_overlay_queue_size", processingQueueSize, AtomicLong::get)
                .description("Tamanho atual da fila de overlays de vídeo")
                .register(registry);

        Gauge.builder("video_overlay_file_size_bytes", overlayFileSize, AtomicLong::get)
                .description("Tamanho do vídeo com overlay em bytes")
                .register(registry);
    }

    public void incrementOverlayRequests() {
        overlayRequestsTotal.increment();
    }

    public void incrementOverlaySuccess() {
        overlaySuccessTotal.increment();
    }

    public void incrementOverlayFailure() {
        overlayFailureTotal.increment();
    }

    public Timer.Sample startOverlayProcessingTimer() {
        return Timer.start();
    }

    public void recordOverlayProcessingDuration(Timer.Sample sample) {
        sample.stop(overlayDurationSeconds);
    }

    public void incrementProcessingQueueSize() {
        processingQueueSize.incrementAndGet();
    }

    public void decrementProcessingQueueSize() {
        processingQueueSize.decrementAndGet();
    }

    public void setOverlayFileSize(Long size) {
        overlayFileSize.set(size);
    }
}
