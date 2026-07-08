package com.steam.skin.scheduler.connector;

import com.steam.skin.scheduler.entity.steam.auth.websocket.packet.SteamPacket;
import lombok.Getter;

@Getter
public class SteamPacketReceivedEvent {
    private final SteamPacket packet;

    public SteamPacketReceivedEvent(SteamPacket packet) {
        this.packet = packet;
    }

}
