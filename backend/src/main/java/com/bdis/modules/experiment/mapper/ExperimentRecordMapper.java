package com.bdis.modules.experiment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.experiment.entity.ExperimentRecordEntity;
import com.bdis.modules.experiment.query.ExperimentRecordQuery;
import com.bdis.modules.experiment.vo.ExperimentRecordDetailVO;
import com.bdis.modules.experiment.vo.ExperimentRecordListVO;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ExperimentRecordMapper extends BaseMapper<ExperimentRecordEntity> {

    @Select(
            """
            SELECT COUNT(1) > 0
            FROM edu_experiment_record
            WHERE id = #{id}
              AND is_deleted = 0
            """)
    boolean existsActiveReferenceById(@Param("id") Long id);

    @Select("SELECT * FROM edu_experiment_record WHERE record_no = #{recordNo} LIMIT 1")
    ExperimentRecordEntity selectByRecordNoIncludingDeleted(@Param("recordNo") String recordNo);

    @Select("SELECT * FROM edu_course WHERE id = #{id} LIMIT 1")
    CourseEntity selectCourseByIdIncludingDeleted(@Param("id") Long id);

    @Select("SELECT * FROM research_project WHERE id = #{id} LIMIT 1")
    ResearchProjectEntity selectProjectByIdIncludingDeleted(@Param("id") Long id);

    @Select(
            """
            <script>
            SELECT er.id,
                   er.record_no,
                   CASE WHEN er.course_id IS NOT NULL THEN 'course' ELSE 'project' END AS source_type,
                   er.course_id,
                   c.course_no,
                   c.course_name,
                   er.project_id,
                   p.project_no,
                   p.project_name,
                   er.experiment_title,
                   er.recorder_id,
                   recorder.real_name AS recorder_name,
                   er.recorded_at,
                   er.archive_status,
                   er.status,
                   er.created_at,
                   er.updated_at
            FROM edu_experiment_record er
            LEFT JOIN edu_course c ON c.id = er.course_id
            LEFT JOIN research_project p ON p.id = er.project_id
            LEFT JOIN sys_user recorder ON recorder.id = er.recorder_id
            WHERE er.is_deleted = 0
            <if test="query.keyword != null and query.keyword.trim() != ''">
              AND (er.record_no LIKE CONCAT('%', #{query.keyword}, '%')
                   OR er.experiment_title LIKE CONCAT('%', #{query.keyword}, '%'))
            </if>
            <if test="query.courseId != null">AND er.course_id = #{query.courseId}</if>
            <if test="query.projectId != null">AND er.project_id = #{query.projectId}</if>
            <if test="query.recorderId != null">AND er.recorder_id = #{query.recorderId}</if>
            <if test="query.archiveStatus != null and query.archiveStatus.trim() != ''">
              AND er.archive_status = #{query.archiveStatus}
            </if>
            <if test="query.recordedFrom != null">AND er.recorded_at &gt;= #{query.recordedFrom}</if>
            <if test="query.recordedTo != null">AND er.recorded_at &lt;= #{query.recordedTo}</if>
            <if test="query.scopeAll != true">
              AND (er.recorder_id = #{query.scopeUserId}
                   OR EXISTS (SELECT 1 FROM edu_course scoped_course
                              WHERE scoped_course.id = er.course_id
                                AND scoped_course.teacher_id = #{query.scopeUserId}
                                AND scoped_course.is_deleted = 0)
                   OR EXISTS (SELECT 1 FROM research_project scoped_project
                              WHERE scoped_project.id = er.project_id
                                AND scoped_project.leader_id = #{query.scopeUserId}
                                AND scoped_project.is_deleted = 0))
            </if>
            ORDER BY er.recorded_at DESC, er.id DESC
            </script>
            """)
    Page<ExperimentRecordListVO> selectPageVO(
            Page<ExperimentRecordListVO> page, @Param("query") ExperimentRecordQuery query);

    @Select(
            """
            SELECT er.id,
                   er.record_no,
                   CASE WHEN er.course_id IS NOT NULL THEN 'course' ELSE 'project' END AS source_type,
                   er.course_id,
                   c.course_no,
                   c.course_name,
                   er.project_id,
                   p.project_no,
                   p.project_name,
                   er.experiment_title,
                   er.experiment_process,
                   er.experiment_result,
                   er.recorder_id,
                   recorder.username AS recorder_username,
                   recorder.real_name AS recorder_name,
                   er.recorded_at,
                   er.archive_status,
                   er.submitted_at,
                   er.submitted_by,
                   submitter.real_name AS submitted_by_name,
                   er.archived_at,
                   er.archived_by,
                   archiver.real_name AS archived_by_name,
                   er.archive_comment,
                   er.status,
                   er.remark,
                   er.version,
                   er.created_at,
                   er.updated_at,
                   er.created_by,
                   er.updated_by
            FROM edu_experiment_record er
            LEFT JOIN edu_course c ON c.id = er.course_id
            LEFT JOIN research_project p ON p.id = er.project_id
            LEFT JOIN sys_user recorder ON recorder.id = er.recorder_id
            LEFT JOIN sys_user submitter ON submitter.id = er.submitted_by
            LEFT JOIN sys_user archiver ON archiver.id = er.archived_by
            WHERE er.id = #{id} AND er.is_deleted = 0
            LIMIT 1
            """)
    ExperimentRecordDetailVO selectDetailById(@Param("id") Long id);

    @Update(
            """
            UPDATE edu_experiment_record
            SET archive_status = 'submitted',
                submitted_at = #{submittedAt},
                submitted_by = #{submittedBy},
                archived_at = NULL,
                archived_by = NULL,
                archive_comment = NULL,
                updated_at = #{submittedAt},
                updated_by = #{submittedBy},
                version = version + 1
            WHERE id = #{id}
              AND is_deleted = 0
              AND archive_status = 'draft'
              AND version = #{version}
            """)
    int submitByIdAndVersion(
            @Param("id") Long id,
            @Param("version") Integer version,
            @Param("submittedBy") Long submittedBy,
            @Param("submittedAt") LocalDateTime submittedAt);

    @Update(
            """
            UPDATE edu_experiment_record
            SET archive_status = 'archived',
                archived_at = #{archivedAt},
                archived_by = #{archivedBy},
                archive_comment = #{archiveComment},
                updated_at = #{archivedAt},
                updated_by = #{archivedBy},
                version = version + 1
            WHERE id = #{id}
              AND is_deleted = 0
              AND archive_status = 'submitted'
              AND version = #{version}
            """)
    int archiveByIdAndVersion(
            @Param("id") Long id,
            @Param("version") Integer version,
            @Param("archivedBy") Long archivedBy,
            @Param("archivedAt") LocalDateTime archivedAt,
            @Param("archiveComment") String archiveComment);

    @Update(
            """
            UPDATE edu_experiment_record
            SET is_deleted = 1,
                deleted_at = #{deletedAt},
                deleted_by = #{deletedBy},
                updated_at = #{deletedAt},
                updated_by = #{deletedBy},
                version = version + 1
            WHERE id = #{id}
              AND is_deleted = 0
              AND version = #{version}
            """)
    int logicalDeleteByIdAndVersion(
            @Param("id") Long id,
            @Param("version") Integer version,
            @Param("deletedBy") Long deletedBy,
            @Param("deletedAt") LocalDateTime deletedAt);
}
