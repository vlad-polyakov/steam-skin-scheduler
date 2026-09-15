package com.steam.skin.scheduler.content.entity.content;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.steam.skin.scheduler.content.entity.TableUpdateStatus;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Builder
public class TableMultiStatus {

    @JsonProperty("status")
    private TableUpdateStatus status;

    @JsonProperty("entities")
    private String entityName;
}
