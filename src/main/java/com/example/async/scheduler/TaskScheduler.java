package com.example.async.scheduler;

import com.example.async.entity.AsyncTask;
import com.example.async.enums.TaskStatus;
import com.example.async.mapper.TaskMapper;
import com.example.async.service.TaskExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 异步任务调度器
 * 
 * 核心功能：
 * 1. 定时轮询待处理任务（默认500ms间隔）
 * 2. 使用乐观锁批量认领任务（默认每次50个）
 * 3. 将认领成功的任务提交到线程池异步执行
 * 4. 定时检测并处理超时任务（每分钟执行一次）
 * 
 * 工作原理：
 * - 通过定时任务定期扫描数据库中的待处理任务
 * - 使用乐观锁机制防止多个节点同时认领同一任务
 * - 认领成功的任务会被提交到线程池中异步执行
 * - 超时任务会被重置为待处理状态，以便重新执行
 * 
 * 适用场景：
 * - 适用于起步阶段的异步任务处理（< 1000 QPS）
 * - 纯MySQL方案，无需额外的中间件（如Redis、RabbitMQ）
 * - 支持多节点部署，通过乐观锁实现分布式锁
 * 
 * @author RelayAgent
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "async-task.scheduler.enabled", havingValue = "true", matchIfMissing = true)
public class TaskScheduler {

    /**
     * 任务数据访问层
     * 负责与数据库交互，查询和更新任务状态
     */
    private final TaskMapper taskMapper;

    /**
     * 任务执行服务
     * 负责具体执行任务的业务逻辑
     */
    private final TaskExecutorService taskExecutorService;

    /**
     * 异步任务执行线程池
     * 用于异步执行认领成功的任务
     */
    private final Executor asyncTaskExecutor;

    /**
     * 轮询间隔（毫秒）
     * 默认500ms，可通过配置文件调整
     * 较短的间隔可以更快地响应新任务，但会增加数据库压力
     */
    @Value("${async-task.scheduler.polling-interval:500}")
    private long pollingInterval;

    /**
     * 批量认领任务数量
     * 默认每次认领50个任务，可通过配置文件调整
     * 较大的批次可以提高吞吐量，但会增加内存占用
     */
    @Value("${async-task.scheduler.batch-size:50}")
    private int batchSize;

    /**
     * 执行节点标识
     * 用于标识当前任务在哪个节点上执行
     * 默认使用环境变量HOSTNAME，如果没有则使用"local-node"
     */
    @Value("${async-task.executor.execute-node:local-node}")
    private String executeNode;

    /**
     * 定时调度任务 - 每500ms执行一次
     * 
     * 执行流程：
     * 1. 查询待处理的任务（按优先级排序）
     * 2. 使用乐观锁批量认领任务
     * 3. 将认领成功的任务提交到线程池异步执行
     * 
     * 注意事项：
     * - 使用fixedDelay而不是fixedRate，确保上一次执行完成后再执行下一次
     * - 乐观锁机制确保同一任务不会被多个节点同时认领
     * - 认领失败的任务会被跳过，下次轮询时再次尝试
     * 
     * @throws Exception 调度过程中可能发生的异常
     */
    @Scheduled(fixedDelayString = "${async-task.scheduler.polling-interval:500}")
    public void scheduleTasks() throws Exception {
        try {
            // 1. 查询待处理的任务
            // 按优先级升序、创建时间升序排序，优先处理高优先级和较早创建的任务
            List<AsyncTask> pendingTasks = taskMapper.selectPendingTasks(batchSize);
            
            // 如果没有待处理任务，直接返回
            if (pendingTasks.isEmpty()) {
                return;
            }

            log.debug("发现 {} 个待处理任务", pendingTasks.size());

            // 2. 使用乐观锁批量认领任务
            // 乐观锁机制：只有当任务的version与数据库中的version一致时，才能成功认领
            List<AsyncTask> claimedTasks = claimTasksWithOptimisticLock(pendingTasks);
            
            // 如果没有成功认领的任务，直接返回
            if (claimedTasks.isEmpty()) {
                return;
            }

            log.info("成功认领 {} 个任务，节点: {}", claimedTasks.size(), executeNode);

            // 3. 将认领成功的任务提交到线程池异步执行
            // 使用CompletableFuture实现异步执行，不阻塞调度线程
            executeTasksAsync(claimedTasks);

        } catch (Exception e) {
            // 记录异常，但不中断调度任务
            log.error("任务调度异常", e);
        }
    }

