package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class VideoBatchServiceMetrics {

    private final Counter batchRequestsTotal;
    private final Counter batchSuccessTotal;
    private final Counter batchFailureTotal;
    private final Timer batchProcessingDurationSeconds;
    private final AtomicLong processingQueueSize = new AtomicLong(0);
    private final AtomicLong processedFileSize = new AtomicLong(0);

    public VideoBatchServiceMetrics(MeterRegistry registry) {
        batchRequestsTotal = Counter.builder("video_batch_requests_total")
                .description("Total de solicitações de processamento em lote de vídeos")
                .register(registry);

        batchSuccessTotal = Counter.builder("video_batch_success_total")
                .description("Total de processamentos em lote bem-sucedidos")
                .register(registry);

        batchFailureTotal = Counter.builder("video_batch_failure_total")
                .description("Total de processamentos em lote com falha")
                .register(registry);

        batchProcessingDurationSeconds = Timer.builder("video_batch_processing_duration_seconds")
                .description("Duração do processamento em lote de vídeos em segundos")
                .register(registry);

        Gauge.builder("video_batch_queue_size", processingQueueSize, AtomicLong::get)
                .description("Tamanho atual da fila de processamento em lote de vídeos")
                .register(registry);

        Gauge.builder("video_batch_processed_file_size_bytes", processedFileSize, AtomicLong::get)
                .description("Tamanho do arquivo final processado em bytes")
                .register(registry);
    }

    public void incrementBatchRequests() {
        batchRequestsTotal.increment();
    }

    public void incrementBatchSuccess() {
        batchSuccessTotal.increment();
    }

    public void incrementBatchFailure() {
        batchFailureTotal.increment();
    }

    public Timer.Sample startBatchProcessingTimer() {
        return Timer.start();
    }

    public void recordBatchProcessingDuration(Timer.Sample sample) {
        sample.stop(batchProcessingDurationSeconds);
    }

    public void incrementProcessingQueueSize() {
        processingQueueSize.incrementAndGet();
    }

    public void decrementProcessingQueueSize() {
        processingQueueSize.decrementAndGet();
    }

    public void setProcessedFileSize(Long size) {
        processedFileSize.set(size);
    }
}
