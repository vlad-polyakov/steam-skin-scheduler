package com.steam.skin.scheduler.content.controller;

import com.steam.protobuf.ContentManifest;
import com.steam.skin.scheduler.content.service.ManifestService;
import com.steam.skin.scheduler.getupdates.entity.pics.ContentInfo;
import com.steam.skin.scheduler.getupdates.service.SteamVersionsCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/content")
public class ContentController {

    @Autowired
    private SteamVersionsCheckService steamVersionsCheckService;

    @Autowired
    private ManifestService manifestService;

    @GetMapping("/manifest")
    public ResponseEntity<?> getManifest() throws Exception {
        ContentInfo contentInfo = steamVersionsCheckService.getContentInfoForDownload();
        ContentManifest.ContentManifestPayload.FileMapping manifestPayload = manifestService.downloadManifestPayload(contentInfo.getManifestId());
        return ResponseEntity.ok(manifestPayload);
    }

}
