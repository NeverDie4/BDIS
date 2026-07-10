package com.bdis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan({
    "com.bdis.file.mapper",
    "com.bdis.audit.mapper",
    "com.bdis.soap.mapper",
    "com.bdis.dashboard.mapper"
})
@SpringBootApplication
public class BdisApplication {

    public static void main(String[] args) {
        SpringApplication.run(BdisApplication.class, args);
    }
}