    /**
     * 使用乐观锁批量认领任务
     * 
     * 乐观锁原理：
     * - 在更新任务状态时，检查version是否与数据库中的version一致
     * - 如果一致，说明任务没有被其他节点认领，可以成功认领
     * - 如果不一致，说明任务已被其他节点认领，本次认领失败
     * 
     * @param pendingTasks 待处理的任务列表
     * @return 成功认领的任务列表
     */
    private List<AsyncTask> claimTasksWithOptimisticLock(List<AsyncTask> pendingTasks) {
        List<AsyncTask> claimedTasks = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 遍历所有待处理任务，尝试认领
        for (AsyncTask task : pendingTasks) {
            try {
                // 使用乐观锁认领任务
                // 更新条件：status=0（待处理）AND version=当前版本
                // 更新内容：status=1（处理中），设置执行节点和开始时间，version+1
                int updatedRows = taskMapper.claimTask(
                        task.getId(),
                        task.getVersion(),
                        executeNode,
                        now
                );

                // 如果更新成功，说明任务认领成功
                if (updatedRows > 0) {
                    task.setStatus(TaskStatus.PROCESSING.getCode());
                    task.setExecuteNode(executeNode);
                    task.setExecuteStartTime(now);
                    task.setVersion(task.getVersion() + 1);
                    claimedTasks.add(task);
                }
            } catch (Exception e) {
                // 记录认领异常，但不影响其他任务的认领
                log.error("任务认领异常: taskId={}", task.getId(), e);
            }
        }

        return claimedTasks;
    }

    /**
     * 异步执行任务
     * 
     * 使用CompletableFuture实现异步执行，不阻塞调度线程
     * 每个任务都在独立的线程中执行，互不干扰
     * 
     * @param tasks 需要执行的任务列表
     */
    private void executeTasksAsync(List<AsyncTask> tasks) {
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        // 为每个任务创建一个异步执行任务
        for (AsyncTask task : tasks) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    // 调用任务执行服务执行任务
                    taskExecutorService.executeTask(task);
                } catch (Exception e) {
                    // 记录执行异常，但不影响其他任务的执行
                    log.error("任务执行异常: taskId={}", task.getId(), e);
                }
            }, asyncTaskExecutor);

            futures.add(future);
        }

        // 注意：这里不等待所有任务完成，让它们在后台异步执行
        // 如果需要等待所有任务完成，可以使用 CompletableFuture.allOf(futures).join();
    }

    /**
     * 定时处理超时任务 - 每分钟执行一次
     * 
     * 超时任务定义：
     * - 任务状态为PROCESSING（处理中）
     * - 任务执行时间超过设定的超时时间（默认300秒）
     * 
     * 处理方式：
     * - 将超时任务重置为PENDING（待处理）状态
     * - 增加重试次数
     * - 如果重试次数达到最大值，任务将被标记为FAILED（失败）
     * 
     * 注意事项：
     * - 使用乐观锁确保同一任务不会被多个节点同时处理
     * - 超时任务的处理也是异步的，不影响主调度流程
     */
    @Scheduled(fixedDelay = 60000) // 每分钟执行一次
    public void handleTimeoutTasks() {
        try {
            // 查询超时的任务
            // 查询条件：status=1（处理中）AND 当前时间 > 执行开始时间 + 超时时间
            List<AsyncTask> timeoutTasks = taskMapper.selectTimeoutTasks(100);
            
            // 如果没有超时任务，直接返回
            if (timeoutTasks.isEmpty()) {
                return;
            }

            log.warn("发现 {} 个超时任务", timeoutTasks.size());

            // 处理超时任务
            int handledCount = taskExecutorService.handleTimeoutTasks(timeoutTasks.size());
            
            if (handledCount > 0) {
                log.warn("成功处理 {} 个超时任务", handledCount);
            }

        } catch (Exception e) {
            // 记录异常，但不中断调度任务
            log.error("处理超时任务异常", e);
        }
    }

    /**
     * 获取当前执行节点标识
     * 
     * @return 执行节点标识
     */
    public String getExecuteNode() {
        return executeNode;
    }

    /**
     * 获取轮询间隔
     * 
     * @return 轮询间隔（毫秒）
     */
    public long getPollingInterval() {
        return pollingInterval;
    }

    /**
     * 获取批量认领任务数量
     * 
     * @return 批量认领任务数量
     */
    public int getBatchSize() {
        return batchSize;
    }
}
