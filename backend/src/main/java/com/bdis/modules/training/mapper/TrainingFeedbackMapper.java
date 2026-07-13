package com.bdis.modules.training.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.modules.training.entity.TrainingFeedbackEntity;
import com.bdis.modules.training.query.TrainingFeedbackQuery;
import com.bdis.modules.training.vo.TrainingFeedbackDetailVO;
import com.bdis.modules.training.vo.TrainingFeedbackListVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TrainingFeedbackMapper extends BaseMapper<TrainingFeedbackEntity> {
    @Select("""
            SELECT * FROM edu_training_feedback
            WHERE training_record_id = #{recordId} AND user_id = #{userId}
            LIMIT 1
            """)
    TrainingFeedbackEntity selectByRecordAndUser(
            @Param("recordId") Long recordId, @Param("userId") Long userId);

    @Select("SELECT COUNT(1) FROM edu_training_feedback WHERE training_record_id = #{recordId}")
    Long countByRecordId(@Param("recordId") Long recordId);

    @Select("""
            <script>
            SELECT f.id, f.training_record_id, r.plan_id, p.plan_no, p.plan_name,
                   f.user_id, u.username, u.real_name, f.rating,
                   f.feedback_content, f.submitted_at, f.created_at, f.updated_at
            FROM edu_training_feedback f
            JOIN edu_training_record r ON r.id = f.training_record_id
            JOIN edu_training_plan p ON p.id = r.plan_id AND p.is_deleted = 0
            JOIN sys_user u ON u.id = f.user_id AND u.is_deleted = 0
            WHERE 1 = 1
            <if test="query.planId != null">AND r.plan_id = #{query.planId}</if>
            <if test="query.trainingRecordId != null">
              AND f.training_record_id = #{query.trainingRecordId}
            </if>
            <if test="query.userId != null">AND f.user_id = #{query.userId}</if>
            <if test="query.rating != null">AND f.rating = #{query.rating}</if>
            <if test="query.submittedFrom != null">
              AND f.submitted_at &gt;= #{query.submittedFrom}
            </if>
            <if test="query.submittedTo != null">
              AND f.submitted_at &lt;= #{query.submittedTo}
            </if>
            <if test="query.scopeAll != true">
              AND (f.user_id = #{query.scopeUserId}
                   OR p.owner_id = #{query.scopeUserId}
                   OR p.trainer_id = #{query.scopeUserId})
            </if>
            ORDER BY
            <choose>
              <when test="query.sortField == 'rating'">f.rating</when>
              <when test="query.sortField == 'createdAt'">f.created_at</when>
              <when test="query.sortField == 'updatedAt'">f.updated_at</when>
              <otherwise>f.submitted_at</otherwise>
            </choose>
            <choose>
              <when test="query.sortOrder == 'asc'">ASC</when>
              <otherwise>DESC</otherwise>
            </choose>, f.id DESC
            </script>
            """)
    Page<TrainingFeedbackListVO> selectPageVO(
            Page<TrainingFeedbackListVO> page, @Param("query") TrainingFeedbackQuery query);

    @Select("""
            SELECT f.id, f.training_record_id, r.plan_id, p.plan_no, p.plan_name,
                   f.user_id, u.username, u.real_name, f.rating,
                   f.feedback_content, f.submitted_at, f.created_at, f.updated_at,
                   r.attendance_status, r.training_status, r.progress, r.score, f.remark
            FROM edu_training_feedback f
            JOIN edu_training_record r ON r.id = f.training_record_id
            JOIN edu_training_plan p ON p.id = r.plan_id AND p.is_deleted = 0
            JOIN sys_user u ON u.id = f.user_id AND u.is_deleted = 0
            WHERE f.id = #{id}
            LIMIT 1
            """)
    TrainingFeedbackDetailVO selectDetailById(@Param("id") Long id);

    @Select("""
            SELECT f.id, f.training_record_id, r.plan_id, p.plan_no, p.plan_name,
                   f.user_id, u.username, u.real_name, f.rating,
                   f.feedback_content, f.submitted_at, f.created_at, f.updated_at,
                   r.attendance_status, r.training_status, r.progress, r.score, f.remark
            FROM edu_training_feedback f
            JOIN edu_training_record r ON r.id = f.training_record_id
            JOIN edu_training_plan p ON p.id = r.plan_id AND p.is_deleted = 0
            JOIN sys_user u ON u.id = f.user_id AND u.is_deleted = 0
            WHERE f.training_record_id = #{recordId}
            LIMIT 1
            """)
    TrainingFeedbackDetailVO selectDetailByRecordId(@Param("recordId") Long recordId);
}
