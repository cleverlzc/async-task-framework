package com.example.async.entity;

import com.example.async.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 异步任务实体类
 * 对应数据库表 async_task
 * 
 * 核心特性：
 * 1. 乐观锁机制：通过version字段防止并发重复执行
 * 2. 幂等性保证：通过businessKey保证同一业务只执行一次
 * 3. 优先级调度：通过priority字段实现优先级队列
 * 4. 超时控制：通过timeoutSeconds字段控制任务执行超时时间
 * 5. 重试机制：通过retryCount和maxRetry字段实现自动重试
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncTask {
    
    /**
     * 任务ID，主键，自增
     */
    private Long id;
    
    /**
     * 任务类型，标识任务的业务类型
     * 例如：EMAIL、SMS、REPORT等
     * 用于路由到对应的TaskHandler处理器
     */
    private String taskType;
    
    /**
     * 业务键，用于保证幂等性
     * 同一个businessKey的任务只会执行一次
     * 例如：订单ID、用户ID等业务唯一标识
     * 数据库中有唯一索引约束
     */
    private String businessKey;
    
    /**
     * 任务载荷，JSON格式的字符串
     * 存储任务执行所需的业务数据
     * 例如：{"orderId": 123, "userId": 456, "amount": 100.00}
     */
    private String payload;
    
    /**
     * 任务优先级，数值越小优先级越高
     * 取值范围：1-10，默认值为5
     * 1=最高优先级，10=最低优先级
     * 调度器会优先执行高优先级的任务
     */
    private Integer priority;
    
    /**
     * 任务状态，对应TaskStatus枚举
     * 0=PENDING（待处理）
     * 1=PROCESSING（处理中）
     * 2=SUCCESS（成功）
     * 3=FAILED（失败）
     * 4=CANCELLED（已取消）
     */
    private Integer status;
    
    /**
     * 当前重试次数
     * 任务失败后会自动重试，每次重试此字段+1
     */
    private Integer retryCount;
    
    /**
     * 最大重试次数
     * 超过此次数后任务将不再重试，状态保持为FAILED
     * 默认值为3
     */
    private Integer maxRetry;
    
    /**
     * 错误信息，任务失败时记录的异常信息
     * 只保留最后一次失败的错误信息
     */
    private String errorMsg;
    
    /**
     * 任务执行结果，JSON格式的字符串
     * 任务成功后存储执行结果
     * 例如：{"success": true, "message": "邮件发送成功"}
     */
    private String result;
    
    /**
     * 执行节点标识
     * 标识任务在哪个节点上执行
     * 多节点部署时用于追踪任务分布
     */
    private String executeNode;
    
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
     * 超时时间（秒）
     * 任务执行的最大允许时间
     * 超过此时间未完成的任务会被标记为超时并重试
     * 默认值为300秒（5分钟）
     */
    private Integer timeoutSeconds;
    
    /**
     * 乐观锁版本号
     * 用于防止并发重复执行
     * 每次更新任务状态时version字段+1
     * 更新时检查version是否匹配，不匹配则更新失败
     */
    private Integer version;
    
    /**
     * 创建时间
     * 任务创建的时间戳
     */
    private LocalDateTime createTime;
    
    /**
     * 更新时间
     * 任务最后更新的时间戳
     * 每次任务状态变更都会更新此字段
     */
    private LocalDateTime updateTime;
    
    /**
     * 获取任务状态枚举
     * 
     * @return TaskStatus枚举值
     */
    public TaskStatus getStatusEnum() {
        return TaskStatus.fromCode(this.status);
    }
    
    /**
     * 判断任务是否可以执行
     * 满足以下条件可以执行：
     * 1. 状态为PENDING（待处理）
     * 2. 重试次数未达到上限
     * 
     * @return true表示可以执行，false表示不能执行
     */
    public boolean canExecute() {
        TaskStatus taskStatus = getStatusEnum();
        return taskStatus == TaskStatus.PENDING && this.retryCount < this.maxRetry;
    }
    
    /**
     * 判断任务是否超时
     * 满足以下条件表示超时：
     * 1. 状态为PROCESSING（处理中）
     * 2. executeStartTime不为空
     * 3. timeoutSeconds不为空
     * 4. 当前时间超过 executeStartTime + timeoutSeconds
     * 
     * @return true表示已超时，false表示未超时
     */
    public boolean isTimeout() {
        if (this.executeStartTime == null || this.timeoutSeconds == null) {
            return false;
        }
        LocalDateTime timeoutTime = this.executeStartTime.plusSeconds(this.timeoutSeconds);
        return LocalDateTime.now().isAfter(timeoutTime);
    }
    
    /**
     * 判断任务是否还可以重试
     * 满足以下条件可以重试：
     * 1. 状态为FAILED（失败）
     * 2. 重试次数未达到上限
     * 
     * @return true表示可以重试，false表示不能重试
     */
    public boolean canRetry() {
        TaskStatus taskStatus = getStatusEnum();
        return taskStatus == TaskStatus.FAILED && this.retryCount < this.maxRetry;
    }
    
    /**
     * 判断任务是否为最终状态
     * 最终状态包括：SUCCESS、FAILED（重试次数耗尽）、CANCELLED
     * 
     * @return true表示是最终状态，false表示可以继续流转
     */
    public boolean isFinalStatus() {
        TaskStatus taskStatus = getStatusEnum();
        if (taskStatus == null) {
            return false;
        }
        return taskStatus.isFinal();
    }
    
    /**
     * 获取任务执行耗时（毫秒）
     * 
     * @return 执行耗时，如果任务未完成则返回null
     */
    public Long getDuration() {
        if (this.executeStartTime != null && this.executeEndTime != null) {
            return java.time.Duration.between(this.executeStartTime, this.executeEndTime).toMillis();
        }
        return null;
    }
}
