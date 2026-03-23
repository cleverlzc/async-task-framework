package com.example.async.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 任务处理器注册中心
 * 
 * 负责管理所有任务处理器的注册和查找。
 * 使用线程安全的ConcurrentHashMap存储处理器，支持并发访问。
 * 
 * 工作流程：
 * 1. Spring容器启动时，所有实现TaskHandler接口的Bean会被自动注册
 * 2. 任务执行时，通过任务类型查找对应的处理器
 * 3. 如果找不到对应的处理器，会抛出异常
 * 
 * @author async-task-framework
 * @version 1.0.0
 */
@Slf4j
@Component
public class TaskHandlerRegistry {
    
    /**
     * 处理器映射表
     * 
     * Key: 任务类型标识
     * Value: 任务处理器实例
     * 
     * 使用ConcurrentHashMap保证线程安全，支持并发注册和查找
     */
    private final Map<String, TaskHandler> handlers = new ConcurrentHashMap<>();
    
    /**
     * 注册任务处理器
     * 
     * 将处理器注册到注册表中。
     * 如果已存在相同任务类型的处理器，会记录警告日志并覆盖。
     * 
     * @param handler 任务处理器实例
     * @throws IllegalArgumentException 如果handler为null或任务类型为空
     */
    public void register(TaskHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("任务处理器不能为null");
        }
        
        String taskType = handler.getTaskType();
        if (taskType == null || taskType.trim().isEmpty()) {
            throw new IllegalArgumentException("任务类型不能为空");
        }
        
        TaskHandler existing = handlers.put(taskType, handler);
        if (existing != null) {
            log.warn("任务处理器被覆盖: taskType={}, oldHandler={}, newHandler={}", 
                    taskType, existing.getClass().getName(), handler.getClass().getName());
        }
        
        log.info("任务处理器注册成功: taskType={}, handler={}, description={}", 
                taskType, handler.getClass().getName(), handler.getDescription());
    }
    
    /**
     * 根据任务类型获取处理器
     * 
     * @param taskType 任务类型标识
     * @return 任务处理器实例，如果不存在返回null
     */
    public TaskHandler getHandler(String taskType) {
        if (taskType == null || taskType.trim().isEmpty()) {
            return null;
        }
        return handlers.get(taskType);
    }
    
    /**
     * 检查指定任务类型的处理器是否存在
     * 
     * @param taskType 任务类型标识
     * @return 如果存在返回true，否则返回false
     */
    public boolean hasHandler(String taskType) {
        return handlers.containsKey(taskType);
    }
    
    /**
     * 注销任务处理器
     * 
     * 从注册表中移除指定的处理器。
     * 
     * @param taskType 任务类型标识
     * @return 如果存在并移除成功返回true，否则返回false
     */
    public boolean unregister(String taskType) {
        if (taskType == null || taskType.trim().isEmpty()) {
            return false;
        }
        
        TaskHandler removed = handlers.remove(taskType);
        if (removed != null) {
            log.info("任务处理器注销成功: taskType={}, handler={}", 
                    taskType, removed.getClass().getName());
            return true;
        }
        return false;
    }
    
    /**
     * 获取已注册的处理器数量
     * 
     * @return 处理器数量
     */
    public int size() {
        return handlers.size();
    }
    
    /**
     * 清空所有处理器
     * 
     * 注意：此方法主要用于测试场景，生产环境中不建议调用
     */
    public void clear() {
        handlers.clear();
        log.warn("所有任务处理器已被清空");
    }
    
    /**
     * 获取所有已注册的任务类型
     * 
     * @return 任务类型集合
     */
    public java.util.Set<String> getRegisteredTaskTypes() {
        return new java.util.HashSet<>(handlers.keySet());
    }
}