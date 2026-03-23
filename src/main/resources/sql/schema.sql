-- ========================================
-- 异步任务处理框架 - 数据库初始化脚本
-- ========================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS async_task_db 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE async_task_db;

-- ========================================
-- 任务主表
-- ========================================
DROP TABLE IF EXISTS async_task;

CREATE TABLE async_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    task_type VARCHAR(64) NOT NULL COMMENT '任务类型',
    business_key VARCHAR(128) COMMENT '业务主键',
    payload JSON COMMENT '任务参数（JSON格式）',
    priority TINYINT DEFAULT 5 COMMENT '优先级（1-10，数字越小优先级越高）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-待处理 1-处理中 2-成功 3-失败 4-取消',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    max_retry INT DEFAULT 3 COMMENT '最大重试次数',
    error_msg TEXT COMMENT '错误信息',
    result JSON COMMENT '执行结果',
    execute_node VARCHAR(64) COMMENT '执行节点标识',
    execute_start_time DATETIME COMMENT '开始执行时间',
    execute_end_time DATETIME COMMENT '结束执行时间',
    timeout_seconds INT DEFAULT 300 COMMENT '超时时间（秒）',
    version INT DEFAULT 0 COMMENT '乐观锁版本号',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    -- 索引
    INDEX idx_status_priority (status, priority),
    INDEX idx_business_key (business_key),
    INDEX idx_create_time (create_time),
    INDEX idx_execute_node (execute_node),
    INDEX idx_task_type (task_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异步任务表';

-- ========================================
-- 任务执行日志表
-- ========================================
DROP TABLE IF EXISTS task_execution_log;

CREATE TABLE task_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
    task_id BIGINT NOT NULL COMMENT '任务ID',
    execute_node VARCHAR(64) COMMENT '执行节点',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    duration_ms INT COMMENT '执行耗时（毫秒）',
    status TINYINT NOT NULL COMMENT '状态：0-成功 1-失败',
    error_msg TEXT COMMENT '错误信息',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    
    -- 索引
    INDEX idx_task_id (task_id),
    INDEX idx_execute_node (execute_node),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行日志表';

-- ========================================
-- 插入测试数据（可选）
-- ========================================
-- INSERT INTO async_task (task_type, business_key, payload, priority, status)
-- VALUES 
-- ('EMAIL_SEND', 'TEST001', '{"to":"test@example.com","subject":"测试邮件","content":"这是一封测试邮件"}', 5, 0),
-- ('REPORT_GENERATE', 'TEST002', '{"reportType":"daily","date":"2024-01-01"}', 3, 0);
