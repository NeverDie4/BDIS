package com.bdis.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.modules.training.entity.TrainingRecordEntity;
import com.bdis.modules.training.query.TrainingRecordQuery;
import com.bdis.modules.training.vo.TrainingRecordDetailVO;
import com.bdis.modules.training.vo.TrainingRecordListVO;
import com.bdis.modules.training.vo.TrainingSummaryVO;
import com.bdis.modules.user.entity.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TrainingRecordMapper extends BaseMapper<TrainingRecordEntity> {
    @Select("""
            <script>
            SELECT * FROM sys_user WHERE id IN
            <foreach collection="userIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    List<UserEntity> selectUsersIncludingDeleted(@Param("userIds") List<Long> userIds);

    @Select("""
            <script>
            SELECT * FROM edu_training_record
            WHERE plan_id = #{planId} AND user_id IN
            <foreach collection="userIds" item="id" open="(" separator="," close=")">
              #{id}
            </foreach>
            </script>
            """)
    List<TrainingRecordEntity> selectByPlanAndUsers(
            @Param("planId") Long planId, @Param("userIds") List<Long> userIds);
    @Select("""
            SELECT * FROM edu_training_record
            WHERE plan_id = #{planId} AND user_id = #{userId}
            LIMIT 1
            """)
    TrainingRecordEntity selectByPlanAndUser(
            @Param("planId") Long planId, @Param("userId") Long userId);

    @Select("""
            <script>
            SELECT r.id, r.plan_id, p.plan_no, p.plan_name, r.user_id,
                   u.username, u.real_name, r.course_id, r.progress,
                   r.training_status, r.attendance_status, r.score,
                   r.started_at, r.checked_in_at, r.completed_at,
                   r.created_at, r.updated_at
            FROM edu_training_record r
            JOIN edu_training_plan p ON p.id = r.plan_id AND p.is_deleted = 0
            JOIN sys_user u ON u.id = r.user_id AND u.is_deleted = 0
            WHERE 1 = 1
            <if test="query.keyword != null and query.keyword != ''">
              AND (p.plan_no LIKE CONCAT('%', #{query.keyword}, '%')
                   OR p.plan_name LIKE CONCAT('%', #{query.keyword}, '%')
                   OR u.username LIKE CONCAT('%', #{query.keyword}, '%')
                   OR u.real_name LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            <if test="query.planId != null">AND r.plan_id = #{query.planId}</if>
            <if test="query.userId != null">AND r.user_id = #{query.userId}</if>
            <if test="query.trainingStatus != null and query.trainingStatus != ''">
              AND r.training_status = #{query.trainingStatus}
            </if>
            <if test="query.attendanceStatus != null and query.attendanceStatus != ''">
              AND r.attendance_status = #{query.attendanceStatus}
            </if>
            <if test="query.recordedFrom != null">AND r.created_at &gt;= #{query.recordedFrom}</if>
            <if test="query.recordedTo != null">AND r.created_at &lt;= #{query.recordedTo}</if>
            ORDER BY r.updated_at DESC, r.id DESC
            </script>
            """)
    Page<TrainingRecordListVO> selectPageVO(
            Page<TrainingRecordListVO> page, @Param("query") TrainingRecordQuery query);

    @Select("""
            SELECT r.id, r.plan_id, p.plan_no, p.plan_name, r.user_id,
                   u.username, u.real_name, r.course_id, r.progress,
                   r.training_status, r.attendance_status, r.score,
                   r.started_at, r.checked_in_at, r.completed_at,
                   r.created_at, r.updated_at, r.result_comment, r.remark
            FROM edu_training_record r
            JOIN edu_training_plan p ON p.id = r.plan_id AND p.is_deleted = 0
            JOIN sys_user u ON u.id = r.user_id AND u.is_deleted = 0
            WHERE r.id = #{id}
            LIMIT 1
            """)
    TrainingRecordDetailVO selectDetailById(@Param("id") Long id);

    @Delete("""
            DELETE FROM edu_training_record
            WHERE id = #{id}
              AND COALESCE(attendance_status, 'pending') = 'pending'
              AND COALESCE(progress, 0) = 0
              AND COALESCE(training_status, 'not_started') = 'not_started'
              AND score IS NULL
              AND checked_in_at IS NULL
              AND completed_at IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM edu_training_feedback f
                  WHERE f.training_record_id = edu_training_record.id
              )
            """)
    int deletePristine(@Param("id") Long id);

    @Select("""
            SELECT p.id AS plan_id, p.plan_no, p.plan_name, p.publish_status,
                   COUNT(r.id) AS total_participant_count,
                   COALESCE(SUM(r.training_status = 'completed'), 0) AS completed_count,
                   COALESCE(SUM(r.training_status = 'failed'), 0) AS failed_count,
                   COALESCE(SUM(r.training_status = 'makeup'), 0) AS makeup_count,
                   COALESCE(SUM(r.training_status = 'not_started'), 0) AS not_started_count,
                   COALESCE(SUM(r.training_status = 'learning'), 0) AS learning_count,
                   COALESCE(SUM(COALESCE(r.attendance_status, 'pending') = 'pending'), 0)
                     AS pending_attendance_count,
                   COALESCE(SUM(r.attendance_status = 'present'), 0) AS present_count,
                   COALESCE(SUM(r.attendance_status = 'late'), 0) AS late_count,
                   COALESCE(SUM(r.attendance_status = 'absent'), 0) AS absent_count,
                   COALESCE(SUM(r.attendance_status = 'leave'), 0) AS leave_count,
                   COALESCE(AVG(r.progress), 0.00) AS average_progress,
                   COALESCE(SUM(r.score IS NOT NULL), 0) AS scored_participant_count,
                   AVG(r.score) AS average_score,
                   (SELECT COUNT(f.id)
                      FROM edu_training_feedback f
                      JOIN edu_training_record fr ON fr.id = f.training_record_id
                     WHERE fr.plan_id = p.id) AS feedback_count,
                   (SELECT AVG(f.rating)
                      FROM edu_training_feedback f
                      JOIN edu_training_record fr ON fr.id = f.training_record_id
                     WHERE fr.plan_id = p.id) AS average_rating
            FROM edu_training_plan p
            LEFT JOIN edu_training_record r ON r.plan_id = p.id
            WHERE p.id = #{planId} AND p.is_deleted = 0
            GROUP BY p.id, p.plan_no, p.plan_name, p.publish_status
            """)
    TrainingSummaryVO selectSummary(@Param("planId") Long planId);
}
