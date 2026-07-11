package com.steam.skin.scheduler.getupdates.service;

import com.steam.skin.scheduler.getupdates.entity.availableservers.SteamCMContainerResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SteamAvailableServersService {

    private final RestTemplate restTemplate;
    private static final String SERVERS_ENDPOINT = "https://api.steampowered.com/ISteamDirectory/GetCMList/v1/?cellid=0";

    public SteamAvailableServersService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<String> getWebSocketServers() {
        try {
            SteamCMContainerResponse responseContainer =
                    restTemplate.getForObject(
                            SERVERS_ENDPOINT,
                            SteamCMContainerResponse.class
                    );

            if (responseContainer != null) {
                return responseContainer.getResponse().getServerList().stream()
                        .map(server -> server.split(":")[0])
                        .map(host -> "wss://" + host + "/cmsocket/")
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Error getting list of servers: " + e.getMessage());
        }

        return Collections.emptyList();
    }

}
