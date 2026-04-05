package com.stazy.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@ConfigurationPropertiesScan
@SpringBootApplication
public class StazyBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(StazyBackendApplication.class, args);
    }
}
