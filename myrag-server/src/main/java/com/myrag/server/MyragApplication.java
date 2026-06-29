package com.myrag.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@ComponentScan(basePackages = "com.myrag")
@EntityScan(basePackages = {"com.myrag.rag.core.entity", "com.myrag.rag.monitor.entity"})
@EnableJpaRepositories(basePackages = {"com.myrag.rag.core.repository", "com.myrag.rag.monitor.repository"})
@EnableAsync
public class MyragApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyragApplication.class, args);
    }
}
