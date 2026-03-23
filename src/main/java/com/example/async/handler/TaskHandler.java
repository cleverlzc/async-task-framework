package com.example.async.handler;

/**
 * 异步任务处理器接口
 * 
 * 所有任务处理器必须实现此接口，定义任务的具体执行逻辑。
 * 通过实现此接口，可以轻松扩展新的任务类型。
 * 
 * @author async-task-framework
 * @version 1.0.0
 */
public interface TaskHandler {
    
    /**
     * 获取任务类型标识
     * 
     * 每个处理器对应唯一的任务类型，用于在注册表中查找对应的处理器。
     * 任务类型通常使用业务相关的命名，如 "EMAIL_SEND"、"REPORT_GENERATE" 等。
     * 
     * @return 任务类型标识
     */
    String getTaskType();
    
    /**
     * 执行任务
     * 
     * 定义任务的具体执行逻辑。此方法由任务执行服务调用。
     * 
     * 注意事项：
     * 1. 此方法在线程池中异步执行，不应阻塞过长时间
     * 2. 抛出的异常会被捕获并记录到任务执行日志中
     * 3. 返回值会被序列化为JSON存储到任务结果中
     * 4. 建议添加详细的日志记录，便于问题排查
     * 
     * @param payload 任务载荷，JSON格式的字符串
     *               包含任务执行所需的参数，由调用方传入
     * @return 任务执行结果，会被序列化为JSON存储
     * @throws Exception 任务执行失败时抛出异常
     */
    String handle(String payload) throws Exception;
    
    /**
     * 获取处理器描述信息
     * 
     * 返回处理器的功能描述，用于文档和日志记录。
     * 
     * @return 处理器描述
     */
    default String getDescription() {
        return "任务处理器: " + getTaskType();
    }
    
    /**
     * 获取默认超时时间（秒）
     * 
     * 返回此处理器建议的超时时间。
     * 如果返回null，则使用全局配置的默认超时时间（300秒）。
     * 
     * @return 超时时间（秒），null表示使用默认值
     */
    default Integer getDefaultTimeout() {
        return null;
    }
    
    /**
     * 获取默认最大重试次数
     * 
     * 返回此处理器建议的最大重试次数。
     * 如果返回null，则使用全局配置的默认重试次数（3次）。
     * 
     * @return 最大重试次数，null表示使用默认值
     */
    default Integer getDefaultMaxRetry() {
        return null;
    }
}