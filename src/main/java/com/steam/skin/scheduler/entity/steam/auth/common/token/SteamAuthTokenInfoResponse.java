package com.steam.skin.scheduler.entity.steam.auth.common.token;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SteamAuthTokenInfoResponse {
    private String access_token;
    private String refresh_token;
}
