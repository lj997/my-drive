package com.mydrive;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.mydrive.mapper")
public class MyDriveApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyDriveApplication.class, args);
    }
}
