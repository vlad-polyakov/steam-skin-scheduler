package com.steam.skin.scheduler.getupdates.entity.websocket.session;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@Builder
public class SteamCMSessionContext {
    private int sessionId;
    private long steamId;
    private String username;
    private String token;
    private long buildVersion;
    private long depotVersion;
    private byte[] depotKey;
}
