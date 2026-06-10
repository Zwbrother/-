package com.zjsu.scholarship;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zjsu.scholarship.mapper")
public class ScholarshipApplication {
    public static void main(String[] args) {
        SpringApplication.run(ScholarshipApplication.class, args);
    }
}
