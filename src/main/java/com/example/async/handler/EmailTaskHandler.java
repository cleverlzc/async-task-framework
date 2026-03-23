package com.example.async.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 邮件任务处理器示例
 * 
 * 演示如何实现自定义任务处理器
 * 
 * 任务参数格式（JSON）：
 * {
 *   "to": "recipient@example.com",
 *   "subject": "邮件主题",
 *   "content": "邮件内容"
 * }
 * 
 * @author RelayAgent
 * @version 1.0.0
 */
@Slf4j
@Component
public class EmailTaskHandler implements TaskHandler<Map<String, String>, String> {

    private static final String TASK_TYPE = "EMAIL_SEND";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String handle(String payload) throws Exception {
        log.info("开始处理邮件发送任务");
        
        // 解析任务参数
        Map<String, String> emailParams = objectMapper.readValue(payload, 
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
        
        String to = emailParams.get("to");
        String subject = emailParams.get("subject");
        String content = emailParams.get("content");
        
        log.info("邮件参数 - 收件人: {}, 主题: {}", to, subject);
        
        // 模拟邮件发送（实际项目中替换为真实的邮件发送逻辑）
        sendEmail(to, subject, content);
        
        String result = String.format("邮件发送成功: to=%s, subject=%s", to, subject);
        log.info(result);
        
        return result;
    }

    @Override
    public String getTaskType() {
        return TASK_TYPE;
    }

    @Override
    public Integer getDefaultTimeout() {
        return 60; // 邮件发送超时时间60秒
    }

    @Override
    public Integer getDefaultMaxRetry() {
        return 3; // 邮件发送最大重试3次
    }

    /**
     * 模拟发送邮件
     * 实际项目中应替换为真实的邮件发送逻辑（如JavaMail、Spring Mail等）
     */
    private void sendEmail(String to, String subject, String content) throws Exception {
        // 模拟网络延迟
        Thread.sleep(500);
        
        // 模拟10%的失败率（用于测试重试机制）
        if (Math.random() < 0.1) {
            throw new Exception("邮件发送失败: SMTP服务不可用");
        }
        
        // 实际项目中，这里应该调用邮件服务API或使用JavaMail发送邮件
        // 例如：
        // JavaMailSender.send(mimeMessage);
        log.debug("邮件已发送至: {}", to);
    }
}
