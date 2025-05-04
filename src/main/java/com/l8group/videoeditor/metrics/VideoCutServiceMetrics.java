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
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@RequiredArgsConstructor
public class VideoCutServiceMetrics {

    private final MeterRegistry meterRegistry;

    private Counter cutRequests;
    private Counter cutSuccess;
    private Counter cutFailures;
    private Counter cutFailureInvalidCutTime;
    private Counter cutFailureInvalidMediaProperties;
    private Counter cutFailureProcessing;
    private Timer cutTimer;
    private AtomicInteger processingQueueSize;

    private AtomicLong maxCutDuration = new AtomicLong(0);

    @PostConstruct
    private void initMetrics() {
        cutRequests = meterRegistry.counter("video_cut_requests");
        cutSuccess = meterRegistry.counter("video_cut_success");
        cutFailures = meterRegistry.counter("video_cut_failures");
        cutFailureInvalidCutTime = meterRegistry.counter("video_cut_failure_invalid_cut_time");
        cutFailureInvalidMediaProperties = meterRegistry.counter("video_cut_failure_invalid_media_properties");
        cutFailureProcessing = meterRegistry.counter("video_cut_failure_processing");

        cutTimer = meterRegistry.timer("video_cut_duration");

        processingQueueSize = new AtomicInteger(0);
        Gauge.builder("video_cut_processing_queue_size", processingQueueSize, AtomicInteger::get)
                .register(meterRegistry);

        if (meterRegistry.find("video_cut_duration_seconds_max") == null) {
            Gauge.builder("video_cut_duration_seconds_max", maxCutDuration, AtomicLong::get)
                    .register(meterRegistry);
        }

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
        return Timer.start();
    }

    public void recordCutDuration(Timer.Sample timerSample) {
        long duration = timerSample.stop(cutTimer);
        updateMaxCutDuration(duration);
    }

    private void updateMaxCutDuration(long duration) {
        long currentMax = maxCutDuration.get();
        if (duration > currentMax) {
            if (maxCutDuration.compareAndSet(currentMax, duration)) {
                log.info("Updated max cut duration to: {} ms", duration);
            }
        }
    }

    public void incrementProcessingQueueSize() {
        processingQueueSize.incrementAndGet();
    }

    public void decrementProcessingQueueSize() {
        processingQueueSize.decrementAndGet();
    }
}
