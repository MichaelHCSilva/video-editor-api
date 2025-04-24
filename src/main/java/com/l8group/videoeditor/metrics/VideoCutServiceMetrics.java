package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class VideoCutServiceMetrics {

    private final Counter cutRequestsTotal;
    private final Counter cutSuccessTotal;
    private final Counter cutFailureTotal;
    private final Timer cutDurationSeconds;
    private final AtomicLong processingQueueSize = new AtomicLong(0);
    private final AtomicLong cutFileSize = new AtomicLong(0);

    public VideoCutServiceMetrics(MeterRegistry registry) {
        cutRequestsTotal = Counter.builder("video_cut_requests_total")
                .description("Total de solicitações de corte de vídeo")
                .register(registry);

        cutSuccessTotal = Counter.builder("video_cut_success_total")
                .description("Total de cortes de vídeo bem-sucedidos")
                .register(registry);

        cutFailureTotal = Counter.builder("video_cut_failure_total")
                .description("Total de cortes de vídeo com falha")
                .register(registry);

        cutDurationSeconds = Timer.builder("video_cut_duration_seconds")
                .description("Duração dos cortes de vídeo em segundos")
                .register(registry);

        Gauge.builder("video_cut_queue_size", processingQueueSize, AtomicLong::get)
                .description("Tamanho atual da fila de cortes de vídeo")
                .register(registry);

        Gauge.builder("video_cut_file_size_bytes", cutFileSize, AtomicLong::get)
                .description("Tamanho do vídeo cortado em bytes")
                .register(registry);
    }

    public void incrementCutRequests() {
        cutRequestsTotal.increment();
    }

    public void incrementCutSuccess() {
        cutSuccessTotal.increment();
    }

    public void incrementCutFailure() {
        cutFailureTotal.increment();
    }

    public Timer.Sample startCutTimer() {
        return Timer.start();
    }

    public void recordCutDuration(Timer.Sample sample) {
        sample.stop(cutDurationSeconds);
    }

    public void incrementProcessingQueueSize() {
        processingQueueSize.incrementAndGet();
    }

    public void decrementProcessingQueueSize() {
        processingQueueSize.decrementAndGet();
    }

    public void setCutFileSize(Long size) {
        cutFileSize.set(size);
    }
}
