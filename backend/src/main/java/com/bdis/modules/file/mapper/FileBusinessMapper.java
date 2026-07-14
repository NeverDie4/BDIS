package com.bdis.modules.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface FileBusinessMapper extends BaseMapper<FileBusinessEntity> {

    @Select(
            """
            SELECT COUNT(*)
            FROM sys_file_business relation
            JOIN sys_file_resource file ON file.id = relation.file_id
            WHERE relation.biz_type = #{bizType}
              AND relation.biz_id = #{bizId}
              AND file.is_deleted = 0
              AND file.status = 1
            """)
    long countActiveFiles(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    @Select(
            """
            SELECT file.*
            FROM sys_file_business relation
            JOIN sys_file_resource file ON file.id = relation.file_id
            WHERE relation.biz_type = #{bizType}
              AND relation.biz_id = #{bizId}
              AND file.is_deleted = 0
              AND file.status = 1
            ORDER BY relation.sort_order ASC, relation.id DESC
            LIMIT #{size} OFFSET #{offset}
            """)
    List<FileResourceEntity> selectActiveFiles(
            @Param("bizType") String bizType,
            @Param("bizId") Long bizId,
            @Param("offset") long offset,
            @Param("size") long size);
}
