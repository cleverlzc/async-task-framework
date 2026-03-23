package com.example.async.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置类
 * 
 * 配置任务执行使用的线程池，支持动态配置。
 * 线程池参数通过application.yml中的async-task.thread-pool配置。
 * 
 * 核心参数说明：
 * - core-pool-size: 核心线程数，默认8
 * - max-pool-size: 最大线程数，默认16
 * - queue-capacity: 队列容量，默认1000
 * - keep-alive-seconds: 空闲线程存活时间，默认60秒
 * - thread-name-prefix: 线程名称前缀，默认"async-task-executor-"
 * - rejection-policy: 拒绝策略，默认CALLER_RUNS（调用者运行）
 * 
 * 拒绝策略说明：
 * - ABORT: 抛出RejectedExecutionException异常
 * - CALLER_RUNS: 由调用线程执行该任务（推荐，保证任务不丢失）
 * - DISCARD: 直接丢弃任务，不抛出异常
 * - DISCARD_OLDEST: 丢弃队列中最老的任务，然后重新尝试执行
 * 
 * @author async-task-framework
 * @version 1.0.0
 */
@Slf4j
@Configuration
@EnableAsync
public class ThreadPoolConfig {
    
    /**
     * 创建异步任务执行线程池
     * 
     * 使用ThreadPoolTaskExecutor作为线程池实现，支持Spring异步任务。
     * 
     * @return 线程池执行器
     */
    @Bean(name = "asyncTaskExecutor")
    @ConfigurationProperties(prefix = "async-task.thread-pool")
    public Executor asyncTaskExecutor() {
        log.info("初始化异步任务线程池...");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 设置核心参数
        executor.setCorePoolSize(8); // 核心线程数
        executor.setMaxPoolSize(16); // 最大线程数
        executor.setQueueCapacity(1000); // 队列容量
        executor.setKeepAliveSeconds(60); // 空闲线程存活时间
        executor.setThreadNamePrefix("async-task-executor-"); // 线程名称前缀
        
        // 设置拒绝策略为调用者运行
        // 这样当线程池和队列都满时，任务会由提交任务的线程执行
        // 可以保证任务不丢失，但会降低系统吞吐量
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        // 等待所有任务完成后再关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(true);
        
        // 等待时间设置为60秒
        executor.setAwaitTerminationSeconds(60);
        
        // 初始化线程池
        executor.initialize();
        
        log.info("异步任务线程池初始化完成: corePoolSize={}, maxPoolSize={}, queueCapacity={}, " +
                        "keepAliveSeconds={}, threadNamePrefix={}, rejectedPolicy={}",
                executor.getCorePoolSize(),
                executor.getMaxPoolSize(),
                executor.getQueueCapacity(),
                executor.getKeepAliveSeconds(),
                executor.getThreadNamePrefix(),
                "CallerRunsPolicy");
        
        return executor;
    }
    
    /**
     * 线程池配置属性类
     * 
     * 用于从配置文件中读取线程池参数。
     * 通过@ConfigurationProperties注解实现自动绑定。
     */
    @ConfigurationProperties(prefix = "async-task.thread-pool")
    public static class ThreadPoolProperties {
        
        /**
         * 核心线程数
         */
        private int corePoolSize = 8;
        
        /**
         * 最大线程数
         */
        private int maxPoolSize = 16;
        
        /**
         * 队列容量
         */
        private int queueCapacity = 1000;
        
        /**
         * 空闲线程存活时间（秒）
         */
        private int keepAliveSeconds = 60;
        
        /**
         * 线程名称前缀
         */
        private String threadNamePrefix = "async-task-executor-";
        
        /**
         * 拒绝策略
         */
        private String rejectionPolicy = "CALLER_RUNS";
        
        // Getter and Setter methods
        public int getCorePoolSize() {
            return corePoolSize;
        }
        
        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }
        
        public int getMaxPoolSize() {
            return maxPoolSize;
        }
        
        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }
        
        public int getQueueCapacity() {
            return queueCapacity;
        }
        
        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
        
        public int getKeepAliveSeconds() {
            return keepAliveSeconds;
        }
        
        public void setKeepAliveSeconds(int keepAliveSeconds) {
            this.keepAliveSeconds = keepAliveSeconds;
        }
        
        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }
        
        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
        
        public String getRejectionPolicy() {
            return rejectionPolicy;
        }
        
        public void setRejectionPolicy(String rejectionPolicy) {
            this.rejectionPolicy = rejectionPolicy;
        }
        
        /**
         * 转换拒绝策略字符串为RejectedExecutionHandler对象
         * 
         * @return 拒绝策略处理器
         */
        public RejectedExecutionHandler getRejectedExecutionHandler() {
            switch (rejectionPolicy.toUpperCase()) {
                case "ABORT":
                    return new ThreadPoolExecutor.AbortPolicy();
                case "CALLER_RUNS":
                    return new ThreadPoolExecutor.CallerRunsPolicy();
                case "DISCARD":
                    return new ThreadPoolExecutor.DiscardPolicy();
                case "DISCARD_OLDEST":
                    return new ThreadPoolExecutor.DiscardOldestPolicy();
                default:
                    log.warn("未知的拒绝策略: {}, 使用默认策略: CALLER_RUNS", rejectionPolicy);
                    return new ThreadPoolExecutor.CallerRunsPolicy();
            }
        }
    }
}
