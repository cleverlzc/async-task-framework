package com.example.async.dto;

import com.example.async.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步任务查询响应DTO
 * 
 * @author RelayAgent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskQueryResponse {
    
    /**
     * 任务ID
     */
    private Long id;
    
    /**
     * 任务类型
     */
    private String taskType;
    
    /**
     * 业务键
     */
    private String businessKey;
    
    /**
     * 任务载荷
     */
    private String payload;
    
    /**
     * 任务优先级
     */
    private Integer priority;
    
    /**
     * 任务状态
     */
    private TaskStatus status;
    
    /**
     * 状态描述
     */
    private String statusDesc;
    
    /**
     * 重试次数
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     */
    private Integer maxRetry;
    
    /**
     * 错误信息
     */
    private String errorMsg;
    
    /**
     * 执行结果
     */
    private String result;
    
    /**
     * 执行节点
     */
    private String executeNode;
    
    /**
     * 执行开始时间
     */
    private LocalDateTime executeStartTime;
    
    /**
     * 执行结束时间
     */
    private LocalDateTime executeEndTime;
    
    /**
     * 超时时间（秒）
     */
    private Integer timeoutSeconds;
    
    /**
     * 创建时间
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updateTime;
    
    /**
     * 是否可重试
     */
    public boolean canRetry() {
        return status != null && status.canRetry() && retryCount < maxRetry;
    }
    
    /**
     * 是否为最终状态
     */
    public boolean isFinal() {
        return status != null && status.isFinal();
    }
    
    /**
     * 执行耗时（毫秒）
     */
    public Long getDuration() {
        if (executeStartTime != null && executeEndTime != null) {
            return java.time.Duration.between(executeStartTime, executeEndTime).toMillis();
        }
        return null;
    }
    
    /**
     * 执行日志列表（可选字段）
     */
    private List<ExecutionLogItem> executionLogs;
    
    /**
     * 执行日志项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionLogItem {
        private Long id;
        private String executeNode;
        private TaskStatus status;
        private String errorMsg;
        private LocalDateTime executeStartTime;
        private LocalDateTime executeEndTime;
        private Long durationMs;
    }
}
