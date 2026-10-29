package com.index.platform.web;

import com.index.platform.domain.Crop;
import com.index.platform.dto.Dto;
import com.index.platform.exception.ApiException;
import com.index.platform.repo.CropRepository;
import com.index.platform.service.Mappers;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/crops")
@RequiredArgsConstructor
public class CropController {
    private final CropRepository crops;

    @GetMapping
    @Transactional(readOnly = true)
    public List<Dto.CropResponse> list(@RequestParam(defaultValue = "false") boolean includeInactive) {
        var list = includeInactive ? crops.findAllByOrderByName() : crops.findByActiveTrueOrderByName();
        return list.stream().map(Mappers::crop).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Dto.CropResponse create(@Valid @RequestBody Dto.CropRequest r) {
        if (crops.existsByNameIgnoreCase(r.name().trim())) throw ApiException.conflict("Crop already exists");
        Crop c = new Crop();
        c.setName(r.name().trim()); c.setCategory(r.category());
        if (r.unit() != null && !r.unit().isBlank()) c.setUnit(r.unit().trim());
        if (r.active() != null) c.setActive(r.active());
        return Mappers.crop(crops.save(c));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public Dto.CropResponse update(@PathVariable Long id, @Valid @RequestBody Dto.CropRequest r) {
        Crop c = crops.findById(id).orElseThrow(() -> ApiException.notFound("Crop"));
        if (!c.getName().equalsIgnoreCase(r.name().trim()) && crops.existsByNameIgnoreCase(r.name().trim()))
            throw ApiException.conflict("Crop already exists");
        c.setName(r.name().trim()); c.setCategory(r.category());
        if (r.unit() != null && !r.unit().isBlank()) c.setUnit(r.unit().trim());
        if (r.active() != null) c.setActive(r.active());
        return Mappers.crop(c);
    }
}
