package com.steam.skin.scheduler.content.service;

import com.google.protobuf.ByteString;
import com.steam.skin.scheduler.getupdates.entity.websocket.packet.SteamContentContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;

@Service
public class FileChunksService {

    private static final String DEPOT_ID = "2347770";
    private final RestTemplate restTemplate;


    @Autowired
    private SteamCdnDirectoryService steamCdnDirectoryService;

    @Autowired
    private SteamContentContext steamContentContext;

    public FileChunksService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.connectTimeout(Duration.ofSeconds(10)).readTimeout(Duration.ofSeconds(15))
                .build();
    }




    public byte[] downloadChunk(ByteString chunkSha, int expectedSize) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Valve/Steam HTTP Client 1.0");
        headers.set(HttpHeaders.ACCEPT, "*/*");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String shaHex = bytesToHex(chunkSha.toByteArray());

        String url = String.format(
                "https://%s/depot/%s/chunk/%s",
                steamCdnDirectoryService.getBestCdnHost(),
                DEPOT_ID,
                shaHex
        );

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class
        );

        return response.getBody();
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);

        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }

        return sb.toString();
    }



}
