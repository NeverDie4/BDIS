package com.bdis.modules.assistant.mapper;

import com.bdis.modules.assistant.dto.HerbAiChatHistoryQueryRequest;
import com.bdis.modules.assistant.entity.HerbAiChatSession;
import com.bdis.modules.assistant.vo.HerbAiChatSessionVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Param;

public interface HerbAiChatSessionMapper {

    int insert(HerbAiChatSession session);

    HerbAiChatSession selectBySessionId(
            @Param("sessionId") String sessionId, @Param("userId") Long userId);

    Long countPage(@Param("query") HerbAiChatHistoryQueryRequest query);

    List<HerbAiChatSessionVO> selectPage(
            @Param("query") HerbAiChatHistoryQueryRequest query,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize);

    int updateLastMessage(
            @Param("sessionId") String sessionId,
            @Param("userId") Long userId,
            @Param("lastMessage") String lastMessage,
            @Param("lastMessageTime") LocalDateTime lastMessageTime);

    int logicDeleteBySessionId(@Param("sessionId") String sessionId, @Param("userId") Long userId);
}
