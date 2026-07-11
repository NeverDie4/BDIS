package com.bdis;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@MapperScan({
    "com.bdis.modules.herb.mapper",
    "com.bdis.modules.map.mapper",
    "com.bdis.modules.file.mapper",
    "com.bdis.modules.growth.mapper",
    "com.bdis.modules.evaluation.mapper",
    "com.bdis.modules.declaration.mapper",
    "com.bdis.modules.performance.mapper"
})
@SpringBootApplication
public class BdisApplication {

    public static void main(String[] args) {
        SpringApplication.run(BdisApplication.class, args);
    }
}
