package com.steam.skin.scheduler.content.service;

import SevenZip.Compression.LZMA.Decoder;
import com.github.luben.zstd.Zstd;
import com.steam.protobuf.ContentManifest;
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
import java.util.List;
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

    public byte[] downloadChunk(ContentManifest.ContentManifestPayload.FileMapping fileMapping, String depotId) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.USER_AGENT, "Valve/Steam HTTP Client 1.0");
        headers.set(HttpHeaders.ACCEPT, "*/*");

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        List<ContentManifest.ContentManifestPayload.FileMapping.ChunkData> chunkList = fileMapping.getChunksList();
        byte[] decodedData = new byte[calculateChunksSize(chunkList)];
        int offset = 0;
        for(ContentManifest.ContentManifestPayload.FileMapping.ChunkData chunkData: chunkList) {
            String shaHex = bytesToHex(chunkData.getSha().toByteArray());
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
            byte[] decodedResponse = decodeChunk(response.getBody(), chunkData.getCbOriginal(), Integer.toUnsignedLong(chunkData.getCrc()));
            offset = appendChunk(decodedData, decodedResponse, offset);
        }
        return decodedData;
    }

    private int calculateChunksSize(List<ContentManifest.ContentManifestPayload.FileMapping.ChunkData> chunkList) {
        int size = 0;
        for(ContentManifest.ContentManifestPayload.FileMapping.ChunkData chunk: chunkList) {
            size += chunk.getCbOriginal();
        }
        return size;
    }

    private int appendChunk(byte[] target, byte[] chunk, int offset) {
        System.arraycopy(chunk, 0, target, offset, chunk.length);
        return offset + chunk.length;
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

        byte[] result;

        if (isVza(decrypted)) {
            result = decompressVza(
                    decrypted,
                    expectedUncompressedSize
            );
        } else if (isVsza(decrypted)) {
            result = decompressVsza(
                    decrypted,
                    expectedUncompressedSize
            );
        } else {
            throw new IOException(
                    "Unknown chunk compression format: "
                            + bytesToHex(
                            Arrays.copyOf(
                                    decrypted,
                                    Math.min(16, decrypted.length)
                            )
                    )
            );
        }

        if (result.length != expectedUncompressedSize) {
            throw new IllegalStateException(
                    "Invalid decompressed size: actual="
                            + result.length
                            + ", expected="
                            + expectedUncompressedSize
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

    private boolean isVza(byte[] data) {
        return data.length >= 3
                && data[0] == 'V'
                && data[1] == 'Z'
                && data[2] == 'a';
    }

    private boolean isVsza(byte[] data) {
        return data.length >= 4
                && data[0] == 'V'
                && data[1] == 'S'
                && data[2] == 'Z'
                && data[3] == 'a';
    }

    private byte[] decompressVsza(
            byte[] data,
            int expectedSize
    ) throws IOException {

        final int HEADER_LENGTH = 8;
        final int FOOTER_LENGTH = 15;

        if (data == null) {
            throw new IOException("VSZa data is null");
        }

        if (data.length < HEADER_LENGTH + FOOTER_LENGTH) {
            throw new IOException(
                    "VSZa data is too short: " + data.length
            );
        }

        if (data[0] != 'V'
                || data[1] != 'S'
                || data[2] != 'Z'
                || data[3] != 'a') {

            throw new IOException(
                    "Invalid VSZa header: "
                            + bytesToHex(
                            Arrays.copyOf(
                                    data,
                                    Math.min(16, data.length)
                            )
                    )
            );
        }

        long headerCrc = readUInt32LE(data, 4);

        int footerOffset = data.length - FOOTER_LENGTH;

        long footerCrc = readUInt32LE(
                data,
                footerOffset
        );

        long footerSize = readUInt32LE(
                data,
                footerOffset + 4
        );
        int tailOffset = data.length - 3;

        if (data[tailOffset] != 'z'
                || data[tailOffset + 1] != 's'
                || data[tailOffset + 2] != 'v') {

            throw new IOException(
                    "Invalid VSZa footer tail: "
                            + bytesToHex(
                            Arrays.copyOfRange(
                                    data,
                                    Math.max(0, data.length - 32),
                                    data.length
                            )
                    )
            );
        }
        if (footerSize
                != Integer.toUnsignedLong(expectedSize)) {

            throw new IOException(
                    "VSZa size mismatch: footer="
                            + Long.toUnsignedString(footerSize)
                            + ", manifest="
                            + Integer.toUnsignedLong(expectedSize)
            );
        }
        int zstdOffset = HEADER_LENGTH;

        int zstdLength = footerOffset - zstdOffset;

        if (zstdLength <= 0) {
            throw new IOException(
                    "VSZa Zstd payload is empty"
            );
        }

        byte[] zstdData = Arrays.copyOfRange(
                data,
                zstdOffset,
                footerOffset
        );

        byte[] result = new byte[expectedSize];
        long decodedSize;
        try {
            decodedSize = Zstd.decompress(
                    result,
                    zstdData
            );
        } catch (Exception e) {
            throw new IOException(
                    "Failed to decompress VSZa Zstd payload",
                    e
            );
        }
        if (Zstd.isError(decodedSize)) {
            throw new IOException(
                    "Zstd decompression failed: "
                            + Zstd.getErrorName(decodedSize)
            );
        }
        if (decodedSize
                != Integer.toUnsignedLong(expectedSize)) {

            throw new IOException(
                    "Invalid decompressed size: actual="
                            + Long.toUnsignedString(decodedSize)
                            + ", expected="
                            + Integer.toUnsignedLong(expectedSize)
            );
        }

        CRC32 crc32 = new CRC32();
        crc32.update(result);

        long actualCrc = crc32.getValue();
        if (actualCrc != headerCrc) {

            throw new IOException(
                    "VSZa CRC mismatch: actual="
                            + Long.toUnsignedString(actualCrc)
                            + ", header="
                            + Long.toUnsignedString(headerCrc)
            );
        }

        if (footerCrc != 0
                && actualCrc != footerCrc) {

            throw new IOException(
                    "VSZa footer CRC mismatch: actual="
                            + Long.toUnsignedString(actualCrc)
                            + ", footer="
                            + Long.toUnsignedString(footerCrc)
            );
        }

        return result;
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
