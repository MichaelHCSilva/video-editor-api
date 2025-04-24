package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class VideoConversionServiceMetrics {

    private final Counter conversionRequestsTotal;
    private final Counter conversionSuccessTotal;
    private final Counter conversionFailureTotal;
    private final Timer conversionDurationSeconds;
    private final AtomicLong processingQueueSize = new AtomicLong(0);
    private final AtomicLong convertedFileSize = new AtomicLong(0);

    public VideoConversionServiceMetrics(MeterRegistry registry) {
        conversionRequestsTotal = Counter.builder("video_conversion_requests_total")
                .description("Total de solicitações de conversão de vídeo")
                .register(registry);

        conversionSuccessTotal = Counter.builder("video_conversion_success_total")
                .description("Total de conversões de vídeo bem-sucedidas")
                .register(registry);

        conversionFailureTotal = Counter.builder("video_conversion_failure_total")
                .description("Total de conversões de vídeo com falha")
                .register(registry);

        conversionDurationSeconds = Timer.builder("video_conversion_duration_seconds")
                .description("Duração das conversões de vídeo em segundos")
                .register(registry);

        Gauge.builder("video_conversion_queue_size", processingQueueSize, AtomicLong::get)
                .description("Tamanho atual da fila de conversão de vídeos")
                .register(registry);

        Gauge.builder("video_conversion_file_size_bytes", convertedFileSize, AtomicLong::get)
                .description("Tamanho do vídeo convertido em bytes")
                .register(registry);
    }

    public void incrementConversionRequests() {
        conversionRequestsTotal.increment();
    }

    public void incrementConversionSuccess() {
        conversionSuccessTotal.increment();
    }

    public void incrementConversionFailure() {
        conversionFailureTotal.increment();
    }

    public Timer.Sample startConversionTimer() {
        return Timer.start();
    }

    public void recordConversionDuration(Timer.Sample sample) {
        sample.stop(conversionDurationSeconds);
    }

    public void incrementProcessingQueueSize() {
        processingQueueSize.incrementAndGet();
    }

    public void decrementProcessingQueueSize() {
        processingQueueSize.decrementAndGet();
    }

    public void setConvertedFileSize(Long size) {
        convertedFileSize.set(size);
    }
}
