package com.example.async;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 异步任务处理框架启动类
 * 
 * 功能特性：
 * 1. 纯MySQL + 定时轮询方案，满足 < 1000 QPS 场景需求
 * 2. 乐观锁机制防止任务重复执行
 * 3. 自动重试机制（默认最大重试3次）
 * 4. 超时控制（默认300秒）
 * 5. 线程池管理并发任务执行
 * 6. 幂等性保证（通过businessKey）
 * 7. 完整的执行日志记录
 * 8. 可扩展的Handler注册模式
 * 9. 批量任务认领（每次50个）
 * 
 * 技术栈：
 * - Spring Boot 3.2.0
 * - MyBatis 3.0.3
 * - MySQL 8.0+
 * - Java 17
 * 
 * @author RelayAgent
 * @version 1.0.0
 */
@SpringBootApplication
// 启用定时任务调度
@EnableScheduling
// 启用事务管理
@EnableTransactionManagement
public class AsyncTaskApplication {

    /**
     * 应用程序入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(AsyncTaskApplication.class, args);
        System.out.println("=================================================");
        System.out.println("异步任务处理框架启动成功！");
        System.out.println("访问地址: http://localhost:8080/async-task");
        System.out.println("健康检查: http://localhost:8080/async-task/api/tasks/health");
        System.out.println("=================================================");
    }
}
