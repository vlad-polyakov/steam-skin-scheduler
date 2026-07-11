package com.steam.skin.scheduler.userauth.entity.steam.auth.common.session;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SteamSessionLoginRequest {

    private String accountName;
    private String encryptedPassword;
    private String encryptionTimestamp;
    private boolean rememberLogin;
    private Integer persistence;
    private String websiteId;
    private String deviceFriendlyName;
    private Integer platformType;
    private String guard_data;
    private String language="english";
    private Integer qos_level=2;
}
