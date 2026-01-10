package com.docstudio;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动类
 */
@SpringBootApplication
@MapperScan(basePackages = "com.docstudio.manager.mapper")
public class DocStudioApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DocStudioApplication.class, args);
    }
}

