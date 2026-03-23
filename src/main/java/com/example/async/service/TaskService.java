package com.example.async.service;

import com.example.async.dto.TaskQueryResponse;
import com.example.async.dto.TaskSubmitRequest;
import com.example.async.entity.AsyncTask;
import com.example.async.entity.TaskExecutionLog;
import com.example.async.enums.TaskStatus;
import com.example.async.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 异步任务服务
 * 提供任务的提交、查询、统计等业务功能
 * 
 * @author RelayAgent
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {
    
    private final TaskMapper taskMapper;
    
    /**
     * 提交异步任务
     * 
     * @param request 任务提交请求
     * @return 任务ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submitTask(TaskSubmitRequest request) {
        // 检查是否已存在相同businessKey的任务
        AsyncTask existingTask = taskMapper.selectByBusinessKey(request.getBusinessKey());
        if (existingTask != null) {
            log.info("任务已存在，返回已有任务ID: businessKey={}, taskId={}", 
                    request.getBusinessKey(), existingTask.getId());
            return existingTask.getId();
        }
        
        // 构建新任务
        AsyncTask task = AsyncTask.builder()
                .taskType(request.getTaskType())
                .businessKey(request.getBusinessKey())
                .payload(request.getPayload())
                .priority(request.getPriority())
                .status(TaskStatus.PENDING.getCode())
                .retryCount(0)
                .maxRetry(request.getMaxRetry())
                .timeoutSeconds(request.getTimeoutSeconds())
                .version(0)
                .build();
        
        // 插入任务
        int result = taskMapper.insert(task);
        if (result <= 0) {
            throw new RuntimeException("任务提交失败");
        }
        
        log.info("任务提交成功: taskId={}, taskType={}, businessKey={}", 
                task.getId(), task.getTaskType(), task.getBusinessKey());
        
        return task.getId();
    }
    
    /**
     * 根据任务ID查询任务详情
     * 
     * @param taskId 任务ID
     * @return 任务查询响应
     */
    public TaskQueryResponse queryTask(Long taskId) {
        AsyncTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return null;
        }
        
        return convertToResponse(task);
    }
    
    /**
     * 根据业务键查询任务详情
     * 
     * @param businessKey 业务键
     * @return 任务查询响应
     */
    public TaskQueryResponse queryByBusinessKey(String businessKey) {
        AsyncTask task = taskMapper.selectByBusinessKey(businessKey);
        if (task == null) {
            return null;
        }
        
        return convertToResponse(task);
    }
    
    /**
     * 查询任务的执行日志
     * 
     * @param taskId 任务ID
     * @return 执行日志列表
     */
    public List<TaskQueryResponse.ExecutionLogItem> queryExecutionLogs(Long taskId) {
        List<TaskExecutionLog> logs = taskMapper.selectExecutionLogs(taskId);
        
        return logs.stream()
                .map(log -> TaskQueryResponse.ExecutionLogItem.builder()
                        .id(log.getId())
                        .executeNode(log.getExecuteNode())
                        .status(TaskStatus.fromCode(log.getStatus()))
                        .errorMsg(log.getErrorMsg())
                        .executeStartTime(log.getExecuteStartTime())
                        .executeEndTime(log.getExecuteEndTime())
                        .durationMs(log.getDurationMs())
                        .build())
                .collect(Collectors.toList());
    }
    
    /**
     * 获取任务统计信息
     * 
     * @return 统计信息Map，key为状态码，value为任务数量
     */
    public Map<String, Object> getStatistics() {
        List<Map<String, Object>> statusCounts = taskMapper.countByStatus();
        
        Map<String, Object> statistics = new HashMap<>();
        for (Map<String, Object> item : statusCounts) {
            Integer status = (Integer) item.get("status");
            Long count = (Long) item.get("count");
            TaskStatus taskStatus = TaskStatus.fromCode(status);
            
            if (taskStatus != null) {
                statistics.put(taskStatus.name(), count);
            }
        }
        
        return statistics;
    }
    
    /**
     * 将实体转换为响应DTO
     * 
     * @param task 任务实体
     * @return 响应DTO
     */
    private TaskQueryResponse convertToResponse(AsyncTask task) {
        return TaskQueryResponse.builder()
                .id(task.getId())
                .taskType(task.getTaskType())
                .businessKey(task.getBusinessKey())
                .payload(task.getPayload())
                .priority(task.getPriority())
                .status(TaskStatus.fromCode(task.getStatus()))
                .statusDesc(TaskStatus.fromCode(task.getStatus()) != null 
                        ? TaskStatus.fromCode(task.getStatus()).getDesc() : "未知")
                .retryCount(task.getRetryCount())
                .maxRetry(task.getMaxRetry())
                .errorMsg(task.getErrorMsg())
                .result(task.getResult())
                .executeNode(task.getExecuteNode())
                .executeStartTime(task.getExecuteStartTime())
                .executeEndTime(task.getExecuteEndTime())
                .timeoutSeconds(task.getTimeoutSeconds())
                .createTime(task.getCreateTime())
                .updateTime(task.getUpdateTime())
                .build();
    }
}