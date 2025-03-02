package com.l8group.videoeditor.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String VIDEO_EXCHANGE = "video.exchange";
    public static final String VIDEO_UPLOAD_QUEUE = "video.upload.queue";
    public static final String VIDEO_UPLOAD_ROUTING_KEY = "video.upload.routing";

    @Bean
    public Queue videoUploadQueue() {
        return new Queue(VIDEO_UPLOAD_QUEUE, true);
    }

    @Bean
    public DirectExchange videoExchange() {
        return new DirectExchange(VIDEO_EXCHANGE);
    }

    @Bean
    public Binding videoUploadBinding(Queue videoUploadQueue, DirectExchange videoExchange) {
        return BindingBuilder.bind(videoUploadQueue).to(videoExchange).with(VIDEO_UPLOAD_ROUTING_KEY);
    }
}
