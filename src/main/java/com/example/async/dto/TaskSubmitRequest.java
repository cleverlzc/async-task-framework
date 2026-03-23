package com.example.async.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 异步任务提交请求DTO
 * 
 * @author RelayAgent
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmitRequest {
    
    /**
     * 任务类型，用于路由到对应的Handler
     * 例如：EMAIL、SMS、NOTIFICATION等
     */
    @NotBlank(message = "任务类型不能为空")
    private String taskType;
    
    /**
     * 业务键，用于保证幂等性
     * 相同的businessKey只会创建一个任务
     * 例如：ORDER_ID_12345、USER_PAYMENT_67890
     */
    @NotBlank(message = "业务键不能为空")
    private String businessKey;
    
    /**
     * 任务载荷，JSON格式的业务数据
     * 例如：{"to":"user@example.com","subject":"订单通知","content":"您的订单已创建"}
     */
    @NotBlank(message = "任务载荷不能为空")
    private String payload;
    
    /**
     * 任务优先级，1-10，数字越小优先级越高
     * 默认值为5，建议使用1-3表示高优先级，4-7表示普通优先级，8-10表示低优先级
     */
    @Builder.Default
    private Integer priority = 5;
    
    /**
     * 任务超时时间（秒）
     * 默认300秒（5分钟），根据业务场景可调整
     */
    @Builder.Default
    private Integer timeoutSeconds = 300;
    
    /**
     * 最大重试次数
     * 默认3次，超过此次数后任务将标记为最终失败
     */
    @Builder.Default
    private Integer maxRetry = 3;
}