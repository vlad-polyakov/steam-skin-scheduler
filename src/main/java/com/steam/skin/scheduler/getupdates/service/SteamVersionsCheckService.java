package com.steam.skin.scheduler.getupdates.service;

import com.steam.skin.scheduler.getupdates.entity.pics.CS2VersionEntity;
import com.steam.skin.scheduler.getupdates.entity.pics.ContentInfo;
import com.steam.skin.scheduler.getupdates.entity.pics.DepotVersionEntity;
import com.steam.skin.scheduler.getupdates.entity.pics.UpdateStatus;
import com.steam.skin.scheduler.getupdates.repository.CS2VersionRepository;
import com.steam.skin.scheduler.getupdates.repository.DepotVersionRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public ContentInfo getContentInfoForDownload() {
        String manifestId = null;
        String gameBuild = null;
        Optional<CS2VersionEntity> lastBuildOpt = cs2VersionRepository.findFirstByOrderByIdDesc();
        if (lastBuildOpt.isPresent()) {
            gameBuild = lastBuildOpt.get().getBuildId();
        }

        Optional<DepotVersionEntity> lastDepotOpt = depotVersionRepository.findFirstByOrderByIdDesc();
        if (lastDepotOpt.isPresent()) {
            manifestId = lastDepotOpt.get().getManifestId();
        }

        if(gameBuild == null || manifestId == null) {
            throw new RuntimeException("No required data to download depot");
        }

        return ContentInfo.builder().gameBuildId(gameBuild).manifestId(manifestId).build();

    }

    @Transactional
    public void updateDepotKey(long manifestId, byte[] depotKey) {
        DepotVersionEntity entity = depotVersionRepository.findByManifestId(String.valueOf(manifestId))
                .orElseThrow(() -> new EntityNotFoundException("Version entry not found"));

        entity.setDepotKey(depotKey);
    }
}
