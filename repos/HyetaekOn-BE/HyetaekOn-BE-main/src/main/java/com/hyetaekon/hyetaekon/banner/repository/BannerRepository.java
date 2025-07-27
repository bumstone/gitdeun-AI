package com.hyetaekon.hyetaekon.banner.repository;

import com.hyetaekon.hyetaekon.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {
    List<Banner> findAllByOrderByDisplayOrderAsc();
}
