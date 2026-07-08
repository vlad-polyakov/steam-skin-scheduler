package com.steam.skin.scheduler.entity.get.servers;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
public class SteamCMResponse {

    @JsonProperty("serverlist_websockets")
    private List<String> serverList;

    private int result;
}