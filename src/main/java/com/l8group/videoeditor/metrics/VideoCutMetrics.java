package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoCutMetrics {

    private final MeterRegistry meterRegistry;

    private Counter cutRequests;
    private Counter cutSuccess;
    private Counter cutFailures;
    private Counter cutFailureInvalidCutTime;
    private Counter cutFailureInvalidMediaProperties;
    private Counter cutFailureProcessing;
    private Timer cutTimer;
    private AtomicInteger processingQueueSize;

    @PostConstruct
    private void initMetrics() {
        cutRequests = meterRegistry.counter("video_cut_requests_total");
        cutSuccess = meterRegistry.counter("video_cut_success_total");
        cutFailures = meterRegistry.counter("video_cut_failures_total");
        cutFailureInvalidCutTime = meterRegistry.counter("video_cut_failure_invalid_cut_time_total");
        cutFailureInvalidMediaProperties = meterRegistry.counter("video_cut_failure_invalid_media_properties_total");
        cutFailureProcessing = meterRegistry.counter("video_cut_failure_processing_total");

        cutTimer = meterRegistry.timer("video_cut_duration_seconds");

        processingQueueSize = new AtomicInteger(0);
        Gauge.builder("video_cut_processing_queue_size", processingQueueSize, AtomicInteger::get)
                .description("Tamanho da fila de processamento de corte de vídeo")
                .register(meterRegistry);

        log.info("VideoCutServiceMetrics initialized successfully");
    }

    public void incrementCutRequests() {
        cutRequests.increment();
    }

    public void incrementCutSuccess() {
        cutSuccess.increment();
    }

    public void incrementCutFailures() {
        cutFailures.increment();
    }

    public void incrementCutFailureInvalidCutTime() {
        cutFailureInvalidCutTime.increment();
    }

    public void incrementCutFailureInvalidMediaProperties() {
        cutFailureInvalidMediaProperties.increment();
    }

    public void incrementCutFailureProcessing() {
        cutFailureProcessing.increment();
    }

    public Timer.Sample startCutTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordCutDuration(Timer.Sample timerSample) {
        timerSample.stop(cutTimer);
    }

    public void incrementProcessingQueueSize() {
        processingQueueSize.incrementAndGet();
    }

    public void decrementProcessingQueueSize() {
        processingQueueSize.decrementAndGet();
    }
}