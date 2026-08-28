package com.steam.skin.scheduler.content.service;

import SevenZip.Compression.LZMA.Decoder;
import com.google.protobuf.ByteString;
import com.steam.skin.scheduler.getupdates.entity.websocket.packet.SteamContentContext;
import jakarta.annotation.Nonnull;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.zip.CRC32;

@Service
public class FileChunksService {



    private final RestTemplate restTemplate;

    @Autowired
    private SteamCdnDirectoryService steamCdnDirectoryService;

    @Autowired
    private SteamContentContext steamContentContext;

    public FileChunksService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(15))
                .build();
    }

    public byte[] downloadChunk(ByteString chunkSha, String depotId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Valve/Steam HTTP Client 1.0");
        headers.set(HttpHeaders.ACCEPT, "*/*");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        String shaHex = bytesToHex(chunkSha.toByteArray());
        String url = String.format(
                "https://%s/depot/%s/chunk/%s",
                steamCdnDirectoryService.getBestCdnHost(),
                depotId,
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

    public byte[] decodeChunk(
            byte[] encryptedChunk,
            int expectedUncompressedSize,
            long expectedChecksum
    ) throws Exception {
        byte[] decrypted = decryptChunk(encryptedChunk);
        byte[] result = decompressVza(decrypted, expectedUncompressedSize);
        if (result.length != expectedUncompressedSize) {
            throw new IllegalStateException(
                    "Invalid decompressed size: actual=" + result.length
                            + ", expected=" + expectedUncompressedSize
            );
        }
        long actualChecksum = steamAdler32(result);
        if (actualChecksum != expectedChecksum) {
            throw new IllegalStateException(
                    "Invalid chunk checksum: actual="
                            + Long.toUnsignedString(actualChecksum)
                            + ", expected="
                            + Long.toUnsignedString(expectedChecksum)
            );
        }
        System.out.println(bytesToHex(result));
        return result;
    }

    private long steamAdler32(byte[] data) {
        final int MOD = 65521;
        long a = 0;
        long b = 0;

        for (byte value : data) {
            a += value & 0xFF;
            a %= MOD;
            b += a;
            b %= MOD;
        }

        return (b << 16) | a;
    }

    private byte[] decryptChunk(byte[] encrypted) throws Exception {
        SecretKeySpec key = getKey(encrypted);
        byte[] encryptedIv = Arrays.copyOfRange(encrypted, 0, 16);

        Cipher ecb = Cipher.getInstance("AES/ECB/NoPadding");
        ecb.init(Cipher.DECRYPT_MODE, key);

        byte[] iv = ecb.doFinal(encryptedIv);
        byte[] encryptedData = Arrays.copyOfRange(encrypted, 16, encrypted.length);

        Cipher cbc = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cbc.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        return cbc.doFinal(encryptedData);
    }

    @Nonnull
    private SecretKeySpec getKey(byte[] encrypted) {
        if (encrypted == null || encrypted.length < 32) {
            throw new IllegalArgumentException(
                    "Chunk is too small: " + (encrypted == null ? 0 : encrypted.length)
            );
        }

        byte[] depotKey = steamContentContext.getDepotKey();

        if (depotKey == null || depotKey.length != 32) {
            throw new IllegalStateException(
                    "Depot key must be exactly 32 bytes"
            );
        }

        SecretKeySpec key = new SecretKeySpec(depotKey, "AES");
        return key;
    }

    private byte[] decompressVza(byte[] data, int expectedSize) throws IOException {
        final int HEADER_LENGTH = 12;
        final int FOOTER_LENGTH = 10;

        if (data == null) {
            throw new IOException("VZa data is null");
        }

        if (data.length < HEADER_LENGTH + FOOTER_LENGTH) {
            throw new IOException("VZa data is too short: " + data.length);
        }

        if (data[0] != 'V' || data[1] != 'Z' || data[2] != 'a') {
            throw new IOException(
                    "Invalid VZa header: "
                            + bytesToHex(Arrays.copyOf(data, Math.min(16, data.length)))
            );
        }

        byte[] properties = Arrays.copyOfRange(data, 7, 12);

        int compressedOffset = HEADER_LENGTH;
        int footerOffset = data.length - FOOTER_LENGTH;
        int compressedLength = footerOffset - compressedOffset;

        if (compressedLength <= 0) {
            throw new IOException("VZa compressed stream is empty");
        }

        long expectedCrc = readUInt32LE(data, footerOffset);
        long footerSize = readUInt32LE(data, footerOffset + 4);

        if (data[footerOffset + 8] != 'z' || data[footerOffset + 9] != 'v') {
            throw new IOException(
                    "Invalid VZa footer: "
                            + bytesToHex(Arrays.copyOfRange(data, footerOffset, data.length))
            );
        }

        if (footerSize != Integer.toUnsignedLong(expectedSize)) {
            throw new IOException(
                    "VZa size mismatch: footer=" + footerSize
                            + ", manifest=" + Integer.toUnsignedLong(expectedSize)
            );
        }

        ByteArrayInputStream input = new ByteArrayInputStream(
                data,
                compressedOffset,
                compressedLength
        );

        ByteArrayOutputStream output = new ByteArrayOutputStream(expectedSize);

        Decoder decoder = new Decoder();

        if (!decoder.SetDecoderProperties(properties)) {
            throw new IOException(
                    "Invalid LZMA properties: " + bytesToHex(properties)
            );
        }

        boolean decoded = decoder.Code(input, output, expectedSize);

        if (!decoded) {
            throw new IOException("LZMA decoder failed");
        }

        byte[] result = output.toByteArray();

        if (result.length != expectedSize) {
            throw new IOException(
                    "Invalid decompressed size: actual=" + result.length
                            + ", expected=" + expectedSize
            );
        }

        CRC32 crc32 = new CRC32();
        crc32.update(result);

        long actualCrc = crc32.getValue();

        if (actualCrc != expectedCrc) {
            throw new IOException(
                    "VZa CRC mismatch: actual="
                            + Long.toUnsignedString(actualCrc)
                            + ", expected="
                            + Long.toUnsignedString(expectedCrc)
            );
        }

        return result;
    }

    private long readUInt32LE(byte[] data, int offset) {
        return ((long) data[offset] & 0xFF)
                | (((long) data[offset + 1] & 0xFF) << 8)
                | (((long) data[offset + 2] & 0xFF) << 16)
                | (((long) data[offset + 3] & 0xFF) << 24);
    }
}
