package com.steam.skin.scheduler.userauth.entity.steam.auth.common.rsa;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RSASteamKeyResponse {
    private String publickey_mod;
    private String publickey_exp;
    private String timestamp;
}
