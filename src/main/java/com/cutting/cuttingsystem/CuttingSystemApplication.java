package com.cutting.cuttingsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.cutting.cuttingsystem.mapper")
@EnableScheduling
public class CuttingSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CuttingSystemApplication.class, args);
    }

}
