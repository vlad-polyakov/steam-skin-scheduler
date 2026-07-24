package com.steam.skin.scheduler.getupdates.controller;

import com.steam.skin.scheduler.getupdates.entity.pics.ContentInfo;
import com.steam.skin.scheduler.getupdates.entity.pics.UpdateStatus;
import com.steam.skin.scheduler.getupdates.service.SteamClientService;
import com.steam.skin.scheduler.getupdates.service.SteamVersionsCheckService;
import com.steam.skin.scheduler.userauth.entity.steam.auth.common.token.SteamToken;
import com.steam.skin.scheduler.userauth.service.SteamAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@RestController
@RequestMapping("/updates")
public class SteamUpdatesController {

    @Autowired
    private SteamAuthService steamAuthService;

    @Autowired
    private SteamClientService steamClientService;

    @Autowired
    private SteamVersionsCheckService steamVersionsCheckService;

    @PostMapping("/check")
    public ResponseEntity<?> checkUpdates(@RequestParam String login) {
        try {
            SteamToken token = steamAuthService.getToken(login);
            steamClientService.prepareLogin(login, token.getToken(), token.getSteamId());

            CompletableFuture<UpdateStatus> futureStatus = steamClientService.connect();
            UpdateStatus status = futureStatus.join();

            return ResponseEntity.ok(status);

        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            String errorMessage = (cause != null && cause.getMessage() != null)
                    ? cause.getMessage()
                    : e.toString();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error checking updates: " + errorMessage);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                    .body("Steam action timed out or failed: " + e.getMessage());
        }
    }

    @GetMapping("/buildInfo")
    public ResponseEntity<?> getBuildAndDepotInfo() throws RuntimeException {
        ContentInfo contentInfo = steamVersionsCheckService.getContentInfoForDownload();
        return ResponseEntity.ok(contentInfo);
    }
}
