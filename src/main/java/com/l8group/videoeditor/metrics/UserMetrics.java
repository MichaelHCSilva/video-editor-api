package com.l8group.videoeditor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class UserMetrics {

    private static final Logger logger = LoggerFactory.getLogger(UserMetrics.class);
    private final MeterRegistry meterRegistry;
    private Counter usersRegisteredSuccessfully;
    private Counter registrationErrors;

    public UserMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        logger.info("Inicializando UserMetrics e registrando contadores."); 
        usersRegisteredSuccessfully = meterRegistry.counter("user_registrations_success_total");
        registrationErrors = meterRegistry.counter("user_registration_errors_total");
    }

    public void incrementRegistrationSuccessCount() {
        usersRegisteredSuccessfully.increment();
        logger.debug("Incrementando user_registrations_success_total"); 
    }

    public void incrementRegistrationErrorCount() {
        registrationErrors.increment();
        logger.debug("Incrementando user_registration_errors_total"); 
    }

}