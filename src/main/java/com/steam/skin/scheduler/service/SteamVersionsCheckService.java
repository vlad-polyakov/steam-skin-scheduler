package com.steam.skin.scheduler.service;

import com.steam.skin.scheduler.entity.steam.pics.CS2VersionEntity;
import com.steam.skin.scheduler.entity.steam.pics.DepotVersionEntity;
import com.steam.skin.scheduler.entity.steam.pics.UpdateStatus;
import com.steam.skin.scheduler.repository.CS2VersionRepository;
import com.steam.skin.scheduler.repository.DepotVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class SteamVersionsCheckService {

    @Autowired
    private CS2VersionRepository cs2VersionRepository;

    @Autowired
    private DepotVersionRepository depotVersionRepository;

    public UpdateStatus checkForUpdates(String parsedBuildId, String parsedManifestId, int currentTimestamp) {

        Optional<CS2VersionEntity> lastBuildOpt = cs2VersionRepository.findFirstByOrderByIdDesc();
        if (lastBuildOpt.isPresent() && lastBuildOpt.get().getBuildId().equals(parsedBuildId)) {
            return UpdateStatus.NO_UPDATE_NEEDED;
        }

        cs2VersionRepository.save(CS2VersionEntity.builder()
                .buildId(parsedBuildId)
                .timestamp(currentTimestamp)
                .build());

        Optional<DepotVersionEntity> lastDepotOpt = depotVersionRepository.findFirstByOrderByIdDesc();

        if (lastDepotOpt.isPresent() && lastDepotOpt.get().getManifestId().equals(parsedManifestId)) {
            return UpdateStatus.DEPOT_UP_TO_DATE;
        }

        depotVersionRepository.save(DepotVersionEntity.builder()
                .manifestId(parsedManifestId)
                .timestamp(currentTimestamp)
                .build());

        return UpdateStatus.UPDATE_REQUIRED;
    }
}
