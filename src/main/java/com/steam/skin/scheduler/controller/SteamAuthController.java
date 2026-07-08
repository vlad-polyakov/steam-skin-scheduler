package com.steam.skin.scheduler.controller;

import com.steam.skin.scheduler.entity.steam.auth.common.creds.Credentials;
import com.steam.skin.scheduler.service.SteamAuthService;
import jakarta.servlet.http.HttpServletResponse;
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
public class SteamAuthController {

    @Autowired
    private SteamAuthService steamAuthService;

    @PostMapping("/session")
    public ResponseEntity<?> steamSessionTrigger(Credentials request) throws IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        steamAuthService.generateSteamClientSession(request.getLogin(), request.getPassword());
        return ResponseEntity.ok("Session created");
    }

    @PostMapping("/token")
    public ResponseEntity<?> steamTokenGenerate(String mailCode, HttpServletResponse response) throws InterruptedException {
        steamAuthService.generateSteamClientToken(mailCode, response);
        return ResponseEntity.ok("Token saved");
    }
}
