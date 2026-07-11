package com.bdis.modules.assistant.knowledge.mapper;

import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeChunkQueryRequest;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeChunk;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeChunkVO;
import com.bdis.modules.assistant.vo.HerbAssistantRagReferenceVO;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HerbAiKnowledgeChunkMapper {

    int insert(HerbAiKnowledgeChunk chunk);

    List<HerbAiKnowledgeChunk> selectEntitiesByDocId(@Param("docId") Long docId);

    List<HerbAiKnowledgeChunkVO> selectByDocId(
            @Param("docId") Long docId,
            @Param("query") HerbAiKnowledgeChunkQueryRequest query);

    List<HerbAssistantRagReferenceVO> selectLikeReferences(
            @Param("keyword") String keyword,
            @Param("docType") String docType,
            @Param("limit") int limit);

    int updateEmbeddingStatus(
            @Param("id") Long id,
            @Param("embeddingStatus") String embeddingStatus,
            @Param("vectorId") String vectorId,
            @Param("updateTime") LocalDateTime updateTime);

    int resetEmbeddingStatusByDocId(
            @Param("docId") Long docId,
            @Param("embeddingStatus") String embeddingStatus,
            @Param("updateTime") LocalDateTime updateTime);

    int logicDeleteByDocId(
            @Param("docId") Long docId, @Param("updateTime") LocalDateTime updateTime);
}
