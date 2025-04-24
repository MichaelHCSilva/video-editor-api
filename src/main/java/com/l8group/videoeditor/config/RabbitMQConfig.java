package com.l8group.videoeditor.config;

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

    // Video Processing
    public static final String VIDEO_PROCESSING_QUEUE = "video.processing.queue";
    public static final String VIDEO_PROCESSING_ROUTING_KEY = "video.process";

    // Video Cut
    public static final String VIDEO_CUT_QUEUE = "video.cut.queue";
    public static final String VIDEO_CUT_ROUTING_KEY = "video.cut.routing.key";

    // Video Resize
    public static final String VIDEO_RESIZE_QUEUE = "video.resize.queue";
    public static final String VIDEO_RESIZE_ROUTING_KEY = "video.resize.routing.key";

    // Video Conversion
    public static final String VIDEO_CONVERSION_QUEUE = "video.conversion.queue";
    public static final String VIDEO_CONVERSION_ROUTING_KEY = "video.conversion.routing.key";

    // Video Overlay
    public static final String VIDEO_OVERLAY_QUEUE = "video.overlay.queue";
    public static final String VIDEO_OVERLAY_ROUTING_KEY = "video.overlay.routing.key";

    // Video Batch Processing
    public static final String VIDEO_BATCH_PROCESSING_QUEUE = "video.batch.processing.queue";
    public static final String VIDEO_BATCH_PROCESSING_ROUTING_KEY = "video.batch.process";

    // Queues
    @Bean public Queue videoProcessingQueue() { return new Queue(VIDEO_PROCESSING_QUEUE, true); }
    @Bean public Queue videoCutQueue() { return new Queue(VIDEO_CUT_QUEUE, true); }
    @Bean public Queue videoResizeQueue() { return new Queue(VIDEO_RESIZE_QUEUE, true); }
    @Bean public Queue videoConversionQueue() { return new Queue(VIDEO_CONVERSION_QUEUE, true); }
    @Bean public Queue videoOverlayQueue() { return new Queue(VIDEO_OVERLAY_QUEUE, true); }
    @Bean public Queue videoBatchProcessingQueue() { return new Queue(VIDEO_BATCH_PROCESSING_QUEUE, true); }

    // Exchange
    @Bean public TopicExchange videoExchange() { return new TopicExchange(VIDEO_EXCHANGE); }

    // Bindings
    @Bean public Binding videoProcessingBinding(Queue videoProcessingQueue, TopicExchange videoExchange) { return BindingBuilder.bind(videoProcessingQueue).to(videoExchange).with(VIDEO_PROCESSING_ROUTING_KEY); }
    @Bean public Binding videoCutBinding(Queue videoCutQueue, TopicExchange videoExchange) { return BindingBuilder.bind(videoCutQueue).to(videoExchange).with(VIDEO_CUT_ROUTING_KEY); }
    @Bean public Binding videoResizeBinding(Queue videoResizeQueue, TopicExchange videoExchange) { return BindingBuilder.bind(videoResizeQueue).to(videoExchange).with(VIDEO_RESIZE_ROUTING_KEY); }
    @Bean public Binding videoConversionBinding(Queue videoConversionQueue, TopicExchange videoExchange) { return BindingBuilder.bind(videoConversionQueue).to(videoExchange).with(VIDEO_CONVERSION_ROUTING_KEY); }
    @Bean public Binding videoOverlayBinding(Queue videoOverlayQueue, TopicExchange videoExchange) { return BindingBuilder.bind(videoOverlayQueue).to(videoExchange).with(VIDEO_OVERLAY_ROUTING_KEY); }
    @Bean public Binding videoBatchProcessingBinding(Queue videoBatchProcessingQueue, TopicExchange videoExchange) { return BindingBuilder.bind(videoBatchProcessingQueue).to(videoExchange).with(VIDEO_BATCH_PROCESSING_ROUTING_KEY); }

    // Rabbit Template
    @Bean public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) { return new RabbitTemplate(connectionFactory); }
}