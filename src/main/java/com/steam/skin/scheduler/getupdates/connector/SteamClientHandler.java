package com.steam.skin.scheduler.getupdates.connector;


import com.steam.protobuf.EnumsClientserver;
import com.steam.skin.scheduler.getupdates.connector.event.SteamConnectedEvent;
import com.steam.skin.scheduler.getupdates.connector.event.SteamPacketReceivedEvent;
import com.steam.skin.scheduler.getupdates.entity.websocket.packet.SteamPacket;
import com.steam.skin.scheduler.getupdates.packet.SteamPacketProcessor;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;


public class SteamClientHandler extends SimpleChannelInboundHandler<SteamPacket> {

    private final ApplicationEventPublisher eventPublisher;

    public SteamClientHandler(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SteamPacket packet) throws Exception {
        if (packet.getEMsg() == EnumsClientserver.EMsg.k_EMsgMulti_VALUE) {
            List<SteamPacket> packets = SteamPacketProcessor.decodeMultiPacket(packet.getBodyBytes());
            for (SteamPacket p : packets) {
                eventPublisher.publishEvent(new SteamPacketReceivedEvent(p));
            }
        } else {
            eventPublisher.publishEvent(new SteamPacketReceivedEvent(packet));
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt == WebSocketClientProtocolHandler.ClientHandshakeStateEvent.HANDSHAKE_COMPLETE) {
            eventPublisher.publishEvent(new SteamConnectedEvent());
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
}




