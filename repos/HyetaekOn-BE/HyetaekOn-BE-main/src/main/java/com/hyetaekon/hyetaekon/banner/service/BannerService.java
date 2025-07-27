package com.hyetaekon.hyetaekon.banner.service;

import com.hyetaekon.hyetaekon.banner.dto.BannerDto;
import com.hyetaekon.hyetaekon.banner.entity.Banner;
import com.hyetaekon.hyetaekon.banner.mapper.BannerMapper;
import com.hyetaekon.hyetaekon.banner.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BannerService {
    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;

    public List<BannerDto> getBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(bannerMapper::toDto).collect(Collectors.toList());
    }

    public BannerDto getBanner(Long bannerId) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 배너가 존재하지 않습니다."));
        return bannerMapper.toDto(banner);
    }

    public List<BannerDto> getAdminBanners() {
        return getBanners();
    }

    public BannerDto createBanner(BannerDto bannerDto) {
        Banner banner = bannerMapper.toEntity(bannerDto);
        return bannerMapper.toDto(bannerRepository.save(banner));
    }

    public BannerDto updateBanner(Long bannerId, BannerDto bannerDto) {
        Banner banner = bannerRepository.findById(bannerId)
                .orElseThrow(() -> new IllegalArgumentException("해당 배너가 존재하지 않습니다."));

        banner.setTitle(bannerDto.getTitle());
        banner.setImageUrl(bannerDto.getImageUrl());
        banner.setLinkUrl(bannerDto.getLinkUrl());
        banner.setDisplayOrder(bannerDto.getDisplayOrder());

        return bannerMapper.toDto(bannerRepository.save(banner));
    }

    public void deleteBanner(Long bannerId) {
        bannerRepository.deleteById(bannerId);
    }

    @Transactional
    public void updateBannerOrder(List<Long> bannerIds) {
        int order = 1;
        for (Long bannerId : bannerIds) {
            Banner banner = bannerRepository.findById(bannerId)
                    .orElseThrow(() -> new IllegalArgumentException("배너를 찾을 수 없습니다."));
            banner.setDisplayOrder(order++);
        }
    }
}
