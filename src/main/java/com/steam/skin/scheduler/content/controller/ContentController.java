package com.steam.skin.scheduler.content.controller;

import com.steam.protobuf.ContentManifest;
import com.steam.skin.scheduler.content.service.FileChunksService;
import com.steam.skin.scheduler.content.service.ManifestService;
import com.steam.skin.scheduler.getupdates.entity.pics.ContentInfo;
import com.steam.skin.scheduler.getupdates.service.SteamVersionsCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/content")
public class ContentController {

    private static final String DEPOT_ID = "2347770";

    @Autowired
    private SteamVersionsCheckService steamVersionsCheckService;

    @Autowired
    private ManifestService manifestService;

    @Autowired
    private FileChunksService fileChunksService;

    @GetMapping("/manifest")
    public ResponseEntity<?> getManifest() throws Exception {
        ContentInfo contentInfo = steamVersionsCheckService.getContentInfoForDownload();
        ContentManifest.ContentManifestPayload.FileMapping manifestPayload = manifestService.downloadManifestPayload(contentInfo.getManifestId());
        byte[] chunkData = fileChunksService.downloadChunk(manifestPayload.getShaContent(), DEPOT_ID);
        byte[] decoded = fileChunksService.decodeChunk(
                chunkData,
                manifestPayload.getChunks(0).getCbOriginal(),
                Integer.toUnsignedLong(manifestPayload.getChunks(0).getCrc())
        );
        return ResponseEntity.ok(manifestPayload.getFilename());
    }

    @GetMapping("/chunks")
    public ResponseEntity<?> getChunks() throws Exception {
        return ResponseEntity.ok(null);
    }

}
