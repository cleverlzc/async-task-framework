package com.example.async.repository;

import com.example.async.entity.AsyncTask;
import com.example.async.entity.TaskExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步任务Mapper接口
 * 提供任务和执行日志的数据库操作
 * 
 * @author RelayAgent
 */
@Mapper
public interface TaskMapper {

    /**
     * 插入新任务
     * 
     * @param task 任务实体
     * @return 影响行数
     */
    int insert(AsyncTask task);

    /**
     * 根据ID查询任务
     * 
     * @param id 任务ID
     * @return 任务实体
     */
    AsyncTask selectById(@Param("id") Long id);

    /**
     * 根据业务主键查询任务
     * 
     * @param businessKey 业务主键
     * @return 任务实体
     */
    AsyncTask selectByBusinessKey(@Param("businessKey") String businessKey);

    /**
     * 批量查询待处理任务（按优先级排序）
     * 用于定时调度器拉取待执行任务
     * 
     * @param limit 查询数量限制
     * @return 待处理任务列表
     */
    List<AsyncTask> selectPendingTasks(@Param("limit") int limit);

    /**
     * 使用乐观锁认领任务
     * 只有当任务状态为PENDING且version匹配时才能认领成功
     * 
     * @param id 任务ID
     * @param version 当前版本号
     * @param executeNode 执行节点标识
     * @return 影响行数（0表示认领失败，1表示认领成功）
     */
    int claimTaskWithOptimisticLock(@Param("id") Long id,
                                    @Param("version") Integer version,
                                    @Param("executeNode") String executeNode);

    /**
     * 更新任务为执行成功状态
     * 
     * @param id 任务ID
     * @param version 当前版本号
     * @param result 执行结果（JSON格式）
     * @return 影响行数
     */
    int updateTaskSuccess(@Param("id") Long id,
                         @Param("version") Integer version,
                         @Param("result") String result);

    /**
     * 更新任务为失败状态
     * 
     * @param id 任务ID
     * @param version 当前版本号
     * @param errorMsg 错误信息
     * @return 影响行数
     */
    int updateTaskFailed(@Param("id") Long id,
                        @Param("version") Integer version,
                        @Param("errorMsg") String errorMsg);

    /**
     * 更新任务为重试状态（失败后重新入队）
     * 
     * @param id 任务ID
     * @param version 当前版本号
     * @param errorMsg 错误信息
     * @param retryCount 重试次数
     * @return 影响行数
     */
    int updateTaskRetry(@Param("id") Long id,
                       @Param("version") Integer version,
                       @Param("errorMsg") String errorMsg,
                       @Param("retryCount") Integer retryCount);

    /**
     * 更新任务为超时状态
     * 
     * @param id 任务ID
     * @param version 当前版本号
     * @param errorMsg 超时错误信息
     * @return 影响行数
     */
    int updateTaskTimeout(@Param("id") Long id,
                         @Param("version") Integer version,
                         @Param("errorMsg") String errorMsg);

    /**
     * 插入任务执行日志
     * 
     * @param log 执行日志实体
     * @return 影响行数
     */
    int insertExecutionLog(TaskExecutionLog log);

    /**
     * 查询任务的执行历史
     * 
     * @param taskId 任务ID
     * @return 执行日志列表
     */
    List<TaskExecutionLog> selectExecutionLogs(@Param("taskId") Long taskId);

    /**
     * 清理过期的执行日志
     * 
     * @param beforeTime 清理截止时间
     * @return 删除行数
     */
    int deleteOldLogs(@Param("beforeTime") LocalDateTime beforeTime);

    /**
     * 查询超时的处理中任务
     * 用于定时任务检测并恢复超时任务
     * 
     * @param timeoutThreshold 超时阈值时间点
     * @return 超时任务列表
     */
    List<AsyncTask> selectTimeoutTasks(@Param("timeoutThreshold") LocalDateTime timeoutThreshold);

    /**
     * 统计各状态任务数量
     * 
     * @return 状态统计结果（Map格式：status -> count）
     */
    List<java.util.Map<String, Object>> countTasksByStatus();
}
