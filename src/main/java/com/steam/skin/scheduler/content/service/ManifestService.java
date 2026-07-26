package com.steam.skin.scheduler.content.service;

import com.steam.protobuf.ContentManifest;
import com.steam.skin.scheduler.getupdates.entity.websocket.packet.SteamContentContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;


import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Duration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ManifestService {
    private static final String DEPOT_ID = "2347770";
    private final RestTemplate restTemplate;

    @Autowired
    private SteamContentContext contentContext;

    @Autowired
    private SteamCdnDirectoryService steamCdnDirectoryService;

    public ManifestService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.connectTimeout(Duration.ofSeconds(10)).readTimeout(Duration.ofSeconds(15))
                .build();
    }

    public ContentManifest.ContentManifestPayload downloadManifestPayload(String manifestId) throws Exception {
        String cdnHost = steamCdnDirectoryService.getBestCdnHost();

        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Valve/Steam HTTP Client 1.0");
        headers.set(HttpHeaders.ACCEPT, "*/*");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String url = String.format(
                "https://%s/depot/%s/manifest/%s/5/%s",
                cdnHost,
                DEPOT_ID,
                contentContext.getDepotVersion(),
                Long.toUnsignedString(contentContext.getManifestCode())
        );
        ResponseEntity<byte[]> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                byte[].class
        );
        byte[] manifestBytes = response.getBody();
        return parseManifestBytes(manifestBytes);
    }



    public ContentManifest.ContentManifestPayload parseManifestBytes(byte[] manifestBytes) throws Exception {
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(manifestBytes));

        ZipEntry entry;
        byte[] unzipped = new byte[0];
        while ((entry = zis.getNextEntry()) != null) {
            unzipped = zis.readAllBytes();
        }
        ByteBuffer buffer = ByteBuffer.wrap(unzipped)
                .order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() >= 8) {
            int magic = buffer.getInt();
            int length = buffer.getInt();
            byte[] section = new byte[length];
            buffer.get(section);
            if(magic == 0x71F617D0) {
                ContentManifest.ContentManifestPayload payload =
                        ContentManifest.ContentManifestPayload.parseFrom(section);
                return payload;
            }
            else {
                throw new IllegalStateException(
                            "Unknown manifest section: 0x"
                                    + Integer.toHexString(magic));
            }
        }
        return null;
    }
}
