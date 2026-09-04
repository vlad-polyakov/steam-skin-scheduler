package com.steam.skin.scheduler.content.entity.chunk;

import com.steam.protobuf.ContentManifest;

public record ChunkIntersection(
        ContentManifest.ContentManifestPayload.FileMapping.ChunkData chunk,
        long chunkOffset,
        long sourceOffset,
        int length
) {}
