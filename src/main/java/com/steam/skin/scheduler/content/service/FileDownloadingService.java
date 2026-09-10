package com.steam.skin.scheduler.content.service;

import com.steam.protobuf.ContentManifest;
import com.steam.skin.scheduler.content.entity.vpk.VpkEntry;
import com.steam.skin.scheduler.content.util.vpk.VpkReader;
import com.steam.skin.scheduler.getupdates.entity.pics.ContentInfo;
import com.steam.skin.scheduler.getupdates.service.SteamVersionsCheckService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileDownloadingService {

    private static final String DEPOT_ID = "2347770";

    private final SteamVersionsCheckService steamVersionsCheckService;
    private final ManifestService manifestService;
    private final FileChunksService fileChunksService;


    public byte[] downloadFile(String filename) throws Exception {
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
                return fileChunksService.downloadRequiredOnlyChunks(manifestPak01IndexList.get(0), DEPOT_ID, found);
            }
        }
        return null;
    }
}
