package com.eopis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class EopisApplication {

    public static void main(String[] args) {
        SpringApplication.run(EopisApplication.class, args);
    }
}
