package com.bdis.modules.assistant.knowledge.service.impl;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.assistant.knowledge.constant.HerbAiEmbeddingStatusConstants;
import com.bdis.modules.assistant.knowledge.constant.HerbAiKnowledgeDocTypeConstants;
import com.bdis.modules.assistant.knowledge.constant.HerbAiKnowledgeStatusConstants;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeChunkQueryRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocCreateRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocQueryRequest;
import com.bdis.modules.assistant.knowledge.dto.HerbAiKnowledgeDocUpdateRequest;
import com.bdis.modules.assistant.knowledge.entity.HerbAiKnowledgeDoc;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeChunkMapper;
import com.bdis.modules.assistant.knowledge.mapper.HerbAiKnowledgeDocMapper;
import com.bdis.modules.assistant.knowledge.service.HerbAiKnowledgeDocService;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeChunkVO;
import com.bdis.modules.assistant.knowledge.vo.HerbAiKnowledgeDocVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class HerbAiKnowledgeDocServiceImpl implements HerbAiKnowledgeDocService {

    private static final int MAX_PAGE_SIZE = 200;
    private static final DateTimeFormatter DOC_CODE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final Set<String> VALID_DOC_TYPES =
            Set.of(
                    HerbAiKnowledgeDocTypeConstants.SYSTEM_GUIDE,
                    HerbAiKnowledgeDocTypeConstants.HERB_KNOWLEDGE,
                    HerbAiKnowledgeDocTypeConstants.FAQ,
                    HerbAiKnowledgeDocTypeConstants.RULE,
                    HerbAiKnowledgeDocTypeConstants.REPORT,
                    HerbAiKnowledgeDocTypeConstants.OTHER);
    private static final Set<String> VALID_STATUSES =
            Set.of(
                    HerbAiKnowledgeStatusConstants.ENABLED,
                    HerbAiKnowledgeStatusConstants.DISABLED);
    private static final Set<String> VALID_EMBEDDING_STATUSES =
            Set.of(
                    HerbAiEmbeddingStatusConstants.PENDING,
                    HerbAiEmbeddingStatusConstants.PROCESSING,
                    HerbAiEmbeddingStatusConstants.COMPLETED,
                    HerbAiEmbeddingStatusConstants.FAILED);
    private static final Set<String> VALID_SOURCE_TYPES =
            Set.of("manual", "file", "markdown", "api");

    private final HerbAiKnowledgeDocMapper docMapper;
    private final HerbAiKnowledgeChunkMapper chunkMapper;

    public HerbAiKnowledgeDocServiceImpl(
            HerbAiKnowledgeDocMapper docMapper, HerbAiKnowledgeChunkMapper chunkMapper) {
        this.docMapper = docMapper;
        this.chunkMapper = chunkMapper;
    }

    @Override
    @Transactional
    public HerbAiKnowledgeDocVO create(HerbAiKnowledgeDocCreateRequest request) {
        validateCreateRequest(request);
        LocalDateTime now = LocalDateTime.now();
        HerbAiKnowledgeDoc doc = new HerbAiKnowledgeDoc();
        doc.setDocCode(generateDocCode(now));
        doc.setDocTitle(request.getDocTitle().trim());
        doc.setDocType(normalizeDocType(request.getDocType()));
        doc.setSourceType(normalizeSourceType(request.getSourceType()));
        doc.setFileName(trimToNull(request.getFileName()));
        doc.setFileUrl(trimToNull(request.getFileUrl()));
        doc.setContentText(request.getContentText().trim());
        doc.setSummary(trimToNull(request.getSummary()));
        doc.setStatus(normalizeStatus(request.getStatus()));
        doc.setEmbeddingStatus(HerbAiEmbeddingStatusConstants.PENDING);
        doc.setChunkCount(0);
        doc.setRemark(trimToNull(request.getRemark()));
        doc.setCreateTime(now);
        doc.setUpdateTime(now);
        doc.setDeleted(0);
        docMapper.insert(doc);
        return doc.getId() == null ? toVO(doc) : getById(doc.getId());
    }

    @Override
    @Transactional
    public HerbAiKnowledgeDocVO update(Long id, HerbAiKnowledgeDocUpdateRequest request) {
        HerbAiKnowledgeDoc existing = getActiveDoc(id);
        if (request == null) {
            throw new BusinessException("请求参数不能为空");
        }
        if (request.getDocTitle() != null) {
            if (!StringUtils.hasText(request.getDocTitle())) {
                throw new BusinessException("文档标题不能为空");
            }
            existing.setDocTitle(request.getDocTitle().trim());
        }
        if (request.getDocType() != null) {
            existing.setDocType(validateDocType(request.getDocType()));
        }
        if (request.getContentText() != null) {
            if (!StringUtils.hasText(request.getContentText())) {
                throw new BusinessException("文档内容不能为空");
            }
            String normalizedContent = request.getContentText().trim();
            if (!normalizedContent.equals(existing.getContentText())) {
                existing.setContentText(normalizedContent);
                existing.setEmbeddingStatus(HerbAiEmbeddingStatusConstants.PENDING);
            }
        }
        if (request.getSummary() != null) {
            existing.setSummary(trimToNull(request.getSummary()));
        }
        if (request.getStatus() != null) {
            existing.setStatus(validateStatus(request.getStatus()));
        }
        if (request.getRemark() != null) {
            existing.setRemark(trimToNull(request.getRemark()));
        }
        existing.setUpdateTime(LocalDateTime.now());
        if (docMapper.updateById(existing) == 0) {
            throw new BusinessException("知识库文档不存在或已删除");
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        HerbAiKnowledgeDoc existing = getActiveDoc(id);
        LocalDateTime now = LocalDateTime.now();
        chunkMapper.logicDeleteByDocId(existing.getId(), now);
        if (docMapper.logicDeleteById(existing.getId(), now) == 0) {
            throw new BusinessException("知识库文档不存在或已删除");
        }
    }

    @Override
    public HerbAiKnowledgeDocVO getById(Long id) {
        validateId(id);
        HerbAiKnowledgeDocVO doc = docMapper.selectDetailById(id);
        if (doc == null) {
            throw new BusinessException("知识库文档不存在");
        }
        return doc;
    }

    @Override
    public PageResult<HerbAiKnowledgeDocVO> page(HerbAiKnowledgeDocQueryRequest request) {
        HerbAiKnowledgeDocQueryRequest query =
                request == null ? new HerbAiKnowledgeDocQueryRequest() : request;
        normalizePage(query);
        if (StringUtils.hasText(query.getDocType())) {
            query.setDocType(validateDocType(query.getDocType()));
        }
        if (StringUtils.hasText(query.getStatus())) {
            query.setStatus(validateStatus(query.getStatus()));
        }
        if (StringUtils.hasText(query.getEmbeddingStatus())) {
            query.setEmbeddingStatus(validateEmbeddingStatus(query.getEmbeddingStatus()));
        }
        query.setKeyword(trimToNull(query.getKeyword()));
        long total = docMapper.countPage(query);
        long offset = (long) (query.getPageNum() - 1) * query.getPageSize();
        List<HerbAiKnowledgeDocVO> records =
                docMapper.selectPage(query, offset, query.getPageSize());
        return new PageResult<>(records, query.getPageNum(), query.getPageSize(), total);
    }

    @Override
    @Transactional
    public HerbAiKnowledgeDocVO enable(Long id) {
        return changeStatus(id, HerbAiKnowledgeStatusConstants.ENABLED);
    }

    @Override
    @Transactional
    public HerbAiKnowledgeDocVO disable(Long id) {
        return changeStatus(id, HerbAiKnowledgeStatusConstants.DISABLED);
    }

    @Override
    public List<HerbAiKnowledgeChunkVO> listChunks(
            Long docId, HerbAiKnowledgeChunkQueryRequest request) {
        getActiveDoc(docId);
        HerbAiKnowledgeChunkQueryRequest query =
                request == null ? new HerbAiKnowledgeChunkQueryRequest() : request;
        if (StringUtils.hasText(query.getEmbeddingStatus())) {
            query.setEmbeddingStatus(validateEmbeddingStatus(query.getEmbeddingStatus()));
        }
        return chunkMapper.selectByDocId(docId, query);
    }

    private HerbAiKnowledgeDocVO changeStatus(Long id, String status) {
        HerbAiKnowledgeDoc existing = getActiveDoc(id);
        if (!status.equals(existing.getStatus())
                && docMapper.updateStatus(id, status, LocalDateTime.now()) == 0) {
            throw new BusinessException("知识库文档不存在或已删除");
        }
        return getById(id);
    }

    private HerbAiKnowledgeDoc getActiveDoc(Long id) {
        validateId(id);
        HerbAiKnowledgeDoc doc = docMapper.selectById(id);
        if (doc == null) {
            throw new BusinessException("知识库文档不存在");
        }
        return doc;
    }

    private void validateCreateRequest(HerbAiKnowledgeDocCreateRequest request) {
        if (request == null || !StringUtils.hasText(request.getDocTitle())) {
            throw new BusinessException("文档标题不能为空");
        }
        if (!StringUtils.hasText(request.getContentText())) {
            throw new BusinessException("文档内容不能为空");
        }
        normalizeDocType(request.getDocType());
        normalizeSourceType(request.getSourceType());
        normalizeStatus(request.getStatus());
    }

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException("知识库文档 ID 无效");
        }
    }

    private String normalizeDocType(String docType) {
        return StringUtils.hasText(docType)
                ? validateDocType(docType)
                : HerbAiKnowledgeDocTypeConstants.OTHER;
    }

    private String validateDocType(String docType) {
        String normalized = docType.trim();
        if (!VALID_DOC_TYPES.contains(normalized)) {
            throw new BusinessException("文档类型无效");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status)
                ? validateStatus(status)
                : HerbAiKnowledgeStatusConstants.ENABLED;
    }

    private String validateStatus(String status) {
        String normalized = status.trim();
        if (!VALID_STATUSES.contains(normalized)) {
            throw new BusinessException("文档状态无效");
        }
        return normalized;
    }

    private String validateEmbeddingStatus(String status) {
        String normalized = status.trim();
        if (!VALID_EMBEDDING_STATUSES.contains(normalized)) {
            throw new BusinessException("向量化状态无效");
        }
        return normalized;
    }

    private String normalizeSourceType(String sourceType) {
        String normalized = StringUtils.hasText(sourceType) ? sourceType.trim() : "manual";
        if (!VALID_SOURCE_TYPES.contains(normalized)) {
            throw new BusinessException("文档来源类型无效");
        }
        return normalized;
    }

    private void normalizePage(HerbAiKnowledgeDocQueryRequest query) {
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(10);
        } else if (query.getPageSize() > MAX_PAGE_SIZE) {
            query.setPageSize(MAX_PAGE_SIZE);
        }
    }

    private String generateDocCode(LocalDateTime now) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 6).toUpperCase();
        return "DOC_" + DOC_CODE_TIME_FORMAT.format(now) + "_" + suffix;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private HerbAiKnowledgeDocVO toVO(HerbAiKnowledgeDoc doc) {
        HerbAiKnowledgeDocVO vo = new HerbAiKnowledgeDocVO();
        vo.setId(doc.getId());
        vo.setDocCode(doc.getDocCode());
        vo.setDocTitle(doc.getDocTitle());
        vo.setDocType(doc.getDocType());
        vo.setSourceType(doc.getSourceType());
        vo.setFileName(doc.getFileName());
        vo.setFileUrl(doc.getFileUrl());
        vo.setContentText(doc.getContentText());
        vo.setSummary(doc.getSummary());
        vo.setStatus(doc.getStatus());
        vo.setEmbeddingStatus(doc.getEmbeddingStatus());
        vo.setChunkCount(doc.getChunkCount());
        vo.setRemark(doc.getRemark());
        vo.setCreateTime(doc.getCreateTime());
        vo.setUpdateTime(doc.getUpdateTime());
        return vo;
    }
}
