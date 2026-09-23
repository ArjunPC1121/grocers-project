package com.oracle.assistantapp.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Loads local development secrets from backend/.env, one directory above Assistant App. */
@Configuration
public class BackendDotenvConfig {

    @Bean
    Dotenv backendDotenv() {
        return Dotenv.configure()
                .directory("..")
                .filename(".env")
                .ignoreIfMissing()
                .load();
    }
}
