package com.example.async.entity;

import com.example.async.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 任务执行日志实体类
 * 对应数据库表 task_execution_log
 * 
 * 用途：
 * 1. 记录每次任务执行的详细信息（包括重试）
 * 2. 追踪任务执行历史，便于问题排查
 * 3. 统计任务执行耗时，用于性能分析
 * 
 * 设计要点：
 * - 每次任务执行（包括重试）都会插入一条日志
 * - 日志与任务一对多关系，一个任务可以有多条执行日志
 * - 日志记录完整的执行时间范围和耗时
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionLog {
    
    /**
     * 日志ID，主键，自增
     */
    private Long id;
    
    /**
     * 任务ID，关联async_task表
     * 标识这条日志属于哪个任务
     */
    private Long taskId;
    
    /**
     * 执行节点标识
     * 标识任务在哪个节点上执行
     * 多节点部署时用于追踪任务分布
     */
    private String executeNode;
    
    /**
     * 执行状态，对应TaskStatus枚举
     * 0=PENDING（不应出现）
     * 1=PROCESSING（不应出现）
     * 2=SUCCESS（执行成功）
     * 3=FAILED（执行失败）
     * 4=CANCELLED（任务取消）
     */
    private Integer status;
    
    /**
     * 错误信息
     * 任务失败时记录的异常信息
     * 成功时此字段为null
     */
    private String errorMsg;
    
    /**
     * 执行开始时间
     * 任务开始执行的时间戳
     */
    private LocalDateTime executeStartTime;
    
    /**
     * 执行结束时间
     * 任务完成执行的时间戳（成功或失败）
     */
    private LocalDateTime executeEndTime;
    
    /**
     * 执行耗时（毫秒）
     * executeEndTime - executeStartTime
     */
    private Long durationMs;
    
    /**
     * 创建时间
     * 日志创建的时间戳
     */
    private LocalDateTime createTime;
    
    /**
     * 获取任务状态枚举
     * 
     * @return TaskStatus枚举值
     */
    public TaskStatus getStatusEnum() {
        return TaskStatus.fromCode(this.status);
    }
    
    /**
     * 判断执行是否成功
     * 
     * @return true表示执行成功，false表示执行失败
     */
    public boolean isSuccess() {
        return getStatusEnum() == TaskStatus.SUCCESS;
    }
    
    /**
     * 判断执行是否失败
     * 
     * @return true表示执行失败，false表示执行成功
     */
    public boolean isFailed() {
        return getStatusEnum() == TaskStatus.FAILED;
    }
    
    /**
     * 判断执行是否被取消
     * 
     * @return true表示被取消，false表示未被取消
     */
    public boolean isCancelled() {
        return getStatusEnum() == TaskStatus.CANCELLED;
    }
    
    /**
     * 获取执行耗时（秒）
     * 
     * @return 执行耗时（秒），保留2位小数
     */
    public Double getDurationSeconds() {
        if (this.durationMs == null) {
            return null;
        }
        return this.durationMs / 1000.0;
    }
    
    /**
     * 判断是否为超时执行
     * 需要结合AsyncTask的timeoutSeconds字段判断
     * 
     * @param timeoutSeconds 任务超时时间（秒）
     * @return true表示超时，false表示未超时
     */
    public boolean isTimeout(int timeoutSeconds) {
        if (this.durationMs == null) {
            return false;
        }
        long timeoutMs = timeoutSeconds * 1000L;
        return this.durationMs > timeoutMs;
    }
}