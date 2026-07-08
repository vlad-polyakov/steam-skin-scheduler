package com.steam.skin.scheduler.service;

import com.steam.skin.scheduler.entity.steam.auth.common.rsa.RSASteamKeyContainerResponse;
import com.steam.skin.scheduler.entity.steam.auth.common.rsa.RSASteamKeyResponse;
import com.steam.skin.scheduler.entity.steam.auth.common.session.SteamSessionLoginContainerResponse;
import com.steam.skin.scheduler.entity.steam.auth.common.session.SteamSessionLoginResponse;
import com.steam.skin.scheduler.entity.steam.auth.common.token.SteamAuthTokenInfoContainerResponse;
import com.steam.skin.scheduler.entity.steam.auth.common.token.SteamAuthTokenInfoResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;

@Service
public class SteamAuthService {

    private final RestTemplate restTemplate;

    private static final String STEAM_RSA_ENDPOINT = "https://api.steampowered.com/IAuthenticationService/GetPasswordRSAPublicKey/v1?account_name={accountName}";
    private static final String STEAM_SESSION_CREDS_ENDPOINT = "https://api.steampowered.com/IAuthenticationService/BeginAuthSessionViaCredentials/v1";
    private static final String STEAM_SESSION_UPDATE_ENDPOINT = "https://api.steampowered.com/IAuthenticationService/UpdateAuthSessionWithSteamGuardCode/v1";
    private static final String STEAM_SESSION_POLL_ENDPOINT = "https://api.steampowered.com/IAuthenticationService/PollAuthSessionStatus/v1";
    private static final int PERSISTANCE_SESSION = 1;
    private static final int PLATFORM_TYPE = 1;
    private static final String TOKEN_COOKIE_NAME = "steamClientToken";

    private SteamSessionLoginResponse steamSession;

    public SteamAuthService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private RSASteamKeyResponse getSteamRsaKey(String accountName) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            RSASteamKeyContainerResponse response = restTemplate.getForObject(
                    STEAM_RSA_ENDPOINT,
                    RSASteamKeyContainerResponse.class,
                    accountName
            );
            if (response != null) {
                return response.getResponse();
            }

        } catch (RuntimeException e) {
            throw new RuntimeException(e);
        }
        return new RSASteamKeyResponse();
    }

    private PublicKey constructPublicKey(RSASteamKeyResponse steamKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        String modulusHex = steamKey.getPublickey_mod();
        String exponentHex = steamKey.getPublickey_exp();
        BigInteger modulus = new BigInteger(modulusHex, 16);
        BigInteger exponent = new BigInteger(exponentHex, 16);
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    private String encryptSteamPassword(String login, String password) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException {
        RSASteamKeyResponse rsaSteamKeyResponse = getSteamRsaKey(login);
        PublicKey publicKey = constructPublicKey(rsaSteamKeyResponse);
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedBytes =
                cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    private SteamSessionLoginResponse authClientSession(String accountName, String encryptedPassword, String timestamp) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("account_name", accountName);
        form.add("encrypted_password", encryptedPassword);
        form.add("encryption_timestamp", timestamp);
        form.add("remember_login", "true");
        form.add("persistence", String.valueOf(PERSISTANCE_SESSION));
        form.add("website_id", "Steam");
        form.add("device_friendly_name", "SteamSkinScheduler");
        form.add("platform_type", String.valueOf(PLATFORM_TYPE));
        form.add("guard_data", "");
        form.add("language", "english");

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(form, headers);
        SteamSessionLoginContainerResponse response  = restTemplate.postForObject(
                STEAM_SESSION_CREDS_ENDPOINT,
                entity,
                SteamSessionLoginContainerResponse.class
        );
        return response != null ? response.getResponse() : null;
    }

    public SteamSessionLoginResponse performSteamClientSessionAuth(String name, String password) throws NoSuchAlgorithmException, InvalidKeySpecException, IllegalBlockSizeException, NoSuchPaddingException, BadPaddingException, InvalidKeyException {
        RSASteamKeyResponse rsaSteamKey = getSteamRsaKey(name);
        String encryptedPassword = encryptSteamPassword(name, password);
        return authClientSession(name, encryptedPassword, rsaSteamKey.getTimestamp());
    }

    public String performSteamClientSessionUpdateWithMailCode(SteamSessionLoginResponse session, String mailCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", session.getClient_id());
        form.add("steamid", session.getSteamid());
        form.add("code", mailCode);
        form.add("request_id", session.getRequest_id());
        form.add("code_type", "2");
        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(form, headers);
        String response  = restTemplate.postForObject(
                STEAM_SESSION_UPDATE_ENDPOINT,
                entity,
                String.class
        );
        return response;
    }

    public void triggerSteamMailCodeConfirmation(SteamSessionLoginResponse session) {
        performSteamClientSessionUpdateWithMailCode(session, null);
    }

    public SteamAuthTokenInfoResponse performSteamClientSessionPolling(SteamSessionLoginResponse session) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", session.getClient_id());
        form.add("request_id", session.getRequest_id());

        HttpEntity<MultiValueMap<String, String>> entity =
                new HttpEntity<>(form, headers);

        SteamAuthTokenInfoContainerResponse response = restTemplate.postForObject(
                STEAM_SESSION_POLL_ENDPOINT,
                entity,
                SteamAuthTokenInfoContainerResponse.class
        );
        return response.getResponse();
    }

    public String waitForSteamTokens(SteamSessionLoginResponse session)
            throws InterruptedException {

        for (int i = 0; i < 12; i++) {
            String response = performSteamClientSessionPolling(session).getRefresh_token();

            if (!response.equals("{\"response\":{}}")) {
                return response;
            }

            Thread.sleep(session.getInterval() * 1000L);
        }

        return null;
    }

    public void saveToken(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(TOKEN_COOKIE_NAME, token);

        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(cookie);
    }

    public String getToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (TOKEN_COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public void generateSteamClientSession(String login, String password) throws IllegalBlockSizeException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeySpecException, BadPaddingException, InvalidKeyException {
        this.steamSession = performSteamClientSessionAuth(login, password);
        triggerSteamMailCodeConfirmation(this.steamSession);
    }

    public void generateSteamClientToken(String mailCode, HttpServletResponse httpServletResponse) throws InterruptedException {
        performSteamClientSessionUpdateWithMailCode(this.steamSession, mailCode);
        String tokenInfo = waitForSteamTokens(this.steamSession);
        saveToken(httpServletResponse, tokenInfo);
    }


}
