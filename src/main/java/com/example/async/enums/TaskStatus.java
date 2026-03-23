package com.example.async.enums;

import lombok.Getter;

/**
 * 任务状态枚举
 * 定义异步任务的所有可能状态
 * 
 * 状态流转规则：
 * PENDING -> PROCESSING -> SUCCESS/FAILED
 * FAILED -> PROCESSING (重试) -> SUCCESS/FAILED
 * PENDING/PROCESSING -> CANCELLED
 */
@Getter
public enum TaskStatus {
    /**
     * 待处理
     * 任务已创建，等待被调度器认领执行
     */
    PENDING(0, "待处理"),
    
    /**
     * 处理中
     * 任务已被调度器认领，正在执行中
     */
    PROCESSING(1, "处理中"),
    
    /**
     * 成功
     * 任务执行成功，这是最终状态
     */
    SUCCESS(2, "成功"),
    
    /**
     * 失败
     * 任务执行失败，如果重试次数未达到上限，可以重试
     * 这是最终状态（当重试次数达到上限时）
     */
    FAILED(3, "失败"),
    
    /**
     * 已取消
     * 任务被手动取消，这是最终状态
     */
    CANCELLED(4, "已取消");
    
    /**
     * 状态码，存储在数据库中的整数值
     */
    private final int code;
    
    /**
     * 状态描述，用于日志和展示
     */
    private final String desc;
    
    TaskStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }
    
    /**
     * 根据状态码获取枚举值
     * 
     * @param code 状态码
     * @return 对应的枚举值，如果不存在则返回null
     */
    public static TaskStatus fromCode(int code) {
        for (TaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
    
    /**
     * 判断是否为最终状态
     * 最终状态包括：SUCCESS、FAILED、CANCELLED
     * 
     * @return true表示是最终状态，false表示可以继续流转
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED;
    }
    
    /**
     * 判断是否可以重试
     * 只有FAILED状态可以重试
     * 
     * @return true表示可以重试，false表示不能重试
     */
    public boolean canRetry() {
        return this == FAILED;
    }
    
    /**
     * 判断是否为待处理状态
     * 
     * @return true表示是待处理状态
     */
    public boolean isPending() {
        return this == PENDING;
    }
    
    /**
     * 判断是否为处理中状态
     * 
     * @return true表示是处理中状态
     */
    public boolean isProcessing() {
        return this == PROCESSING;
    }
    
    /**
     * 判断是否执行成功
     * 
     * @return true表示执行成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
    
    /**
     * 判断是否执行失败
     * 
     * @return true表示执行失败
     */
    public boolean isFailed() {
        return this == FAILED;
    }
}
