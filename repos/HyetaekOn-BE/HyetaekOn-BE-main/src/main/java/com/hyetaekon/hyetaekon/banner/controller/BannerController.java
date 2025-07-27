package com.hyetaekon.hyetaekon.banner.controller;

import com.hyetaekon.hyetaekon.banner.dto.BannerDto;
import com.hyetaekon.hyetaekon.banner.service.BannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
@RequiredArgsConstructor
public class BannerController {
    private final BannerService bannerService;

    @GetMapping
    public ResponseEntity<List<BannerDto>> getBanners() {
        return ResponseEntity.ok(bannerService.getBanners());
    }

    @GetMapping("/{bannerId}")
    public ResponseEntity<BannerDto> getBanner(@PathVariable Long bannerId) {
        return ResponseEntity.ok(bannerService.getBanner(bannerId));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BannerDto>> getAdminBanners() {
        return ResponseEntity.ok(bannerService.getAdminBanners());
    }

    @PostMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerDto> createBanner(@RequestBody BannerDto bannerDto) {
        return ResponseEntity.ok(bannerService.createBanner(bannerDto));
    }

    @PutMapping("/admin/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BannerDto> updateBanner(@PathVariable Long bannerId, @RequestBody BannerDto bannerDto) {
        return ResponseEntity.ok(bannerService.updateBanner(bannerId, bannerDto));
    }

    @DeleteMapping("/admin/{bannerId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBanner(@PathVariable Long bannerId) {
        bannerService.deleteBanner(bannerId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateBannerOrder(@RequestBody List<Long> bannerIds) {
        bannerService.updateBannerOrder(bannerIds);
        return ResponseEntity.noContent().build();
    }
}
