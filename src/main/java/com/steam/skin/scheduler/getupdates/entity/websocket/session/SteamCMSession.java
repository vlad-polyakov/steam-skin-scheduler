package com.steam.skin.scheduler.getupdates.entity.websocket.session;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@Builder
public class SteamCMSession {
    private int sessionId;
    private long steamId;
}
