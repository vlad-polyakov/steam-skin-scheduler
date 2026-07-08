package com.steam.skin.scheduler.service;

import com.steam.protobuf.EnumsClientserver;
import com.steam.protobuf.SteammessagesClientserver;
import com.steam.protobuf.SteammessagesClientserverAppinfo;
import com.steam.protobuf.SteammessagesClientserverLogin;
import com.steam.skin.scheduler.connector.SteamClientConnector;
import com.steam.skin.scheduler.connector.SteamConnectedEvent;
import com.steam.skin.scheduler.connector.SteamPacketReceivedEvent;
import com.steam.skin.scheduler.entity.steam.auth.websocket.packet.SteamPacket;
import com.steam.skin.scheduler.entity.steam.auth.websocket.session.SteamCMSession;
import com.steam.skin.scheduler.packet.SteamPacketBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class SteamClientService {

    @Autowired
    private SteamClientConnector steamClientConnector;

    @Autowired
    private SteamAuthService steamAuthService;

    private String username;
    private String token;
    private long steamId;
    private SteamCMSession steamSession;

    public void connect() throws Exception {
        steamClientConnector.connect();
    }

    @EventListener
    public void onSteamConnected(SteamConnectedEvent event) {
        login();
    }

    public void login() {
        SteamPacket packet = SteamPacketBuilder.buildLogonPacket(username, token, steamId);
        steamClientConnector.sendPacket(packet);
    }

    public void requestPics() {
        SteamPacket packet = SteamPacketBuilder.buildPICSPacket(steamSession.getSessionId(), steamSession.getSteamId());
        steamClientConnector.sendPacket(packet);
    }

    public void disconnect() {
        steamClientConnector.disconnect();
    }

    public void prepareLogin(String username, String token, long steamId) {
        this.username = username;
        this.token = token;
        this.steamId = steamId;
    }

    @EventListener
    public void onPacketReceived(SteamPacketReceivedEvent event) {
        SteamPacket packet = event.getPacket();
        switch (packet.getEMsg()) {
            case EnumsClientserver.EMsg.k_EMsgClientLogOnResponse_VALUE -> handleLogonResponse(packet);
            case EnumsClientserver.EMsg.k_EMsgClientLicenseList_VALUE -> handleLicenseList(packet);
            case EnumsClientserver.EMsg.k_EMsgClientPICSProductInfoResponse_VALUE -> handleProductInfoResponse(packet);
        }
    }

    private void handleLogonResponse(SteamPacket packet) {
        try {
            byte[] body = packet.getBodyBytes();
            var response = SteammessagesClientserverLogin.CMsgClientLogonResponse.parseFrom(body);

            int result = response.getEresult();
            if (result != 1) {
                throw new RuntimeException("Steam login failed: " + result);
            }

            int sessionId = packet.getHeader().getClientSessionid();
            long targetSteamId = packet.getHeader().getSteamid();
            this.steamSession = new SteamCMSession(sessionId, targetSteamId);
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse LogonResponse", e);
        }
    }

    private void handleLicenseList(SteamPacket packet) {
        try {
            SteammessagesClientserver.CMsgClientLicenseList.parseFrom(packet.getBodyBytes());
            requestPics();
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse LicenseList", e);
        }
    }

    private void handleProductInfoResponse(SteamPacket packet) {
        try {
            var response = SteammessagesClientserverAppinfo.CMsgClientPICSProductInfoResponse.parseFrom(packet.getBodyBytes());
            // TODO database processing: write version into db and compare PICS versions with database versions
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse PICS response", e);
        }
    }
}
