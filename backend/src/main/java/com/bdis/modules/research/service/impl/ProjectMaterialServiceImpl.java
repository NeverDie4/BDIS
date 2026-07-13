package com.bdis.modules.research.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.file.dto.FileBusinessBindDTO;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.research.constant.ResearchProjectStatus;
import com.bdis.modules.research.entity.ResearchProjectEntity;
import com.bdis.modules.research.mapper.ResearchProjectMapper;
import com.bdis.modules.research.request.ProjectMaterialBindRequest;
import com.bdis.modules.research.service.ProjectMaterialService;
import com.bdis.modules.research.vo.ProjectMaterialVO;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class ProjectMaterialServiceImpl implements ProjectMaterialService {
    private static final String BIZ_TYPE = "research_project";
    private static final String AUDIT_MODULE = "M13_PROJECT_MATERIAL";
    private static final Set<String> FILE_USAGES = Set.of("attachment", "document", "dataset", "report", "image", "video", "other");

    private final ResearchProjectMapper projectMapper;
    private final FileBusinessMapper businessMapper;
    private final FileResourceMapper resourceMapper;
    private final FileBusinessService fileBusinessService;
    private final AuditLogService auditLogService;

    public ProjectMaterialServiceImpl(ResearchProjectMapper projectMapper, FileBusinessMapper businessMapper,
            FileResourceMapper resourceMapper, FileBusinessService fileBusinessService, AuditLogService auditLogService) {
        this.projectMapper = projectMapper;
        this.businessMapper = businessMapper;
        this.resourceMapper = resourceMapper;
        this.fileBusinessService = fileBusinessService;
        this.auditLogService = auditLogService;
    }

    @Override
    public List<ProjectMaterialVO> list(Long projectId, String fileUsage) {
        requireProject(projectId);
        LambdaQueryWrapper<FileBusinessEntity> wrapper = new LambdaQueryWrapper<FileBusinessEntity>()
                .eq(FileBusinessEntity::getBizType, BIZ_TYPE)
                .eq(FileBusinessEntity::getBizId, projectId)
                .orderByAsc(FileBusinessEntity::getCreatedAt)
                .orderByAsc(FileBusinessEntity::getId);
        if (StringUtils.hasText(fileUsage)) wrapper.eq(FileBusinessEntity::getFileUsage, fileUsage);
        List<FileBusinessEntity> relations = businessMapper.selectList(wrapper);
        List<Long> fileIds = relations.stream().map(FileBusinessEntity::getFileId).filter(Objects::nonNull).distinct().toList();
        if (fileIds.isEmpty()) return List.of();
        Map<Long, FileResourceEntity> files = new LinkedHashMap<>();
        for (FileResourceEntity file : resourceMapper.selectBatchIds(fileIds)) {
            if (Objects.equals(file.getStatus(), 1) && Objects.equals(file.getIsDeleted(), 0)) files.put(file.getId(), file);
        }
        return relations.stream().filter(relation -> files.containsKey(relation.getFileId()))
                .map(relation -> toVO(relation, files.get(relation.getFileId()))).toList();
    }

    @Override
    @Transactional
    public Long bind(Long projectId, ProjectMaterialBindRequest request) {
        ResearchProjectEntity project = requireProject(projectId);
        ResearchProjectStatus.assertMutable(project);
        validateRequest(request);
        FileResourceEntity file = requireActiveFile(request.getFileId());
        FileBusinessEntity existing = businessMapper.selectOne(new LambdaQueryWrapper<FileBusinessEntity>()
                .eq(FileBusinessEntity::getFileId, file.getId()).eq(FileBusinessEntity::getBizType, BIZ_TYPE).eq(FileBusinessEntity::getBizId, projectId));
        if (existing != null) throw new BusinessException(ResultCodeEnum.CONFLICT, "File is already bound to project");
        FileBusinessBindDTO dto = new FileBusinessBindDTO();
        dto.setFileId(file.getId()); dto.setBizType(BIZ_TYPE); dto.setBizId(projectId); dto.setFileUsage(request.getFileUsage()); dto.setRemark(request.getRemark());
        FileBusinessVO relation = fileBusinessService.bind(dto);
        if (relation == null || relation.getId() == null) throw new BusinessException(ResultCodeEnum.CONFLICT, "Project material bind failed");
        recordAudit("BIND", projectId);
        return relation.getId();
    }

    @Override
    @Transactional
    public void unbind(Long projectId, Long fileId) {
        ResearchProjectEntity project = requireProject(projectId);
        ResearchProjectStatus.assertMutable(project);
        FileBusinessEntity relation = businessMapper.selectOne(new LambdaQueryWrapper<FileBusinessEntity>()
                .eq(FileBusinessEntity::getFileId, fileId).eq(FileBusinessEntity::getBizType, BIZ_TYPE).eq(FileBusinessEntity::getBizId, projectId));
        if (relation == null) throw new ResourceNotFoundException("Project material relation not found");
        fileBusinessService.unbind(relation.getId());
        recordAudit("UNBIND", projectId);
    }

    private ResearchProjectEntity requireProject(Long projectId) {
        ResearchProjectEntity project = projectMapper.selectById(projectId);
        if (project == null) throw new ResourceNotFoundException("Research project not found");
        return project;
    }

    private FileResourceEntity requireActiveFile(Long fileId) {
        FileResourceEntity file = resourceMapper.selectById(fileId);
        if (file == null || !Objects.equals(file.getStatus(), 1) || !Objects.equals(file.getIsDeleted(), 0)) {
            throw new ResourceNotFoundException("File not found or inactive");
        }
        return file;
    }

    private void validateRequest(ProjectMaterialBindRequest request) {
        if (request == null || request.getFileId() == null || !StringUtils.hasText(request.getFileUsage())) {
            throw new BusinessException("File and file usage are required");
        }
        if (!FILE_USAGES.contains(request.getFileUsage())) throw new BusinessException("Invalid file usage");
    }

    private ProjectMaterialVO toVO(FileBusinessEntity relation, FileResourceEntity file) {
        ProjectMaterialVO vo = new ProjectMaterialVO();
        vo.setBindingId(relation.getId()); vo.setProjectId(relation.getBizId()); vo.setFileId(file.getId()); vo.setFileUsage(relation.getFileUsage());
        vo.setRemark(relation.getRemark()); vo.setCreatedAt(relation.getCreatedAt()); vo.setFileNo(file.getFileNo()); vo.setFileName(file.getFileName());
        vo.setOriginalFilename(file.getOriginalFilename()); vo.setFileType(file.getFileType()); vo.setFileFormat(file.getFileFormat()); vo.setFileSize(file.getFileSize());
        vo.setFileUrl(file.getFileUrl()); vo.setThumbnailUrl(file.getThumbnailUrl()); vo.setStorageType(file.getStorageType());
        vo.setUploaderId(file.getUploaderId()); vo.setUploaderName(file.getUploaderName()); vo.setUploadedAt(file.getUploadedAt());
        return vo;
    }

    private void recordAudit(String operation, Long projectId) {
        AuditRecordDTO dto = new AuditRecordDTO();
        dto.setOperationModule(AUDIT_MODULE); dto.setOperationType(operation); dto.setBizType(BIZ_TYPE); dto.setBizId(projectId);
        auditLogService.record(dto);
    }
}
