package com.steam.skin.scheduler.content.util.vpk;

import com.steam.skin.scheduler.content.entity.vpk.VpkEntry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class VpkReader {
    public static List<VpkEntry> readDirectory(byte[] vpk) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(vpk)
                .order(ByteOrder.LITTLE_ENDIAN);

        int magic = buffer.getInt();
        if (magic != 0x55AA1234) {
            throw new IOException("Not a VPK file");
        }

        int version = buffer.getInt();
        if (version != 2) {
            throw new IOException("Unsupported VPK version: " + version);
        }

        long treeSize = Integer.toUnsignedLong(buffer.getInt());
        long fileDataSectionSize = Integer.toUnsignedLong(buffer.getInt());
        long archiveMd5SectionSize = Integer.toUnsignedLong(buffer.getInt());
        long otherMd5SectionSize = Integer.toUnsignedLong(buffer.getInt());
        long signatureSectionSize = Integer.toUnsignedLong(buffer.getInt());
        int treeStart = buffer.position();
        int treeEnd = treeStart + Math.toIntExact(treeSize);

        List<VpkEntry> result = new ArrayList<>();

        while (buffer.position() < treeEnd) {

            String extension = readNullTerminatedString(buffer);

            if (extension.isEmpty()) {
                break;
            }

            while (true) {

                String path = readNullTerminatedString(buffer);

                if (path.isEmpty()) {
                    break;
                }

                while (true) {

                    String filename = readNullTerminatedString(buffer);

                    if (filename.isEmpty()) {
                        break;
                    }

                    long crc32 = Integer.toUnsignedLong(buffer.getInt());
                    int preloadBytes = Short.toUnsignedInt(buffer.getShort());
                    int archiveIndex = Short.toUnsignedInt(buffer.getShort());
                    long offset = Integer.toUnsignedLong(buffer.getInt());
                    long length = Integer.toUnsignedLong(buffer.getInt());

                    int terminator = Short.toUnsignedInt(buffer.getShort());

                    if (terminator != 0xFFFF) {
                        throw new IOException("Invalid VPK entry terminator");
                    }

                    result.add(new VpkEntry(
                            path,
                            filename,
                            extension,
                            crc32,
                            preloadBytes,
                            archiveIndex,
                            offset,
                            length
                    ));
                }
            }
        }

        return result;
    }

    private static String readNullTerminatedString(ByteBuffer buffer) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (buffer.hasRemaining()) {
            byte b = buffer.get();

            if (b == 0) {
                break;
            }

            out.write(b);
        }

        return out.toString(StandardCharsets.UTF_8);
    }
}
