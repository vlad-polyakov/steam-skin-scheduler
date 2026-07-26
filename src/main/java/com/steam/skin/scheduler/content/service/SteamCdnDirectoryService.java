package com.steam.skin.scheduler.content.service;

import com.steam.skin.scheduler.content.entity.SteamCdnDirectoryResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SteamCdnDirectoryService {

    private static final String STEAM_PIPE_DIRECTORY_ENDPOINT =
            "https://api.steampowered.com/IContentServerDirectoryService/GetServersForSteamPipe/v1/?cellid=0";

    private static final String FALLBACK_CDN = "steampipe.akamaized.net";

    private final RestTemplate restTemplate;

    public SteamCdnDirectoryService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getBestCdnHost() {
        try {
            SteamCdnDirectoryResponse response = fetchCdnDirectory();
            if (response != null
                    && response.getResponse() != null
                    && response.getResponse().getServers() != null
                    && !response.getResponse().getServers().isEmpty()) {

                var server = response.getResponse().getServers().get(0);

                String host = server.getVhost() != null ? server.getVhost() : server.getHost();
                if (host != null && !host.isBlank()) {
                    return host;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch CDN directory from Steam API, using fallback: " + e.getMessage());
        }

        return FALLBACK_CDN;
    }

    public SteamCdnDirectoryResponse fetchCdnDirectory() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Valve/Steam HTTP Client 1.0");
        headers.set(HttpHeaders.ACCEPT, "application/json");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<SteamCdnDirectoryResponse> response = restTemplate.exchange(
                STEAM_PIPE_DIRECTORY_ENDPOINT,
                HttpMethod.GET,
                entity,
                SteamCdnDirectoryResponse.class
        );

        return response.getBody();
    }
}