package com.steam.skin.scheduler.connector;


import com.steam.skin.scheduler.entity.steam.auth.websocket.packet.SteamPacket;

public interface SteamClientConnector {

     void connect() throws Exception;

     void disconnect();

     void sendPacket(SteamPacket steamPacket);


}
