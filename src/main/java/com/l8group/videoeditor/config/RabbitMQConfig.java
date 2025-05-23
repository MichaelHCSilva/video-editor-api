package com.l8group.videoeditor.config;

import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String VIDEO_EXCHANGE = "video.exchange";

    public static final String USER_STATUS_QUEUE = "user.status.queue";
    public static final String USER_STATUS_ROUTING_KEY = "user.status";
    public static final String USER_STATUS_DLQ = "user.status.dlq";

    public static final String VIDEO_PROCESSING_QUEUE = "video.processing.queue";
    public static final String VIDEO_PROCESSING_ROUTING_KEY = "video.process";
    public static final String VIDEO_PROCESSING_DLQ = "video.processing.dlq";

    public static final String VIDEO_CUT_QUEUE = "video.cut.queue";
    public static final String VIDEO_CUT_ROUTING_KEY = "video.cut.routing.key";
    public static final String VIDEO_CUT_DLQ = "video.cut.dlq";

    public static final String VIDEO_RESIZE_QUEUE = "video.resize.queue";
    public static final String VIDEO_RESIZE_ROUTING_KEY = "video.resize.routing.key";
    public static final String VIDEO_RESIZE_DLQ = "video.resize.dlq";

    public static final String VIDEO_CONVERSION_QUEUE = "video.conversion.queue";
    public static final String VIDEO_CONVERSION_ROUTING_KEY = "video.conversion.routing.key";
    public static final String VIDEO_CONVERSION_DLQ = "video.conversion.dlq";

    public static final String VIDEO_OVERLAY_QUEUE = "video.overlay.queue";
    public static final String VIDEO_OVERLAY_ROUTING_KEY = "video.overlay.routing.key";
    public static final String VIDEO_OVERLAY_DLQ = "video.overlay.dlq";

    public static final String VIDEO_BATCH_PROCESSING_QUEUE = "video.batch.processing.queue";
    public static final String VIDEO_BATCH_PROCESSING_ROUTING_KEY = "video.batch.process";
    public static final String VIDEO_BATCH_PROCESSING_DLQ = "video.batch.processing.dlq";

    public static final String VIDEO_DOWNLOAD_QUEUE = "video.download.queue";
    public static final String VIDEO_DOWNLOAD_ROUTING_KEY = "video.download";
    public static final String VIDEO_DOWNLOAD_DLQ = "video.download.dlq";

    @Bean
    public Queue userStatusQueue() {
        return new Queue(USER_STATUS_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", "", "x-dead-letter-routing-key", USER_STATUS_DLQ));
    }

    @Bean
    public Queue videoProcessingQueue() {
        return new Queue(VIDEO_PROCESSING_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", "video.exchange", "x-dead-letter-routing-key", VIDEO_PROCESSING_DLQ));
    }

    @Bean
    public Queue videoCutQueue() {
        return new Queue(VIDEO_CUT_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", "video.exchange", "x-dead-letter-routing-key", VIDEO_CUT_DLQ));
    }

    @Bean
    public Queue videoResizeQueue() {
        return new Queue(VIDEO_RESIZE_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", "video.exchange", "x-dead-letter-routing-key", VIDEO_RESIZE_DLQ));
    }

    @Bean
    public Queue videoConversionQueue() {
        return new Queue(VIDEO_CONVERSION_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", "video.exchange", "x-dead-letter-routing-key", VIDEO_CONVERSION_DLQ));
    }

    @Bean
    public Queue videoOverlayQueue() {
        return new Queue(VIDEO_OVERLAY_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", "video.exchange", "x-dead-letter-routing-key", VIDEO_OVERLAY_DLQ));
    }

    @Bean
    public Queue videoBatchProcessingQueue() {
        return new Queue(VIDEO_BATCH_PROCESSING_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", "video.exchange", "x-dead-letter-routing-key", VIDEO_BATCH_PROCESSING_DLQ));
    }

    @Bean
    public Queue videoDownloadQueue() {
        return new Queue(VIDEO_DOWNLOAD_QUEUE, true, false, false,
                Map.of("x-dead-letter-exchange", "video.exchange", "x-dead-letter-routing-key", VIDEO_DOWNLOAD_DLQ));
    }


    @Bean
    public Queue userStatusDLQ() {
        return new Queue(USER_STATUS_DLQ, true);
    }

    @Bean
    public Queue videoProcessingDLQ() {
        return new Queue(VIDEO_PROCESSING_DLQ, true);
    }

    @Bean
    public Queue videoCutDLQ() {
        return new Queue(VIDEO_CUT_DLQ, true);
    }

    @Bean
    public Queue videoResizeDLQ() {
        return new Queue(VIDEO_RESIZE_DLQ, true);
    }

    @Bean
    public Queue videoConversionDLQ() {
        return new Queue(VIDEO_CONVERSION_DLQ, true);
    }

    @Bean
    public Queue videoOverlayDLQ() {
        return new Queue(VIDEO_OVERLAY_DLQ, true);
    }

    @Bean
    public Queue videoBatchProcessingDLQ() {
        return new Queue(VIDEO_BATCH_PROCESSING_DLQ, true);
    }

    @Bean
    public Queue videoDownloadDLQ() {
        return new Queue(VIDEO_DOWNLOAD_DLQ, true);
    }

    @Bean
    public TopicExchange videoExchange() {
        return new TopicExchange(VIDEO_EXCHANGE);
    }

    @Bean
    public Binding userStatusBinding(Queue userStatusQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(userStatusQueue).to(videoExchange).with(USER_STATUS_ROUTING_KEY);
    }

    @Bean
    public Binding videoProcessingBinding(Queue videoProcessingQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoProcessingQueue).to(videoExchange).with(VIDEO_PROCESSING_ROUTING_KEY);
    }

    @Bean
    public Binding videoCutBinding(Queue videoCutQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoCutQueue).to(videoExchange).with(VIDEO_CUT_ROUTING_KEY);
    }

    @Bean
    public Binding videoResizeBinding(Queue videoResizeQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoResizeQueue).to(videoExchange).with(VIDEO_RESIZE_ROUTING_KEY);
    }

    @Bean
    public Binding videoConversionBinding(Queue videoConversionQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoConversionQueue).to(videoExchange).with(VIDEO_CONVERSION_ROUTING_KEY);
    }

    @Bean
    public Binding videoOverlayBinding(Queue videoOverlayQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoOverlayQueue).to(videoExchange).with(VIDEO_OVERLAY_ROUTING_KEY);
    }

    @Bean
    public Binding videoBatchProcessingBinding(Queue videoBatchProcessingQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoBatchProcessingQueue).to(videoExchange)
                .with(VIDEO_BATCH_PROCESSING_ROUTING_KEY);
    }

    @Bean
    public Binding videoDownloadBinding(Queue videoDownloadQueue, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoDownloadQueue).to(videoExchange).with(VIDEO_DOWNLOAD_ROUTING_KEY);
    }

    @Bean
    public Binding videoProcessingDLQBinding(Queue videoProcessingDLQ, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoProcessingDLQ).to(videoExchange).with(VIDEO_PROCESSING_DLQ);
    }

    @Bean
    public Binding videoCutDLQBinding(Queue videoCutDLQ, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoCutDLQ).to(videoExchange).with(VIDEO_CUT_DLQ);
    }

    @Bean
    public Binding videoResizeDLQBinding(Queue videoResizeDLQ, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoResizeDLQ).to(videoExchange).with(VIDEO_RESIZE_DLQ);
    }

    @Bean
    public Binding videoConversionDLQBinding(Queue videoConversionDLQ, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoConversionDLQ).to(videoExchange).with(VIDEO_CONVERSION_DLQ);
    }

    @Bean
    public Binding videoOverlayDLQBinding(Queue videoOverlayDLQ, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoOverlayDLQ).to(videoExchange).with(VIDEO_OVERLAY_DLQ);
    }

    @Bean
    public Binding videoBatchProcessingDLQBinding(Queue videoBatchProcessingDLQ, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoBatchProcessingDLQ).to(videoExchange).with(VIDEO_BATCH_PROCESSING_DLQ);
    }

    @Bean
    public Binding videoDownloadDLQBinding(Queue videoDownloadDLQ, TopicExchange videoExchange) {
        return BindingBuilder.bind(videoDownloadDLQ).to(videoExchange).with(VIDEO_DOWNLOAD_DLQ);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        return new RabbitTemplate(connectionFactory);
    }
}
