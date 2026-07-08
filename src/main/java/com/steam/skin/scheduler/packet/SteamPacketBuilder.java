package com.steam.skin.scheduler.packet;

import com.steam.protobuf.*;
import com.steam.skin.scheduler.entity.steam.auth.websocket.packet.SteamPacket;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class SteamPacketBuilder {

    private static final int PROTOBUF_MASK = 0x80000000;
    private static final int PROTOCOL_VERSION = 65580;
    private static final int CLIENT_OS_TYPE = 16;
    private static final int CS2_APP_ID = 730;

    public static SteamPacket buildLogonPacket(String username, String token, long steamId) {
        SteammessagesClientserverLogin.CMsgClientLogon logonBody =
                SteammessagesClientserverLogin.CMsgClientLogon.newBuilder()
                        .setProtocolVersion(PROTOCOL_VERSION)
                        .setClientOsType(CLIENT_OS_TYPE)
                        .setAccessToken(token)
                        .setAccountName(username)
                        .setCellId(0)
                        .build();

        SteammessagesBase.CMsgProtoBufHeader header = SteammessagesBase.CMsgProtoBufHeader.newBuilder()
                .setClientSessionid(0)
                .setSteamid(steamId)
                .build();

        return new SteamPacket(EnumsClientserver.EMsg.k_EMsgClientLogon_VALUE, true, header, logonBody.toByteArray());
    }


    public static SteamPacket buildPICSPacket(int sessionId, long steamId) {

        SteammessagesClientserverAppinfo.CMsgClientPICSProductInfoRequest body = SteammessagesClientserverAppinfo.CMsgClientPICSProductInfoRequest.newBuilder()
                .addApps(SteammessagesClientserverAppinfo.CMsgClientPICSProductInfoRequest.AppInfo.newBuilder()
                .setAppid(CS2_APP_ID)
                .build())
                .setMetaDataOnly(false)
                .build();

        SteammessagesBase.CMsgProtoBufHeader header = SteammessagesBase.CMsgProtoBufHeader.newBuilder()
                .setClientSessionid(sessionId)
                .setSteamid(steamId)
                .build();

        return new SteamPacket(
                EnumsClientserver.EMsg.k_EMsgClientPICSProductInfoRequest_VALUE,
                true,
                header,
                body.toByteArray()
        );
    }

    public static BinaryWebSocketFrame toWebSocketFrame(SteamPacket packet) {
        int eMsgToSend = packet.getEMsg();
        if (packet.isProtobuf()) {
            eMsgToSend |= PROTOBUF_MASK;
        }

        byte[] headerBytes = (packet.getHeader() != null) ? packet.getHeader().toByteArray() : new byte[0];
        byte[] bodyBytes = (packet.getBodyBytes() != null) ? packet.getBodyBytes() : new byte[0];

        int totalSize = 4 + 4 + headerBytes.length + bodyBytes.length;
        ByteBuf buf = Unpooled.buffer(totalSize);
        buf.writeIntLE(eMsgToSend);
        buf.writeIntLE(headerBytes.length);

        if (headerBytes.length > 0) {
            buf.writeBytes(headerBytes);
        }

        if (bodyBytes.length > 0) {
            buf.writeBytes(bodyBytes);
        }

        return new BinaryWebSocketFrame(buf);
    }

    public static List<SteamPacket> decodeMultiPacket(byte[] multiBodyBytes) throws Exception {
        SteammessagesBase.CMsgMulti cMsgMulti =
                SteammessagesBase.CMsgMulti.parseFrom(multiBodyBytes);
        byte[] payload = cMsgMulti.getMessageBody().toByteArray();

        if (cMsgMulti.getSizeUnzipped() > 0) {
            try (GZIPInputStream gis = new GZIPInputStream(new ByteArrayInputStream(payload));
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                payload = baos.toByteArray();
            }
        }
        List<SteamPacket> extractedPackets = new ArrayList<>();
        ByteBuf buffer = Unpooled.wrappedBuffer(payload);
        try {
            while (buffer.readableBytes() >= 4) {

                int packetSize = buffer.readIntLE();
                if (buffer.readableBytes() < packetSize) {
                    break;
                }
                ByteBuf singlePacketBuf = buffer.readSlice(packetSize);

                int rawEMsg = singlePacketBuf.readIntLE();
                int eMsg = rawEMsg & ~0x80000000;
                boolean isProtobuf = (rawEMsg & 0x80000000) != 0;

                int headerLen = singlePacketBuf.readIntLE();

                byte[] headerBytes;
                if (isProtobuf) {
                    if (headerLen > 0 && singlePacketBuf.readableBytes() >= headerLen) {
                        headerBytes = new byte[headerLen];
                        singlePacketBuf.readBytes(headerBytes);
                    } else {
                        headerBytes = new byte[0];
                    }
                } else {
                    int legacyHeaderSize = Math.min(singlePacketBuf.readableBytes(), 36);
                    if (legacyHeaderSize > 0) {
                        headerBytes = new byte[legacyHeaderSize];
                        singlePacketBuf.readBytes(headerBytes);
                    } else {
                        headerBytes = new byte[0];
                    }
                }

                byte[] bodyBytes = new byte[singlePacketBuf.readableBytes()];
                singlePacketBuf.readBytes(bodyBytes);

                SteammessagesBase.CMsgProtoBufHeader header = null;
                if (isProtobuf && headerBytes.length > 0) {
                    try {
                        header = SteammessagesBase.CMsgProtoBufHeader.parseFrom(headerBytes);
                    } catch (Exception e) {
                        header = null;
                    }
                }
                extractedPackets.add(new SteamPacket(eMsg, isProtobuf, header, bodyBytes));
            }
        } finally {
            buffer.release();
        }

        return extractedPackets;
    }

}
