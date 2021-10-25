package com.gel.ricedecetion;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.gel.ricedecetion.mapper")
public class RicedecetionApplication {

    public static void main(String[] args) {
        SpringApplication.run(RicedecetionApplication.class, args);
    }

}
