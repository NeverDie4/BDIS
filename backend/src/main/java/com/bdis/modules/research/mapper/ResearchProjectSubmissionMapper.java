package com.bdis.modules.research.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.research.entity.ResearchProjectSubmissionEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface ResearchProjectSubmissionMapper
        extends BaseMapper<ResearchProjectSubmissionEntity> {
    @Select("SELECT * FROM research_project_submission WHERE id=#{id} AND is_deleted=0 LIMIT 1")
    ResearchProjectSubmissionEntity selectActiveById(Long id);

    @Select(
            "SELECT * FROM research_project_submission WHERE project_id=#{projectId} AND is_deleted=0 ORDER BY submitted_at DESC, id DESC")
    List<ResearchProjectSubmissionEntity> selectByProjectId(@Param("projectId") Long projectId);

    @Update(
            """
            UPDATE research_project_submission
            SET submission_status = #{targetStatus},
                reviewed_at = #{reviewedAt},
                updated_at = #{reviewedAt},
                updated_by = #{reviewerId},
                version = version + 1
            WHERE id = #{id}
              AND is_deleted = 0
              AND version = #{version}
              AND ((#{targetStatus} IN ('returned', 'approved')
                    AND submission_status IN ('submitted', 'reviewing'))
                   OR (#{targetStatus} = 'archived' AND submission_status = 'approved'))
            """)
    int reviewByIdAndVersion(
            @Param("id") Long id,
            @Param("version") Integer version,
            @Param("targetStatus") String targetStatus,
            @Param("reviewerId") Long reviewerId,
            @Param("reviewedAt") java.time.LocalDateTime reviewedAt);
}
