package com.bdis.modules.assistant.mapper;

import com.bdis.modules.assistant.vo.HerbAssistantImageExplainContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantMatchContextVO;
import com.bdis.modules.assistant.vo.HerbAssistantRecognitionContextVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HerbAssistantImageContextMapper {

    HerbAssistantImageExplainContextVO selectImageContextById(
            @Param("imageId") Long imageId,
            @Param("currentUserId") Long currentUserId,
            @Param("dataScopeAll") boolean dataScopeAll);

    List<HerbAssistantMatchContextVO> selectTopMatchesByImageId(
            @Param("imageId") Long imageId,
            @Param("currentUserId") Long currentUserId,
            @Param("dataScopeAll") boolean dataScopeAll);

    HerbAssistantRecognitionContextVO selectLatestRecognitionByImageId(
            @Param("imageId") Long imageId,
            @Param("currentUserId") Long currentUserId,
            @Param("dataScopeAll") boolean dataScopeAll);
}
