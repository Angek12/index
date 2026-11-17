package com.index.platform.web;

import com.index.platform.service.PhotoService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Produce photos are public so that image tags work without a login header. */
@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {
    private final PhotoService photos;

    @GetMapping("/{supplyId}")
    public ResponseEntity<byte[]> get(@PathVariable Long supplyId) {
        return photos.find(supplyId)
                .map(p -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(p.contentType()))
                        .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                        .body(p.data()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
