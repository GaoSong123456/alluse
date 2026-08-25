package com.example.enterprise;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 企业级 Demo 启动类
 *
 * @author enterprise-demo
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@MapperScan("com.example.enterprise.**.mapper")
public class EnterpriseDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnterpriseDemoApplication.class, args);
        System.out.println("""
                \n
                ==================================================
                  企业级 Demo 启动成功
                  - RocketMQ 五大特性: 幂等/顺序/可靠/延迟/死信
                  - 大文件 Excel 分片断点续传
                ==================================================
                """);
    }
}

