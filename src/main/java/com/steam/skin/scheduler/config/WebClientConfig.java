package com.steam.skin.scheduler.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient contentWebClient() {
        ConnectionProvider provider = ConnectionProvider.builder("steam-content")
                .maxConnections(8)
                .pendingAcquireMaxCount(32)
                .maxIdleTime(Duration.ofSeconds(30))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .responseTimeout(Duration.ofSeconds(30));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.USER_AGENT, "Valve/Steam HTTP Client 1.0")
                .defaultHeader(HttpHeaders.ACCEPT, "*/*")
                .codecs(configurer ->
                        configurer.defaultCodecs()
                                .maxInMemorySize(16 * 1024 * 1024)
                )
                .build();
    }
}
