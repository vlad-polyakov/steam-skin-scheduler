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
import com.steam.skin.scheduler.entity.steam.pics.ContentInfo;
import com.steam.skin.scheduler.entity.steam.pics.UpdateStatus;
import com.steam.skin.scheduler.packet.SteamPacketBuilder;
import com.steam.skin.scheduler.repository.CS2VersionRepository;
import com.steam.skin.scheduler.repository.DepotVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SteamClientService {

    private static final String DEPOT_ID = "731";
    private static final Pattern BUILD_ID_PATTERN =
            Pattern.compile("\"buildid\"\\s*\"(\\d+)\"");

    private static final Pattern MANIFEST_PATTERN =
            Pattern.compile(
                    "\"" + DEPOT_ID + "\"\\s*\\{.*?\"manifests\"\\s*\\{.*?\"public\"\\s*\\{.*?\"gid\"\\s*\"(\\d+)\"",
                    Pattern.DOTALL
            );

    @Autowired
    private SteamClientConnector steamClientConnector;

    @Autowired
    private SteamAuthService steamAuthService;

    @Autowired
    private SteamVersionsCheckService versionsCheckService;

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
            SteammessagesClientserverAppinfo.CMsgClientPICSProductInfoResponse response = SteammessagesClientserverAppinfo.CMsgClientPICSProductInfoResponse.parseFrom(packet.getBodyBytes());
            ContentInfo contentInfo = parsePICSResponseBuffer(response.getAppsList().get(0).getBuffer().toStringUtf8());
            UpdateStatus status = versionsCheckService.checkForUpdates(contentInfo.getGameBuildId(), contentInfo.getManifestId(), (int) Instant.now().getEpochSecond());
            disconnect();
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse PICS response", e);
        }
    }

    private ContentInfo parsePICSResponseBuffer(String buffer) {
        String buildId = null;
        String manifestId = null;

        Matcher buildMatcher = BUILD_ID_PATTERN.matcher(buffer);
        if (buildMatcher.find()) {
            buildId = buildMatcher.group(1);
        }

        Matcher manifestMatcher = MANIFEST_PATTERN.matcher(buffer);
        if (manifestMatcher.find()) {
            manifestId = manifestMatcher.group(1);
        }

        return ContentInfo.builder()
                .gameBuildId(buildId)
                .manifestId(manifestId)
                .build();
    }
}
