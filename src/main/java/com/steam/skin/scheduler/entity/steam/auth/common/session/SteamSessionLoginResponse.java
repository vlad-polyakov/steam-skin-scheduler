package com.steam.skin.scheduler.entity.steam.auth.common.session;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class SteamSessionLoginResponse {
    private String client_id;
    private String request_id;
    private String steamid;
    private Integer interval;
    private List<AllowedConfirmation> allowed_confirmations;


}
