package com.steam.skin.scheduler.content.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamCdnDirectoryResponse {

    @JsonProperty("response")
    private DirectoryResponse response;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DirectoryResponse {
        @JsonProperty("servers")
        private List<CdnServer> servers;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CdnServer {
        @JsonProperty("type")
        private String type;

        @JsonProperty("host")
        private String host;

        @JsonProperty("vhost")
        private String vhost;

        @JsonProperty("https_support")
        private String httpsSupport;

        @JsonProperty("weighted_load")
        private Double weightedLoad;
    }
}
