package com.oracle.orderapp.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@EnableConfigurationProperties(DownstreamServiceProperties.class)
public class RestClientConfiguration {
    private RestClient client(String url, DownstreamServiceProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.http().connectTimeout());
        factory.setReadTimeout(properties.http().readTimeout());
        return RestClient.builder().baseUrl(url).requestFactory(factory)
                .requestInterceptor((request, body, execution) -> {
                    String correlationId = currentCorrelationId();
                    if (correlationId != null) request.getHeaders().set(CorrelationIdFilter.HEADER, correlationId);
                    return execution.execute(request, body);
                }).build();
    }
    private String currentCorrelationId() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (!(attributes instanceof ServletRequestAttributes servlet)) return null;
        Object stored = servlet.getRequest().getAttribute(CorrelationIdFilter.ATTRIBUTE);
        if (stored instanceof String value && !value.isBlank()) return value;
        String header = servlet.getRequest().getHeader(CorrelationIdFilter.HEADER);
        return header == null || header.isBlank() ? null : header;
    }
    @Bean @Qualifier("userRestClient") RestClient user(DownstreamServiceProperties p){return client(p.services().user().baseUrl(),p);}
    @Bean @Qualifier("employeeRestClient") RestClient employee(DownstreamServiceProperties p){return client(p.services().employee().baseUrl(),p);}
    @Bean @Qualifier("productRestClient") RestClient products(DownstreamServiceProperties p){return client(p.services().products().baseUrl(),p);}
    @Bean @Qualifier("cartRestClient") RestClient cart(DownstreamServiceProperties p){return client(p.services().cart().baseUrl(),p);}
    @Bean @Qualifier("fundsRestClient") RestClient funds(DownstreamServiceProperties p){return client(p.services().funds().baseUrl(),p);}
}
