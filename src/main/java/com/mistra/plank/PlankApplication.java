package com.mistra.plank;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @author mistra
 */
@Slf4j
@EnableScheduling
@EnableCaching
@SpringBootApplication
@MapperScan("com.mistra.plank.dao")
public class PlankApplication {

    public static void main(String[] args) {
        SpringApplication.run(PlankApplication.class, args);
    }

}
