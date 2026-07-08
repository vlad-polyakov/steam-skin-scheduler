package com.steam.skin.scheduler.entity.steam.auth.common.creds;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
public class Credentials {
    private String login;
    private String password;
}
