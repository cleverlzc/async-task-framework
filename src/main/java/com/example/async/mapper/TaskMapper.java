package com.example.async.mapper;

import com.example.async.entity.AsyncTask;
import com.example.async.entity.TaskExecutionLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 异步任务Mapper接口
 * 
 * 功能说明：
 * 1. 任务CRUD操作
 * 2. 乐观锁任务认领
 * 3. 批量查询待处理任务
 * 4. 执行日志记录
 * 5. 任务统计查询
 * 
 * @author RelayAgent
 * @since 1.0.0
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
     * 根据业务键查询任务
     * 
     * @param businessKey 业务键
     * @return 任务实体
     */
    AsyncTask selectByBusinessKey(@Param("businessKey") String businessKey);

    /**
     * 批量查询待处理任务（按优先级排序）
     * 
     * 查询条件：
     * - 状态为PENDING（0）
     * - 重试次数未达上限
     * - 按优先级升序（优先级数值越小越优先）
     * - 按创建时间升序（相同优先级按创建时间排序）
     * 
     * @param limit 查询数量限制
     * @return 待处理任务列表
     */
    List<AsyncTask> selectPendingTasks(@Param("limit") int limit);

    /**
     * 乐观锁认领任务
     * 
     * 实现原理：
     * 使用UPDATE语句的WHERE条件同时检查status和version
     * 只有当任务状态为PENDING且version匹配时才更新成功
     * 
     * SQL示例：
     * UPDATE async_task 
     * SET status = 1, execute_node = ?, execute_start_time = ?, version = version + 1
     * WHERE id = ? AND status = 0 AND version = ?
     * 
     * @param taskId 任务ID
     * @param version 当前版本号
     * @param executeNode 执行节点标识
     * @param executeStartTime 执行开始时间
     * @return 影响行数（0表示认领失败，1表示认领成功）
     */
    int claimTask(@Param("taskId") Long taskId,
                  @Param("version") Integer version,
                  @Param("executeNode") String executeNode,
                  @Param("executeStartTime") LocalDateTime executeStartTime);

    /**
     * 批量乐观锁认领任务
     * 
     * 用于批量认领多个任务，提高效率
     * 
     * @param taskIds 任务ID列表
     * @param versions 版本号列表（与taskIds一一对应）
     * @param executeNode 执行节点标识
     * @param executeStartTime 执行开始时间
     * @return 影响行数
     */
    int batchClaimTasks(@Param("taskIds") List<Long> taskIds,
                        @Param("versions") List<Integer> versions,
                        @Param("executeNode") String executeNode,
                        @Param("executeStartTime") LocalDateTime executeStartTime);

    /**
     * 更新任务为成功状态
     * 
     * @param taskId 任务ID
     * @param version 当前版本号
     * @param result 执行结果（JSON格式）
     * @param executeEndTime 执行结束时间
     * @return 影响行数
     */
    int updateToSuccess(@Param("taskId") Long taskId,
                        @Param("version") Integer version,
                        @Param("result") String result,
                        @Param("executeEndTime") LocalDateTime executeEndTime);

    /**
     * 更新任务为失败状态
     * 
     * @param taskId 任务ID
     * @param version 当前版本号
     * @param errorMsg 错误信息
     * @param executeEndTime 执行结束时间
     * @return 影响行数
     */
    int updateToFailed(@Param("taskId") Long taskId,
                       @Param("version") Integer version,
                       @Param("errorMsg") String errorMsg,
                       @Param("executeEndTime") LocalDateTime executeEndTime);

    /**
     * 增加重试次数并重置为待处理状态
     * 
     * 用于任务失败后重新入队等待重试
     * 
     * @param taskId 任务ID
     * @param version 当前版本号
     * @return 影响行数
     */
    int incrementRetryAndReset(@Param("taskId") Long taskId,
                               @Param("version") Integer version);

    /**
     * 查询超时任务
     * 
     * 查询条件：
     * - 状态为PROCESSING（1）
     * - 执行开始时间 + 超时秒数 < 当前时间
     * 
     * @param limit 查询数量限制
     * @return 超时任务列表
     */
    List<AsyncTask> selectTimeoutTasks(@Param("limit") int limit);

    /**
     * 重置超时任务状态
     * 
     * 将超时的任务重置为PENDING状态，以便重新执行
     * 
     * @param taskId 任务ID
     * @param version 当前版本号
     * @return 影响行数
     */
    int resetTimeoutTask(@Param("taskId") Long taskId,
                         @Param("version") Integer version);

    /**
     * 插入执行日志
     * 
     * @param log 执行日志实体
     * @return 影响行数
     */
    int insertExecutionLog(TaskExecutionLog log);

    /**
     * 查询任务的执行日志
     * 
     * @param taskId 任务ID
     * @return 执行日志列表（按创建时间倒序）
     */
    List<TaskExecutionLog> selectExecutionLogs(@Param("taskId") Long taskId);

    /**
     * 统计各状态任务数量
     * 
     * @return 状态统计结果，key为状态码，value为数量
     */
    List<java.util.Map<String, Object>> countByStatus();

    /**
     * 查询任务统计信息
     * 
     * @param taskType 任务类型（可选）
     * @param statHour 统计小时（可选）
     * @return 统计信息列表
     */
    List<java.util.Map<String, Object>> selectStatistics(@Param("taskType") String taskType,
                                                          @Param("statHour") LocalDateTime statHour);

    /**
     * 插入或更新任务统计
     * 
     * 使用INSERT ... ON DUPLICATE KEY UPDATE语法
     * 
     * @param taskType 任务类型
     * @param statHour 统计小时
     * @param totalCount 总数
     * @param successCount 成功数
     * @param failedCount 失败数
     * @param avgDurationMs 平均耗时
     * @return 影响行数
     */
    int upsertStatistics(@Param("taskType") String taskType,
                         @Param("statHour") LocalDateTime statHour,
                         @Param("totalCount") int totalCount,
                         @Param("successCount") int successCount,
                         @Param("failedCount") int failedCount,
                         @Param("avgDurationMs") long avgDurationMs);

    /**
     * 删除指定时间之前的历史任务
     * 
     * 用于定期清理历史数据
     * 
     * @param beforeTime 删除此时间之前的数据
     * @param limit 删除数量限制（防止一次删除太多）
     * @return 删除的行数
     */
    int deleteHistoryTasks(@Param("beforeTime") LocalDateTime beforeTime,
                          @Param("limit") int limit);

    /**
     * 删除指定时间之前的执行日志
     * 
     * @param beforeTime 删除此时间之前的数据
     * @param limit 删除数量限制
     * @return 删除的行数
     */
    int deleteHistoryLogs(@Param("beforeTime") LocalDateTime beforeTime,
                         @Param("limit") int limit);
}
