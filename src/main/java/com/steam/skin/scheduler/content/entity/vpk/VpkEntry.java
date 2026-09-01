package com.steam.skin.scheduler.content.entity.vpk;

public record VpkEntry(
        String path,
        String filename,
        String extension,
        long crc32,
        int preloadBytes,
        int archiveIndex,
        long offset,
        long length
) {
    public String fullPath() {
        return path + "/" + filename + "." + extension;
    }
}
