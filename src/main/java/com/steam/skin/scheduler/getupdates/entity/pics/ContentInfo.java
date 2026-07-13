package com.steam.skin.scheduler.getupdates.entity.pics;

import lombok.*;

@Data
@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class ContentInfo {
    private String manifestId;
    private String gameBuildId;

}
