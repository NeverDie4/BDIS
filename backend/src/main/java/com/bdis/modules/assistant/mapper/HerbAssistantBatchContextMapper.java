package com.bdis.modules.assistant.mapper;

import com.bdis.modules.assistant.vo.HerbAssistantBatchContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantImageContextVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HerbAssistantBatchContextMapper {

    HerbAssistantBatchContextVO selectBatchContextById(@Param("batchId") Long batchId);

    List<HerbAssistantImageContextVO> selectImageContextsByBatchId(@Param("batchId") Long batchId);
}
