-- 异步任务框架数据库初始化脚本
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS async_task DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE async_task;

-- ============================================
-- 异步任务表
-- 用于存储异步任务的基本信息和状态
-- ============================================
DROP TABLE IF EXISTS `async_task`;

CREATE TABLE `async_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务ID，主键自增',
    `task_type` VARCHAR(100) NOT NULL COMMENT '任务类型，对应Handler的bean名称',
    `business_key` VARCHAR(255) NOT NULL COMMENT '业务唯一标识，用于幂等性保证',
    `payload` JSON NOT NULL COMMENT '任务参数，JSON格式',
    `priority` INT NOT NULL DEFAULT 5 COMMENT '任务优先级，1-10，数字越小优先级越高',
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '任务状态：0-PENDING待处理，1-PROCESSING处理中，2-SUCCESS成功，3-FAILED失败，4-CANCELLED已取消',
    `retry_count` INT NOT NULL DEFAULT 0 COMMENT '当前重试次数',
    `max_retry` INT NOT NULL DEFAULT 3 COMMENT '最大重试次数',
    `error_msg` TEXT COMMENT '错误信息，最后一次失败时的错误堆栈',
    `result` JSON COMMENT '执行结果，JSON格式',
    `execute_node` VARCHAR(100) COMMENT '执行节点标识',
    `execute_start_time` DATETIME COMMENT '任务开始执行时间',
    `execute_end_time` DATETIME COMMENT '任务结束执行时间',
    `timeout_seconds` INT NOT NULL DEFAULT 300 COMMENT '超时时间（秒）',
    `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，用于防止重复执行',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_business_key` (`business_key`),
    KEY `idx_task_type` (`task_type`),
    KEY `idx_status_priority` (`status`, `priority`),
    KEY `idx_create_time` (`create_time`),
    KEY `idx_execute_node` (`execute_node`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异步任务表';

-- ============================================
-- 任务执行日志表
-- 用于记录每次任务执行的详细日志
-- ============================================
DROP TABLE IF EXISTS `task_execution_log`;

CREATE TABLE `task_execution_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志ID，主键自增',
    `task_id` BIGINT NOT NULL COMMENT '关联的任务ID',
    `execute_node` VARCHAR(100) NOT NULL COMMENT '执行节点标识',
    `status` TINYINT NOT NULL COMMENT '执行状态：2-SUCCESS成功，3-FAILED失败',
    `error_msg` TEXT COMMENT '错误信息',
    `execute_start_time` DATETIME NOT NULL COMMENT '执行开始时间',
    `execute_end_time` DATETIME NOT NULL COMMENT '执行结束时间',
    `duration_ms` BIGINT NOT NULL COMMENT '执行耗时（毫秒）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务执行日志表';

-- ============================================
-- 任务统计表
-- 用于按小时聚合统计任务执行情况
-- ============================================
DROP TABLE IF EXISTS `task_statistics`;

CREATE TABLE `task_statistics` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '统计ID，主键自增',
    `task_type` VARCHAR(100) NOT NULL COMMENT '任务类型',
    `stat_hour` DATETIME NOT NULL COMMENT '统计小时，格式：yyyy-MM-dd HH:00:00',
    `total_count` INT NOT NULL DEFAULT 0 COMMENT '总任务数',
    `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功任务数',
    `failed_count` INT NOT NULL DEFAULT 0 COMMENT '失败任务数',
    `avg_duration_ms` BIGINT NOT NULL DEFAULT 0 COMMENT '平均执行耗时（毫秒）',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_task_type_hour` (`task_type`, `stat_hour`),
    KEY `idx_stat_hour` (`stat_hour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='任务统计表';

-- ============================================
-- 初始化示例数据（可选）
-- ============================================
-- 插入一个示例任务
INSERT INTO `async_task` 
    (`task_type`, `business_key`, `payload`, `priority`, `status`, `max_retry`, `timeout_seconds`)
VALUES 
    ('emailTaskHandler', 'EMAIL-20240101-001', 
     JSON_OBJECT('to', 'user@example.com', 'subject', 'Welcome', 'content', 'Hello World!'),
     5, 0, 3, 300);

-- ============================================
-- 创建索引优化查询性能
-- ============================================
-- 为async_task表添加复合索引，优化任务查询
CREATE INDEX idx_status_priority_create ON async_task(status, priority, create_time);

-- 为task_execution_log表添加复合索引，优化日志查询
CREATE INDEX idx_task_create ON task_execution_log(task_id, create_time);