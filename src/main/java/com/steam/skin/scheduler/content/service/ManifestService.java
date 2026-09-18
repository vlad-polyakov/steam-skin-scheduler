package com.steam.skin.scheduler.content.service;

import com.google.protobuf.ByteString;
import com.steam.protobuf.ContentManifest;
import com.steam.skin.scheduler.getupdates.entity.websocket.packet.SteamContentContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
@RequiredArgsConstructor
public class ManifestService {
    private static final String DEPOT_ID = "2347770";

    private final RestTemplate restTemplate;
    private final SteamContentContext contentContext;
    private final SteamCdnDirectoryService steamCdnDirectoryService;

    public List<ContentManifest.ContentManifestPayload.FileMapping> downloadManifestPayload(String manifestId) throws Exception {
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
        var payload = parseManifestBytes(manifestBytes);
        return payload.getMappingsList();
    }



    private ContentManifest.ContentManifestPayload parseManifestBytes(byte[] manifestBytes) throws Exception {
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


    public List<ContentManifest.ContentManifestPayload.FileMapping> getFilesByDecryptedName(List<ContentManifest.ContentManifestPayload.FileMapping>payload, String filename) {
        return payload.stream().filter(mapping -> {
            try {
                return decryptFilename(mapping.getFilenameBytes()).contains(filename);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList());
    }

    private String decryptFilename(ByteString encryptedFilename) throws Exception {

        String base64 = encryptedFilename.toStringUtf8();
        base64 = base64.replaceAll("\\s+", "");
        byte[] encryptedBytes = Base64.getDecoder().decode(base64);

        if (encryptedBytes.length < 32) {
            throw new IllegalArgumentException("Filename data too short");
        }
        SecretKeySpec key = new SecretKeySpec(contentContext.getDepotKey(), "AES");
        Cipher ecb = Cipher.getInstance("AES/ECB/NoPadding");
        ecb.init(Cipher.DECRYPT_MODE, key);

        byte[] encryptedIV = Arrays.copyOfRange(encryptedBytes, 0, 16);
        byte[] iv = ecb.doFinal(encryptedIV);

        Cipher cbc = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cbc.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
        byte[] encryptedName = Arrays.copyOfRange(encryptedBytes, 16, encryptedBytes.length);
        byte[] decrypted = cbc.doFinal(encryptedName);
        int len = 0;
        while (len < decrypted.length && decrypted[len] != 0) {
            len++;
        }
        return new String(decrypted, 0, len, StandardCharsets.UTF_8);
    }

}
