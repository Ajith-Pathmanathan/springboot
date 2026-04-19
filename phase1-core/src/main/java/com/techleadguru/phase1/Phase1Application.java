package com.techleadguru.phase1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true) // Required for Day 19-20: self-invocation fix
public class Phase1Application {

    public static void main(String[] args) {
        SpringApplication.run(Phase1Application.class, args);
    }
}
