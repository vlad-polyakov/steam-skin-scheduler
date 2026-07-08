package com.steam.skin.scheduler.packet;


import com.steam.protobuf.SteammessagesBase;
import com.steam.skin.scheduler.entity.steam.auth.websocket.packet.SteamPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;


import java.util.List;

public class SteamWebSocketPacketDecoder extends MessageToMessageDecoder<BinaryWebSocketFrame> {
    private static final int PROTO_MASK = 0x80000000;

    @Override
    protected void decode(ChannelHandlerContext ctx, BinaryWebSocketFrame frame, List<Object> out) throws Exception {
        ByteBuf in = frame.content();

        if (in.readableBytes() < 4) return;

        int rawEMsg = in.readIntLE();
        boolean isProtobuf = (rawEMsg & PROTO_MASK) != 0;
        int eMsg = isProtobuf ? (rawEMsg & ~PROTO_MASK) : rawEMsg;

        SteammessagesBase.CMsgProtoBufHeader header = null;

        if (isProtobuf) {
            if (in.readableBytes() < 4) return;
            int headerLength = in.readIntLE();

            if (headerLength > 0) {
                if (in.readableBytes() < headerLength) return;

                byte[] headerBytes = new byte[headerLength];
                in.readBytes(headerBytes);
                header = SteammessagesBase.CMsgProtoBufHeader.parseFrom(headerBytes);
            }
        }
        byte[] bodyBytes = new byte[in.readableBytes()];
        in.readBytes(bodyBytes);

        out.add(new SteamPacket(eMsg, isProtobuf, header, bodyBytes));
    }
}