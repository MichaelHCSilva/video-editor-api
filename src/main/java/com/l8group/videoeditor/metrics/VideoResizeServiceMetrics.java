package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class VideoResizeServiceMetrics {

    private final Counter resizeRequestsTotal;
    private final Counter resizeSuccessTotal;
    private final Counter resizeFailureTotal;
    private final Timer resizeDurationSeconds;
    private final AtomicLong processingQueueSize = new AtomicLong(0);
    private final AtomicLong resizeFileSize = new AtomicLong(0);

    public VideoResizeServiceMetrics(MeterRegistry registry) {
        resizeRequestsTotal = Counter.builder("video_resize_requests_total")
                .description("Total de solicitações de redimensionamento de vídeo")
                .register(registry);

        resizeSuccessTotal = Counter.builder("video_resize_success_total")
                .description("Total de redimensionamentos bem-sucedidos")
                .register(registry);

        resizeFailureTotal = Counter.builder("video_resize_failure_total")
                .description("Total de falhas no redimensionamento de vídeo")
                .register(registry);

        resizeDurationSeconds = Timer.builder("video_resize_duration_seconds")
                .description("Duração do redimensionamento do vídeo em segundos")
                .register(registry);

        Gauge.builder("video_resize_queue_size", processingQueueSize, AtomicLong::get)
                .description("Tamanho atual da fila de redimensionamento de vídeo")
                .register(registry);

        Gauge.builder("video_resize_file_size_bytes", resizeFileSize, AtomicLong::get)
                .description("Tamanho do arquivo de vídeo redimensionado em bytes")
                .register(registry);
    }

    public void incrementResizeRequests() {
        resizeRequestsTotal.increment();
    }

    public void incrementResizeSuccess() {
        resizeSuccessTotal.increment();
    }

    public void incrementResizeFailure() {
        resizeFailureTotal.increment();
    }

    public Timer.Sample startResizeTimer() {
        return Timer.start();
    }

    public void recordResizeDuration(Timer.Sample sample) {
        sample.stop(resizeDurationSeconds);
    }

    public void incrementProcessingQueueSize() {
        processingQueueSize.incrementAndGet();
    }

    public void decrementProcessingQueueSize() {
        processingQueueSize.decrementAndGet();
    }

    public void setResizeFileSize(Long size) {
        resizeFileSize.set(size);
    }
}
