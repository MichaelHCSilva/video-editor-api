package com.l8group.videoeditor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);  // Número mínimo de threads
        executor.setMaxPoolSize(50);   // Número máximo de threads
        executor.setQueueCapacity(500); // Tamanho da fila de tarefas
        executor.setThreadNamePrefix("VideoUpload-");
        executor.initialize();
        return executor;
    }
}
