package com.bdis.modules.collection.mapper;

import com.bdis.modules.collection.dto.HerbCollectionTaskMyQueryRequest;
import com.bdis.modules.collection.dto.HerbCollectionTaskQueryRequest;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.vo.HerbCollectionTaskListVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbCollectionTaskMapper {

    int insert(HerbCollectionTaskEntity task);

    int updateById(HerbCollectionTaskEntity task);

    HerbCollectionTaskEntity selectById(@Param("id") Long id);

    HerbCollectionTaskVO selectDetailById(@Param("id") Long id);

    HerbCollectionTaskEntity selectByTaskCode(@Param("taskCode") String taskCode);

    Long countPage(@Param("query") HerbCollectionTaskQueryRequest query);

    List<HerbCollectionTaskVO> selectPage(
            @Param("query") HerbCollectionTaskQueryRequest query,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);

    List<HerbCollectionTaskListVO> selectList(
            @Param("query") HerbCollectionTaskQueryRequest query);

    Long countMyTasks(@Param("query") HerbCollectionTaskMyQueryRequest query);

    List<HerbCollectionTaskVO> selectMyTasks(
            @Param("query") HerbCollectionTaskMyQueryRequest query,
            @Param("offset") Long offset,
            @Param("pageSize") Integer pageSize);

    int logicDeleteById(@Param("id") Long id);

    int updateStatusById(@Param("id") Long id, @Param("taskStatus") String taskStatus);

    Long countBatchByTaskId(@Param("taskId") Long taskId);
}
