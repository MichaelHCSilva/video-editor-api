package com.l8group.videoeditor.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync  // Habilita operações assíncronas no Spring
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        // Configuração do executor assíncrono com um pool de threads
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);  // Número de threads mínimas
        executor.setMaxPoolSize(50);   // Número máximo de threads
        executor.setQueueCapacity(100); // Capacidade da fila para tarefas pendentes
        executor.setThreadNamePrefix("VideoUploadExecutor-");
        executor.initialize();
        return executor;
    }
}
