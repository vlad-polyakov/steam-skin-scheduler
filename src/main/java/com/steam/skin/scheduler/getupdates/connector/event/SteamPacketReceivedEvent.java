package com.steam.skin.scheduler.getupdates.connector.event;

import com.steam.skin.scheduler.getupdates.entity.websocket.packet.SteamPacket;
import lombok.Getter;

@Getter
public class SteamPacketReceivedEvent {
    private final SteamPacket packet;

    public SteamPacketReceivedEvent(SteamPacket packet) {
        this.packet = packet;
    }

}
