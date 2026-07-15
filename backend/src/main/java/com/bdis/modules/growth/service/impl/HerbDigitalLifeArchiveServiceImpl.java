package com.bdis.modules.growth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.vo.FileContentVO;
import com.bdis.modules.collection.constant.HerbCollectionTaskStatusConstants;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.collection.vo.HerbCollectionTaskVO;
import com.bdis.modules.growth.entity.DigitalLifeNarrationEntity;
import com.bdis.modules.growth.mapper.DigitalLifeNarrationMapper;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.support.HerbDigitalLifeArchiveAssembler;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeImageVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeMetricsVO;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicImageVO;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicStageVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.vo.HerbImageVO;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class HerbDigitalLifeArchiveServiceImpl implements HerbDigitalLifeArchiveService {

    private static final String APPROVED = "approved";
    private static final String PUBLIC_ARCHIVE_PATH = "/trace/digital-life/";

    @Value("${bdis.trace.public-web-base-url:http://localhost:3000}")
    private String publicWebBaseUrl = "http://localhost:3000";

    private final HerbCollectionTaskMapper taskMapper;
    private final HerbBatchMapper batchMapper;
    private final GrowthRecordMapper growthRecordMapper;
    private final HerbImageMapper imageMapper;
    private final HerbBatchImageMapper batchImageMapper;
    private final CollectionAccessService collectionAccessService;
    private final HerbDigitalLifeArchiveAssembler archiveAssembler;
    private final FileResourceService fileResourceService;
    private final DigitalLifeNarrationMapper narrationMapper;

    public HerbDigitalLifeArchiveServiceImpl(
            HerbCollectionTaskMapper taskMapper,
            HerbBatchMapper batchMapper,
            GrowthRecordMapper growthRecordMapper,
            HerbImageMapper imageMapper,
            HerbBatchImageMapper batchImageMapper,
            CollectionAccessService collectionAccessService,
            HerbDigitalLifeArchiveAssembler archiveAssembler,
            FileResourceService fileResourceService,
            DigitalLifeNarrationMapper narrationMapper) {
        this.taskMapper = taskMapper;
        this.batchMapper = batchMapper;
        this.growthRecordMapper = growthRecordMapper;
        this.imageMapper = imageMapper;
        this.batchImageMapper = batchImageMapper;
        this.collectionAccessService = collectionAccessService;
        this.archiveAssembler = archiveAssembler;
        this.fileResourceService = fileResourceService;
        this.narrationMapper = narrationMapper;
    }

    @Override
    public HerbDigitalLifeArchiveVO getByTaskId(Long taskId) {
        HerbCollectionTaskEntity task = requireTask(taskId);
        collectionAccessService.requireTaskAccess(task);
        return loadArchive(task, false);
    }

    @Override
    public HerbDigitalLifePublicArchiveVO publicArchive(String traceCode) {
        HerbCollectionTaskEntity task = requirePublicTask(traceCode);
        return toPublicArchive(loadArchive(task, true));
    }

    @Override
    public FileContentVO publicQrCode(String traceCode) {
        HerbCollectionTaskEntity task = requirePublicTask(traceCode);
        String publicUrl = fullPublicUrl(PUBLIC_ARCHIVE_PATH + task.getTraceCode());
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var matrix =
                    new QRCodeWriter().encode(publicUrl, BarcodeFormat.QR_CODE, 320, 320);
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            byte[] content = output.toByteArray();
            FileContentVO file = new FileContentVO();
            file.setResource(new ByteArrayResource(content));
            file.setFileName("digital_life_" + task.getId() + ".png");
            file.setContentType(MediaType.IMAGE_PNG_VALUE);
            file.setFileSize((long) content.length);
            return file;
        } catch (Exception ex) {
            throw new BusinessException("生成数字生命档案二维码失败");
        }
    }

    @Override
    public FileContentVO publicImage(String traceCode, Long imageId) {
        HerbImageVO image = imageMapper.selectPublicByTraceCodeAndImageId(traceCode, imageId);
        if (image == null) {
            throw new ResourceNotFoundException("公开数字生命档案图片不存在");
        }
        Long fileId = fileResourceService.resolveFileId(image.getImageUrl());
        if (fileId == null) {
            throw new ResourceNotFoundException("公开数字生命档案图片文件不存在");
        }
        return fileResourceService.internalContent(fileId);
    }

    private HerbDigitalLifeArchiveVO loadArchive(
            HerbCollectionTaskEntity task, boolean publicOnly) {
        List<HerbBatchVO> batches = safeList(batchMapper.selectByTaskId(task.getId()));
        if (batches.isEmpty()) {
            return decorateArchive(
                    archiveAssembler.assemble(
                            toTaskVO(task), List.of(), Map.of(), Map.of(), Map.of()),
                    task);
        }
        List<Long> batchIds =
                batches.stream().map(HerbBatchVO::getId).filter(Objects::nonNull).toList();
        Map<Long, GrowthRecordVO> growthRecords = loadGrowthRecords(batchIds);
        List<HerbBatchVO> visibleBatches =
                publicOnly
                        ? batches.stream()
                                .filter(batch -> isPublicApproved(growthRecords.get(batch.getId())))
                                .toList()
                        : batches;
        List<Long> visibleBatchIds =
                visibleBatches.stream().map(HerbBatchVO::getId).filter(Objects::nonNull).toList();
        Map<Long, List<HerbImageVO>> imagesByBatch =
                visibleBatchIds.isEmpty()
                        ? Map.of()
                        : groupImages(imageMapper.selectByBatchIds(visibleBatchIds));
        Map<Long, List<HerbBatchImageVO>> batchImagesByBatch =
                visibleBatchIds.isEmpty()
                        ? Map.of()
                        : groupBatchImages(
                                batchImageMapper.selectBatchImagesWithIdentificationByBatchIds(
                                        visibleBatchIds));
        HerbDigitalLifeArchiveVO archive =
                archiveAssembler.assemble(
                        toTaskVO(task),
                        visibleBatches,
                        growthRecords,
                        imagesByBatch,
                        batchImagesByBatch);
        sanitizeManagementImageUrls(archive);
        attachNarrations(archive);
        return decorateArchive(archive, task);
    }

    private Map<Long, GrowthRecordVO> loadGrowthRecords(List<Long> batchIds) {
        if (batchIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, GrowthRecordVO> records = new LinkedHashMap<>();
        for (GrowthRecordVO record :
                safeList(growthRecordMapper.selectJoinedByBatchIds(batchIds))) {
            if (record != null && record.getBatchId() != null) {
                records.put(record.getBatchId(), record);
            }
        }
        return records;
    }

    private Map<Long, List<HerbImageVO>> groupImages(List<HerbImageVO> images) {
        return safeList(images).stream()
                .filter(Objects::nonNull)
                .filter(image -> image.getBatchId() != null)
                .collect(
                        Collectors.groupingBy(
                                HerbImageVO::getBatchId, LinkedHashMap::new, Collectors.toList()));
    }

    private Map<Long, List<HerbBatchImageVO>> groupBatchImages(List<HerbBatchImageVO> batchImages) {
        return safeList(batchImages).stream()
                .filter(Objects::nonNull)
                .filter(image -> image.getBatchId() != null)
                .collect(
                        Collectors.groupingBy(
                                HerbBatchImageVO::getBatchId,
                                LinkedHashMap::new,
                                Collectors.toList()));
    }

    private HerbCollectionTaskEntity requireTask(Long taskId) {
        HerbCollectionTaskEntity task = taskId == null ? null : taskMapper.selectById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("采集任务不存在");
        }
        return task;
    }

    private HerbCollectionTaskEntity requirePublicTask(String traceCode) {
        if (!StringUtils.hasText(traceCode)) {
            throw new BusinessException("数字生命档案溯源码不能为空");
        }
        HerbCollectionTaskEntity task = taskMapper.selectByTraceCode(traceCode);
        if (task == null) {
            throw new ResourceNotFoundException("数字生命档案溯源码不存在");
        }
        if (!Integer.valueOf(1).equals(task.getStatus())
                || HerbCollectionTaskStatusConstants.CANCELLED.equals(task.getTaskStatus())) {
            throw new ForbiddenException("数字生命档案已禁用");
        }
        if (!Integer.valueOf(1).equals(task.getPublicVisible())) {
            throw new ForbiddenException("数字生命档案尚未公开");
        }
        return task;
    }

    private boolean isPublicApproved(GrowthRecordVO record) {
        return record != null
                && APPROVED.equals(record.getReviewStatus())
                && Integer.valueOf(1).equals(record.getPublicVisible());
    }

    private HerbDigitalLifeArchiveVO decorateArchive(
            HerbDigitalLifeArchiveVO archive, HerbCollectionTaskEntity task) {
        archive.setTraceCode(task.getTraceCode());
        archive.setPublicVisible(Integer.valueOf(1).equals(task.getPublicVisible()));
        return archive;
    }

    private HerbCollectionTaskVO toTaskVO(HerbCollectionTaskEntity task) {
        HerbCollectionTaskVO vo = new HerbCollectionTaskVO();
        vo.setId(task.getId());
        vo.setTaskCode(task.getTaskCode());
        vo.setTaskName(task.getTaskName());
        vo.setSpeciesId(task.getSpeciesId());
        vo.setSpeciesName(task.getSpeciesName());
        vo.setBaseId(task.getBaseId());
        vo.setBaseName(task.getBaseName());
        vo.setCollectPlace(task.getCollectPlace());
        vo.setCollectorId(task.getCollectorId());
        vo.setCollectorName(task.getCollectorName());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setDescription(task.getDescription());
        vo.setTraceCode(task.getTraceCode());
        vo.setPublicVisible(task.getPublicVisible());
        return vo;
    }

    private HerbDigitalLifePublicArchiveVO toPublicArchive(HerbDigitalLifeArchiveVO source) {
        HerbDigitalLifePublicArchiveVO target = new HerbDigitalLifePublicArchiveVO();
        target.setTraceCode(source.getTraceCode());
        target.setTaskCode(source.getTaskCode());
        target.setTaskName(source.getTaskName());
        target.setSpeciesName(source.getSpeciesName());
        target.setBaseName(source.getBaseName());
        target.setDescription(source.getDescription());
        target.setStageCount(source.getStageCount());
        target.setValidStageCount(source.getValidStageCount());
        target.setImageCount(source.getImageCount());
        target.setStartTime(source.getStartTime());
        target.setEndTime(source.getEndTime());
        target.setArchiveStatus(source.getArchiveStatus());
        target.setQrCodeUrl(
                "/api/trace/digital-life/" + source.getTraceCode() + "/qrcode");
        target.setStages(
                source.getStages().stream()
                        .map(stage -> toPublicStage(stage, source.getTraceCode()))
                        .toList());
        return target;
    }

    private HerbDigitalLifePublicStageVO toPublicStage(
            HerbDigitalLifeStageVO source, String traceCode) {
        HerbDigitalLifePublicStageVO target = new HerbDigitalLifePublicStageVO();
        target.setSequence(source.getSequence());
        target.setBatchCode(source.getBatchCode());
        target.setBatchName(source.getBatchName());
        target.setGrowthStage(source.getGrowthStage());
        target.setCollectedAt(source.getCollectedAt());
        target.setCollectorName(source.getCollectorName());
        target.setBaseName(source.getBaseName());
        target.setLocationName(source.getLocationName());
        target.setLongitude(source.getLongitude());
        target.setLatitude(source.getLatitude());
        target.setAuditStatus("已通过");
        target.setReviewedAt(source.getReviewedAt());
        target.setDataStatus(source.getDataStatus());
        target.setMetrics(toPublicMetrics(source.getMetrics()));
        target.setRecognition(source.getRecognition());
        target.setAiNarration(source.getAiNarration());
        target.setNarrationSource(source.getNarrationSource());
        target.setNarrationGeneratedTime(source.getNarrationGeneratedTime());
        target.setImages(
                source.getImages().stream().map(image -> toPublicImage(image, traceCode)).toList());
        return target;
    }

    private void attachNarrations(HerbDigitalLifeArchiveVO archive) {
        List<Long> recordIds =
                archive.getStages().stream()
                        .map(HerbDigitalLifeStageVO::getGrowthRecordId)
                        .filter(Objects::nonNull)
                        .toList();
        if (recordIds.isEmpty()) {
            return;
        }
        Map<Long, DigitalLifeNarrationEntity> narrations =
                narrationMapper
                        .selectList(
                                new LambdaQueryWrapper<DigitalLifeNarrationEntity>()
                                        .in(
                                                DigitalLifeNarrationEntity::getGrowthRecordId,
                                                recordIds)
                                        .eq(
                                                DigitalLifeNarrationEntity::getNarrationType,
                                                "stage_summary")
                                        .eq(
                                                DigitalLifeNarrationEntity::getPromptVersion,
                                                "stage-v1"))
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        DigitalLifeNarrationEntity::getGrowthRecordId,
                                        narration -> narration,
                                        (left, right) -> left));
        archive.getStages()
                .forEach(
                        stage -> {
                            DigitalLifeNarrationEntity narration =
                                    narrations.get(stage.getGrowthRecordId());
                            if (narration != null) {
                                stage.setAiNarration(narration.getNarrationText());
                                stage.setNarrationSource(narration.getNarrationSource());
                                stage.setNarrationGeneratedTime(narration.getGeneratedTime());
                            }
                        });
    }

    private HerbDigitalLifeMetricsVO toPublicMetrics(HerbDigitalLifeMetricsVO source) {
        if (source == null) {
            return null;
        }
        HerbDigitalLifeMetricsVO target = new HerbDigitalLifeMetricsVO();
        target.setPlantHeight(source.getPlantHeight());
        target.setStemDiameter(source.getStemDiameter());
        target.setLeafColor(source.getLeafColor());
        target.setFloweringStatus(source.getFloweringStatus());
        target.setTemperature(source.getTemperature());
        target.setHumidity(source.getHumidity());
        target.setSoilMoisture(source.getSoilMoisture());
        target.setSoilPh(source.getSoilPh());
        target.setLight(source.getLight());
        target.setGrowthEvaluation(source.getGrowthEvaluation());
        return target;
    }

    private HerbDigitalLifePublicImageVO toPublicImage(
            HerbDigitalLifeImageVO source, String traceCode) {
        HerbDigitalLifePublicImageVO target = new HerbDigitalLifePublicImageVO();
        target.setImageUrl(
                "/api/trace/digital-life/" + traceCode + "/images/" + source.getImageId());
        target.setImageType(source.getImageType());
        target.setImageTypeName(source.getImageTypeName());
        target.setUploadTime(source.getUploadTime());
        target.setUploaderName(source.getUploaderName());
        target.setPrimaryImage(source.getPrimaryImage());
        return target;
    }

    private void sanitizeManagementImageUrls(HerbDigitalLifeArchiveVO archive) {
        archive.getStages().stream()
                .flatMap(stage -> stage.getImages().stream())
                .forEach(
                        image -> {
                            image.setImageUrl(browserUrlOrNull(image.getImageUrl()));
                            image.setThumbnailUrl(browserUrlOrNull(image.getThumbnailUrl()));
                        });
    }

    private String browserUrlOrNull(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        return url.startsWith("/api/")
                        || url.startsWith("/storage/")
                        || url.startsWith("http://")
                        || url.startsWith("https://")
                ? url
                : null;
    }

    private String fullPublicUrl(String path) {
        if (!StringUtils.hasText(publicWebBaseUrl)) {
            return path;
        }
        return publicWebBaseUrl.replaceAll("/+$", "") + path;
    }

    private static <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
