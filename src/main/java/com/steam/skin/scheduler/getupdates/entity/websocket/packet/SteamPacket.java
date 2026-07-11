package com.steam.skin.scheduler.getupdates.entity.websocket.packet;

import com.steam.protobuf.SteammessagesBase;
import lombok.*;

@Data
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SteamPacket {
    private int eMsg;
    private boolean isProtobuf;
    private SteammessagesBase.CMsgProtoBufHeader header;
    private byte[] bodyBytes;
}