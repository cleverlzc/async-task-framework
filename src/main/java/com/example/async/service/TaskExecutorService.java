package com.example.async.service;

import com.example.async.entity.AsyncTask;
import com.example.async.entity.TaskExecutionLog;
import com.example.async.enums.TaskStatus;
import com.example.async.handler.TaskHandler;
import com.example.async.handler.TaskHandlerRegistry;
import com.example.async.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 异步任务执行服务
 * 负责任务的执行、状态更新、日志记录等核心逻辑
 * 
 * @author RelayAgent
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutorService {
    
    private final TaskMapper taskMapper;
    private final TaskHandlerRegistry handlerRegistry;
    
    /**
     * 执行单个任务
     * 
     * @param task 待执行的任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void executeTask(AsyncTask task) {
        Long taskId = task.getId();
        String taskType = task.getTaskType();
        LocalDateTime executeStartTime = LocalDateTime.now();
        
        log.info("开始执行任务: taskId={}, taskType={}, businessKey={}", 
                taskId, taskType, task.getBusinessKey());
        
        TaskExecutionLog executionLog = null;
        String result = null;
        String errorMsg = null;
        TaskStatus finalStatus = TaskStatus.SUCCESS;
        
        try {
            // 获取对应的Handler
            TaskHandler handler = handlerRegistry.getHandler(taskType);
            if (handler == null) {
                throw new RuntimeException("未找到任务处理器: " + taskType);
            }
            
            // 执行任务
            result = handler.handle(task.getPayload());
            
            log.info("任务执行成功: taskId={}, result={}", taskId, result);
            
        } catch (Exception e) {
            errorMsg = e.getMessage();
            finalStatus = TaskStatus.FAILED;
            log.error("任务执行失败: taskId={}, error={}", taskId, errorMsg, e);
        } finally {
            LocalDateTime executeEndTime = LocalDateTime.now();
            long durationMs = java.time.Duration.between(executeStartTime, executeEndTime).toMillis();
            
            // 记录执行日志
            executionLog = TaskExecutionLog.builder()
                    .taskId(taskId)
                    .executeNode(task.getExecuteNode())
                    .status(finalStatus.getCode())
                    .errorMsg(errorMsg)
                    .executeStartTime(executeStartTime)
                    .executeEndTime(executeEndTime)
                    .durationMs(durationMs)
                    .build();
            
            taskMapper.insertExecutionLog(executionLog);
            
            // 更新任务状态
            if (finalStatus == TaskStatus.SUCCESS) {
                updateTaskToSuccess(taskId, task.getVersion(), result, executeEndTime);
            } else {
                updateTaskToFailed(taskId, task.getVersion(), errorMsg, executeEndTime);
            }
        }
    }
    
    /**
     * 更新任务为成功状态
     * 
     * @param taskId 任务ID
     * @param version 版本号（乐观锁）
     * @param result 执行结果
     * @param executeEndTime 执行结束时间
     */
    private void updateTaskToSuccess(Long taskId, Integer version, String result, LocalDateTime executeEndTime) {
        int rows = taskMapper.updateToSuccess(taskId, version, result, executeEndTime);
        if (rows <= 0) {
            log.warn("更新任务状态失败（乐观锁冲突）: taskId={}, version={}", taskId, version);
        } else {
            log.info("任务状态更新为成功: taskId={}", taskId);
        }
    }
    
    /**
     * 更新任务为失败状态
     * 
     * @param taskId 任务ID
     * @param version 版本号（乐观锁）
     * @param errorMsg 错误信息
     * @param executeEndTime 执行结束时间
     */
    private void updateTaskToFailed(Long taskId, Integer version, String errorMsg, LocalDateTime executeEndTime) {
        int rows = taskMapper.updateToFailed(taskId, version, errorMsg, executeEndTime);
        if (rows <= 0) {
            log.warn("更新任务状态失败（乐观锁冲突）: taskId={}, version={}", taskId, version);
        } else {
            log.info("任务状态更新为失败: taskId={}", taskId);
        }
    }
    
    /**
     * 处理超时任务
     * 将超时的任务重置为待处理状态，并增加重试次数
     * 
     * @param limit 处理数量限制
     */
    @Transactional(rollbackFor = Exception.class)
    public int handleTimeoutTasks(int limit) {
        List<AsyncTask> timeoutTasks = taskMapper.selectTimeoutTasks(limit);
        if (timeoutTasks.isEmpty()) {
            return 0;
        }
        
        log.info("发现超时任务: count={}", timeoutTasks.size());
        
        int handledCount = 0;
        for (AsyncTask task : timeoutTasks) {
            try {
                // 重置超时任务
                int rows = taskMapper.resetTimeoutTask(task.getId(), task.getVersion());
                if (rows > 0) {
                    handledCount++;
                    log.info("超时任务已重置: taskId={}, retryCount={}", 
                            task.getId(), task.getRetryCount() + 1);
                }
            } catch (Exception e) {
                log.error("重置超时任务失败: taskId={}", task.getId(), e);
            }
        }
        
        return handledCount;
    }
}
