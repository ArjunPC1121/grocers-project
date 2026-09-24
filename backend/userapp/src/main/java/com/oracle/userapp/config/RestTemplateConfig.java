/*
Used to make api calls.
 */

package com.oracle.userapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    // Makes one HTTP client available for calls to other backend services.
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
