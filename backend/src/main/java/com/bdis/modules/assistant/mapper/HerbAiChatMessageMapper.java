package com.bdis.modules.assistant.mapper;

import com.bdis.modules.assistant.entity.HerbAiChatMessage;
import com.bdis.modules.assistant.vo.HerbAiChatMessageVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbAiChatMessageMapper {

    int insert(HerbAiChatMessage message);

    List<HerbAiChatMessageVO> selectBySessionId(@Param("sessionId") String sessionId);

    int logicDeleteBySessionId(@Param("sessionId") String sessionId);
}
