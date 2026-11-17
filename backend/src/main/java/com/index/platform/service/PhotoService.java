package com.index.platform.service;

import com.index.platform.domain.FarmSupply;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.security.AuthUser;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** One photo per supply listing, stored in the database (the website shrinks it before upload). */
@Service
@RequiredArgsConstructor
public class PhotoService {
    private static final List<String> TYPES = List.of("image/jpeg", "image/png", "image/webp");
    private final JdbcTemplate jdbc;
    private final SupplyService supplyService;

    public record Photo(String contentType, byte[] data) {}

    @Transactional
    public Dto.SupplyResponse save(AuthUser u, Long supplyId, MultipartFile file) {
        FarmSupply s = supplyService.owned(u, supplyId);
        String type = file.getContentType();
        if (type == null || !TYPES.contains(type)) throw ApiException.badRequest("Photo must be a JPEG, PNG or WebP image");
        if (file.isEmpty() || file.getSize() > 1_000_000) throw ApiException.badRequest("Photo must be smaller than 1 MB");
        byte[] bytes;
        try { bytes = file.getBytes(); } catch (IOException e) { throw ApiException.badRequest("Could not read the photo"); }
        jdbc.update("INSERT INTO supply_photos (supply_id, content_type, data) VALUES (?, ?, ?) "
                + "ON CONFLICT (supply_id) DO UPDATE SET content_type = EXCLUDED.content_type, data = EXCLUDED.data",
                supplyId, type, bytes);
        s.setPhotoUpdatedAt(Instant.now());
        return supplyService.toResponse(s);
    }

    @Transactional
    public Dto.SupplyResponse delete(AuthUser u, Long supplyId) {
        FarmSupply s = supplyService.owned(u, supplyId);
        jdbc.update("DELETE FROM supply_photos WHERE supply_id = ?", supplyId);
        s.setPhotoUpdatedAt(null);
        return supplyService.toResponse(s);
    }

    @Transactional(readOnly = true)
    public Optional<Photo> find(Long supplyId) {
        List<Photo> rows = jdbc.query("SELECT content_type, data FROM supply_photos WHERE supply_id = ?",
                (rs, i) -> new Photo(rs.getString("content_type"), rs.getBytes("data")), supplyId);
        return rows.stream().findFirst();
    }
}
