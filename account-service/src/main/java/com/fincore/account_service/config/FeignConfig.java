package com.fincore.account_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import feign.RequestInterceptor;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor requestInterceptor() {

        return requestTemplate -> {
            ServletRequestAttributes attributes = (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();

            if (attributes == null) { return; }

            String authorization =attributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);

            if (authorization != null) {
                requestTemplate.header(
                        HttpHeaders.AUTHORIZATION,
                        authorization
                );
            }
        };
    }
}