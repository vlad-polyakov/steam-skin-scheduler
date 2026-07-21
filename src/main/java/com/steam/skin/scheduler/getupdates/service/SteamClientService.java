package com.steam.skin.scheduler.getupdates.service;

import com.steam.protobuf.EnumsClientserver;
import com.steam.skin.scheduler.getupdates.connector.SteamClientConnector;
import com.steam.skin.scheduler.getupdates.connector.event.SteamConnectedEvent;
import com.steam.skin.scheduler.getupdates.connector.event.SteamPacketReceivedEvent;
import com.steam.skin.scheduler.getupdates.entity.pics.ContentInfo;
import com.steam.skin.scheduler.getupdates.entity.pics.UpdateStatus;
import com.steam.skin.scheduler.getupdates.entity.websocket.packet.SteamPacket;
import com.steam.skin.scheduler.getupdates.entity.websocket.session.SteamCMSession;
import com.steam.skin.scheduler.getupdates.packet.SteamPacketProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SteamClientService {


    @Autowired
    private SteamClientConnector steamClientConnector;

    @Autowired
    private SteamVersionsCheckService versionsCheckService;

    private String username;
    private String token;
    private long steamId;
    private SteamCMSession steamSession;
    private final Map<Long, CompletableFuture<UpdateStatus>> pendingRequests = new ConcurrentHashMap<>();


    private static final String DEPOT_ID = "2347770";
    private static final Pattern BUILD_ID_PATTERN =
            Pattern.compile("\"buildid\"\\s*\"(\\d+)\"");

    private static final Pattern MANIFEST_PATTERN =
            Pattern.compile(
                    "\"" + DEPOT_ID + "\"\\s*\\{.*?\"manifests\"\\s*\\{.*?\"public\"\\s*\\{.*?\"gid\"\\s*\"(\\d+)\"",
                    Pattern.DOTALL
            );


    public CompletableFuture<UpdateStatus> connect(long steamId) throws Exception {
        CompletableFuture<UpdateStatus> future = new CompletableFuture<>();
        pendingRequests.put(steamId, future);
        future.orTimeout(30, TimeUnit.SECONDS)
                .whenComplete((res, ex) -> pendingRequests.remove(steamId));
        steamClientConnector.connect();
        return future;
    }

    @EventListener
    public void onSteamConnected(SteamConnectedEvent event) {
        login();
    }

    public void login() {
        SteamPacket packet = SteamPacketProcessor.buildLogonPacket(username, token, steamId);
        steamClientConnector.sendPacket(packet);
    }

    public void requestPics() {
        SteamPacket packet = SteamPacketProcessor.buildPICSPacket(steamSession.getSessionId(), steamSession.getSteamId());
        steamClientConnector.sendPacket(packet);
    }

    public void requestDepotKey() {
        SteamPacket packet = SteamPacketProcessor.buildDepotKeyPacket(DEPOT_ID, steamSession.getSessionId(), steamId);
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
            case EnumsClientserver.EMsg.k_EMsgClientLogOnResponse_VALUE -> handleLogon(packet);
            case EnumsClientserver.EMsg.k_EMsgClientPICSProductInfoResponse_VALUE -> handleProductInfo(packet);
            case EnumsClientserver.EMsg.k_EMsgClientGetDepotDecryptionKeyResponse_VALUE -> handleDepotKey(packet);
        }
    }

    private void handleLogon(SteamPacket packet) {
        try {
            var response = SteamPacketProcessor.handleLogonResponse(packet);
            int result = response.getEresult();
            if (result != 1) {
                throw new RuntimeException("Steam login failed: " + result);
            }
            int sessionId = packet.getHeader().getClientSessionid();
            long targetSteamId = packet.getHeader().getSteamid();
            this.steamSession = new SteamCMSession(sessionId, targetSteamId);
            requestPics();
        } catch (Exception e) {
            throw new RuntimeException("Cannot parse LogonResponse", e);
        }
    }

    private void handleProductInfo(SteamPacket packet) {
        try {
            var response = SteamPacketProcessor.handleProductInfoResponse(packet);
            ContentInfo contentInfo = parsePICSResponseBuffer(response.getAppsList().get(0).getBuffer().toStringUtf8());
            UpdateStatus status = versionsCheckService.checkForUpdates(contentInfo.getGameBuildId(), contentInfo.getManifestId(), (int) Instant.now().getEpochSecond());
            disconnect();
            CompletableFuture<UpdateStatus> future = pendingRequests.remove(this.steamId);
            if (future != null) {
                future.complete(status);
            }
        } catch (Exception e) {
            CompletableFuture<UpdateStatus> future = pendingRequests.remove(this.steamId);
            if (future != null) {
                future.completeExceptionally(e);
            }
            throw new RuntimeException("Cannot parse PICS response", e);
        }
    }

    private void handleDepotKey(SteamPacket packet) {
        try {
            var response = SteamPacketProcessor.handleDepotKeyResponse(packet);

            CompletableFuture<UpdateStatus> future = pendingRequests.remove(this.steamId);

        } catch (Exception e) {

            throw new RuntimeException("Cannot parse Depot Key response", e);
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
