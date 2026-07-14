package com.bdis.modules.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bdis.modules.file.entity.FileResourceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface FileResourceMapper extends BaseMapper<FileResourceEntity> {

    @Select(
            """
            SELECT *
            FROM sys_file_resource
            WHERE id = #{fileId}
              AND is_deleted = 0
            FOR UPDATE
            """)
    FileResourceEntity selectByIdForUpdate(@Param("fileId") Long fileId);
}
