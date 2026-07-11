package com.steam.skin.scheduler.getupdates.connector;


import com.steam.skin.scheduler.getupdates.entity.websocket.packet.SteamPacket;

public interface SteamClientConnector {

     void connect() throws Exception;

     void disconnect();

     void sendPacket(SteamPacket steamPacket);


}
