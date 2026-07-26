package com.steam.skin.scheduler.getupdates.entity.websocket.packet;

import lombok.*;
import org.springframework.stereotype.Component;

@Data
@Getter
@Setter
@NoArgsConstructor
@Component
public class SteamContentContext {
    private long buildVersion;
    private long depotVersion;
    private byte[] depotKey;
    private long manifestCode;
}
