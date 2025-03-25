package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.*;
import org.springframework.stereotype.Component;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class VideoFileServiceMetrics {

    private final Counter uploadRequestsTotal;
    private final Counter uploadSuccessTotal;
    private final Counter uploadFailureTotal;
    private final Timer uploadDurationSeconds;
    private final AtomicLong processingQueueSize = new AtomicLong(0);
    private final AtomicLong fileSize = new AtomicLong(0);
    private final Counter fileValidationErrorsTotal;
    private final Counter fileStorageErrorsTotal;

    public VideoFileServiceMetrics(MeterRegistry registry) {
        uploadRequestsTotal = Counter.builder("video_upload_requests_total")
                .description("Total de solicitações de upload de vídeo")
                .register(registry);

        uploadSuccessTotal = Counter.builder("video_upload_success_total")
                .description("Total de uploads de vídeo bem-sucedidos")
                .register(registry);

        uploadFailureTotal = Counter.builder("video_upload_failure_total")
                .description("Total de uploads de vídeo com falha")
                .register(registry);

        uploadDurationSeconds = Timer.builder("video_upload_duration_seconds")
                .description("Duração dos uploads de vídeo em segundos")
                .register(registry);

        Gauge.builder("video_processing_queue_size", processingQueueSize, AtomicLong::get)
                .description("Tamanho atual da fila de processamento de vídeos")
                .register(registry);

        fileValidationErrorsTotal = Counter.builder("video_file_validation_errors_total")
                .description("Total de erros de validação de arquivo")
                .register(registry);

        fileStorageErrorsTotal = Counter.builder("video_file_storage_errors_total")
                .description("Total de erros de armazenamento de arquivo")
                .register(registry);

        Gauge.builder("video_file_size_bytes", fileSize, AtomicLong::get)
                .description("Tamanho do vídeo sendo enviado em bytes")
                .register(registry);
    }

    public void incrementUploadRequests() {
        uploadRequestsTotal.increment();
    }

    public void incrementUploadSuccess() {
        uploadSuccessTotal.increment();
    }

    public void incrementUploadFailure() {
        uploadFailureTotal.increment();
    }

    public Timer.Sample startUploadTimer() {
        return Timer.start();
    }

    public void recordUploadDuration(Timer.Sample sample) {
        sample.stop(uploadDurationSeconds);
    }

    public void incrementProcessingQueueSize() {
        processingQueueSize.incrementAndGet();
    }

    public void decrementProcessingQueueSize() {
        processingQueueSize.decrementAndGet();
    }

    public void incrementFileValidationErrors() {
        fileValidationErrorsTotal.increment();
    }

    public void incrementFileStorageErrors() {
        fileStorageErrorsTotal.increment();
    }

    public void setFileSize(Long size) {
        fileSize.set(size);
    }
}