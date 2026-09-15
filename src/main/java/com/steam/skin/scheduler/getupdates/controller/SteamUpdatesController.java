package com.steam.skin.scheduler.getupdates.controller;

import com.steam.skin.scheduler.getupdates.entity.pics.UpdateStatus;
import com.steam.skin.scheduler.getupdates.service.SteamUpdateOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletionException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/updates")
public class SteamUpdatesController {

    private final SteamUpdateOrchestratorService updateOrchestratorService;

    @PostMapping("/check")
    public ResponseEntity<?> checkUpdates(@RequestParam String login) {
        try {
            UpdateStatus status = updateOrchestratorService.checkUpdates(login);
            return ResponseEntity.ok(status);
        }
        catch (CompletionException e) {
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

}
