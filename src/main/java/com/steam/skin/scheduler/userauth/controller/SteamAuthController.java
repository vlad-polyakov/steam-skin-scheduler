package com.steam.skin.scheduler.userauth.controller;

import com.steam.skin.scheduler.userauth.entity.steam.auth.common.creds.Credentials;
import com.steam.skin.scheduler.userauth.service.SteamAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

@RestController
@RequestMapping("/auth/steam")
@RequiredArgsConstructor
public class SteamAuthController {

    private final SteamAuthService steamAuthService;

    @PostMapping("/session")
    public ResponseEntity<?> steamSessionTrigger(Credentials request) throws IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        steamAuthService.generateSteamClientSession(request.getLogin(), request.getPassword());
        return ResponseEntity.ok("Session created");
    }

    @PostMapping("/token")
    public ResponseEntity<?> steamTokenGenerate(String username, String mailCode) throws InterruptedException {
        steamAuthService.generateSteamClientToken(username, mailCode);
        return ResponseEntity.ok("Token saved");
    }
}
