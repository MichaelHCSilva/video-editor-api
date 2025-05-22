package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Counter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class VideoConversionMetrics {

    private final Timer conversionDurationTimer;
    private final Counter conversionRequests;
    private final Counter conversionSuccess;
    private final Counter conversionFailure;
    private final MeterRegistry registry;

    private final AtomicLong convertedFileSizeBytes = new AtomicLong(0);
    private final AtomicInteger processingQueueSize = new AtomicInteger(0);

    public VideoConversionMetrics(MeterRegistry registry) {
        this.registry = registry;

        this.conversionRequests = Counter.builder("video_conversion_requests_total")
                .description("Total de requisições de conversão de vídeo")
                .register(registry);

        this.conversionSuccess = Counter.builder("video_conversion_success_total")
                .description("Total de conversões de vídeo bem-sucedidas")
                .register(registry);

        this.conversionFailure = Counter.builder("video_conversion_failure_total")
                .description("Total de falhas na conversão de vídeo")
                .register(registry);

        this.conversionDurationTimer = Timer.builder("video_conversion_duration_seconds")
                .description("Duração das conversões de vídeo em segundos")
                .register(registry);

        Gauge.builder("video_conversion_file_size_bytes", convertedFileSizeBytes, AtomicLong::get)
                .description("Tamanho total dos arquivos convertidos em bytes") // Mudada descrição
                .register(registry);

        Gauge.builder("video_conversion_queue_size", processingQueueSize, AtomicInteger::get)
                .description("Tamanho da fila de processamento de conversão")
                .register(registry);
    }

    public Timer.Sample startConversionTimer() {
        return Timer.start(registry);
    }

    public void recordConversionDuration(Timer.Sample sample) {
        sample.stop(conversionDurationTimer);
    }

    public void incrementConversionRequests() {
        conversionRequests.increment();
    }

    public void incrementConversionSuccess() {
        conversionSuccess.increment();
    }

    public void incrementConversionFailure() {
        conversionFailure.increment();
    }

    public void addConvertedFileSize(long bytes) { // Alterado para addConvertedFileSize
        convertedFileSizeBytes.addAndGet(bytes);
    }

    public long getTotalConvertedFileSize() { // Alterado para getTotalConvertedFileSize
        return convertedFileSizeBytes.get();
    }

    public void incrementProcessingQueueSize() {
        processingQueueSize.incrementAndGet();
    }

    public void decrementProcessingQueueSize() {
        processingQueueSize.updateAndGet(value -> Math.max(0, value - 1));
    }

    public int getProcessingQueueSize() {
        return processingQueueSize.get();
    }
}