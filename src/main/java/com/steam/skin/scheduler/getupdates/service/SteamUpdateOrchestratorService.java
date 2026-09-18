package com.steam.skin.scheduler.getupdates.service;

import com.steam.skin.scheduler.getupdates.entity.pics.UpdateStatus;
import com.steam.skin.scheduler.userauth.entity.steam.auth.common.token.SteamToken;
import com.steam.skin.scheduler.userauth.service.SteamAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Service
@RequiredArgsConstructor
public class SteamUpdateOrchestratorService {

    private final SteamAuthService steamAuthService;
    private final SteamClientService steamClientService;

    public UpdateStatus checkUpdates(String login) throws CompletionException, Exception {
            SteamToken token = steamAuthService.getToken(login);
            steamClientService.prepareLogin(login, token.getToken(), token.getSteamId());
            CompletableFuture<UpdateStatus> futureStatus = steamClientService.connect();
            UpdateStatus status = futureStatus.join();
            return status;
    }
}
