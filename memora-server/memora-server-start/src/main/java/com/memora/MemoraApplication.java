package com.memora;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 应用启动类
 */
@SpringBootApplication
@MapperScan(basePackages = "com.memora.manager.mapper")
@EnableScheduling
public class MemoraApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(MemoraApplication.class, args);
    }
}
