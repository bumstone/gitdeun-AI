package com.hyetaekon.hyetaekon.common.config;

import com.hyetaekon.hyetaekon.common.converter.ServiceCategoryConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConverterConfig {

    @Bean
    public ServiceCategoryConverter serviceCategoryConverter() {
        return new ServiceCategoryConverter();
    }
}

