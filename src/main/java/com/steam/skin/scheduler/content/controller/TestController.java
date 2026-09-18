package com.steam.skin.scheduler.content.controller;

import com.steam.skin.scheduler.getupdates.service.SteamVersionsCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestController {

    private final SteamVersionsCheckService steamVersionsCheckService;

    @PostMapping("/clear/all")
    public ResponseEntity<?> clearVersionsDb() {
        steamVersionsCheckService.clearVersionTables();
        return ResponseEntity.ok().build();
    }

    @PostMapping("/clear/latest")
    public ResponseEntity<?> clearLatestVersions() {
        steamVersionsCheckService.deleteLastVersion();
        return ResponseEntity.ok().build();
    }



}
