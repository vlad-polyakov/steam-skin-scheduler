package com.steam.skin.scheduler.content.controller;

import com.steam.skin.scheduler.content.entity.content.TableMultiStatus;
import com.steam.skin.scheduler.content.service.ContentOrchestratorService;
import com.steam.skin.scheduler.content.service.CsContentService;
import com.steam.skin.scheduler.content.service.FileDownloadingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/content")
@RequiredArgsConstructor
public class ContentController {

    private final FileDownloadingService fileDownloadingService;
    private final ContentOrchestratorService csContentService;


    @GetMapping("/file/download")
    public ResponseEntity<?> downloadFile(String filename) throws Exception {
        return ResponseEntity.ok(fileDownloadingService.downloadFile(filename));
    }

    @PostMapping("/update/data")
    public ResponseEntity<?> updateDatabase(String login) throws Exception {
        TableMultiStatus result = csContentService.updateContentTables(login);
        return ResponseEntity.ok(result);
    }
}
