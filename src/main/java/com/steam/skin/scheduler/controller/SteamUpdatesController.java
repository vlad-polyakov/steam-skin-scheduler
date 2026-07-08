package com.steam.skin.scheduler.controller;

import com.steam.skin.scheduler.service.SteamAuthService;
import com.steam.skin.scheduler.service.SteamClientService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/updates")
public class SteamUpdatesController {

    @Autowired
    private SteamAuthService steamAuthService;

    @Autowired
    private SteamClientService steamClientService;

    @PostMapping("/updates/check")
    public ResponseEntity<?> checkUpdates(String login, long steamId, HttpServletRequest request) throws Exception {
        String token = steamAuthService.getToken(request);
        steamClientService.prepareLogin(login, token, steamId);
        steamClientService.connect();
        return ResponseEntity.ok("Updates have been checked");
    }

}
