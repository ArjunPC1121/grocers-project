package com.oracle.supportassistantapp.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BackendDotenvConfig {
    @Bean
    Dotenv backendDotenv() {
        return Dotenv.configure().directory("../").ignoreIfMalformed().ignoreIfMissing().load();
    }
}
