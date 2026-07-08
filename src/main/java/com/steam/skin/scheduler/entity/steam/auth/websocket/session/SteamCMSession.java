package com.steam.skin.scheduler.entity.steam.auth.websocket.session;

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
