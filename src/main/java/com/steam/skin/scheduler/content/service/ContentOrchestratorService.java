package com.steam.skin.scheduler.content.service;

import com.steam.skin.scheduler.content.entity.TableUpdateStatus;
import com.steam.skin.scheduler.content.entity.content.TableMultiStatus;
import com.steam.skin.scheduler.getupdates.entity.pics.UpdateStatus;
import com.steam.skin.scheduler.getupdates.service.SteamUpdateOrchestratorService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ContentOrchestratorService {
    private final CsContentService csContentService;
    private final SteamUpdateOrchestratorService updateOrchestratorService;

    public TableMultiStatus updateContentTables(String login) throws Exception {
        UpdateStatus gameUpdateStatus = updateOrchestratorService.checkUpdates(login);
        if(!gameUpdateStatus.equals(UpdateStatus.UPDATE_REQUIRED)) {
            return TableMultiStatus.builder().status(TableUpdateStatus.GAME_NOT_UPDATED).build();
        }
        TableUpdateStatus status = TableUpdateStatus.NO_CONTENT_UPDATE;
        Map<String, TableUpdateStatus> statusMap = new HashMap<>();
        TableUpdateStatus rarityUpdateStatus = csContentService.fillRaritiesTable();
        statusMap.put("Rarity", rarityUpdateStatus);
        TableUpdateStatus weaponUpdateStatus = csContentService.fillWeaponTable();
        statusMap.put("Weapon", weaponUpdateStatus);
        TableUpdateStatus itemSetUpdateStatus = csContentService.fillItemSetTable();
        statusMap.put("Collection", itemSetUpdateStatus);

        String entityNames = "";
        for(String key: statusMap.keySet()) {
            if(statusMap.get(key).equals(TableUpdateStatus.UPDATED_SUCCESSFULLY)) {
                entityNames += key;
                entityNames += ", ";
            }
        }
        if (entityNames != "") {
            status = TableUpdateStatus.UPDATED_SUCCESSFULLY;
        }
        if(entityNames.endsWith(", ")) {
            entityNames = entityNames.substring(0, entityNames.length() - 2);
        }
        return TableMultiStatus.builder().entityName(entityNames).status(status).build();

    }
}
