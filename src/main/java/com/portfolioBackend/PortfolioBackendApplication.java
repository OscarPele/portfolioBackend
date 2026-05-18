package com.portfolioBackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Punto de entrada de la aplicacion Spring Boot del portfolio.
 */
@SpringBootApplication
@EnableScheduling
public class PortfolioBackendApplication {

    /**
     * Arranca el contexto de Spring Boot.
     */
    public static void main(String[] args) {
        SpringApplication.run(PortfolioBackendApplication.class, args);
    }
}
