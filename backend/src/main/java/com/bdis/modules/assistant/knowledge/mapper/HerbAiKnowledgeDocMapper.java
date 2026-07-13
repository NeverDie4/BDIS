package com.bdis.modules.assistant.knowledge.mapper;

import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocQueryRequest;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeDoc;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeDocVO;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HerbAiKnowledgeDocMapper {

    int insert(HerbAiKnowledgeDoc doc);

    int updateById(HerbAiKnowledgeDoc doc);

    HerbAiKnowledgeDoc selectById(@Param("id") Long id);

    HerbAiKnowledgeDocVO selectDetailById(@Param("id") Long id);

    long countPage(@Param("query") HerbAiKnowledgeDocQueryRequest query);

    List<HerbAiKnowledgeDocVO> selectPage(
            @Param("query") HerbAiKnowledgeDocQueryRequest query,
            @Param("offset") long offset,
            @Param("pageSize") int pageSize);

    int updateStatus(
            @Param("id") Long id,
            @Param("status") String status,
            @Param("updateTime") java.time.LocalDateTime updateTime);

    int updateChunkCountAndEmbeddingStatus(
            @Param("id") Long id,
            @Param("chunkCount") Integer chunkCount,
            @Param("embeddingStatus") String embeddingStatus,
            @Param("updateTime") java.time.LocalDateTime updateTime);

    int updateEmbeddingStatus(
            @Param("id") Long id,
            @Param("embeddingStatus") String embeddingStatus,
            @Param("updateTime") java.time.LocalDateTime updateTime);

    List<HerbAiKnowledgeDoc> selectPendingEnabled();

    int logicDeleteById(
            @Param("id") Long id, @Param("updateTime") java.time.LocalDateTime updateTime);
}
