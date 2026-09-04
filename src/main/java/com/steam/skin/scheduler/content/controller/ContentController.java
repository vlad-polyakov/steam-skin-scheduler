package com.steam.skin.scheduler.content.controller;

import com.steam.protobuf.ContentManifest;
import com.steam.skin.scheduler.content.entity.vpk.VpkEntry;
import com.steam.skin.scheduler.content.service.FileChunksService;
import com.steam.skin.scheduler.content.service.ManifestService;
import com.steam.skin.scheduler.content.util.vpk.VpkReader;
import com.steam.skin.scheduler.getupdates.entity.pics.ContentInfo;
import com.steam.skin.scheduler.getupdates.service.SteamVersionsCheckService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    @GetMapping("/file/download")
    public ResponseEntity<?> downloadFile(String filename) throws Exception {
        ContentInfo contentInfo = steamVersionsCheckService.getContentInfoForDownload();
        List<ContentManifest.ContentManifestPayload.FileMapping> manifestPayloadList = manifestService.downloadManifestPayload(contentInfo.getManifestId());
        List<ContentManifest.ContentManifestPayload.FileMapping> manifestPak01DirList = manifestService.getFilesByDecryptedName(manifestPayloadList, "pak01_dir");
        VpkEntry found;
        for (ContentManifest.ContentManifestPayload.FileMapping fileMapping: manifestPak01DirList) {
            byte[] data = fileChunksService.downloadChunk(fileMapping, DEPOT_ID);
            List<VpkEntry> vpkEntry = VpkReader.readDirectory(data);
            found = vpkEntry.stream().filter(vpk -> vpk.filename().contains(filename)).findFirst().orElse(null);
            if(found != null) {
                List<ContentManifest.ContentManifestPayload.FileMapping> manifestPak01IndexList = manifestService.getFilesByDecryptedName(manifestPayloadList, "pak01_" + found.archiveIndex());
                byte[] fileData = fileChunksService.downloadRequiredOnlyChunks(manifestPak01IndexList.get(0), DEPOT_ID, found);
                return  ResponseEntity.ok(fileData);
            }
        }
        return ResponseEntity.ok(null);
    }

}
