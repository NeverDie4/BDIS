package com.bdis.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.ResourceNotFoundException;
import com.bdis.file.service.FileResourceService;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbBatchImageMapper;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.collection.support.CollectionAccessService;
import com.bdis.modules.collection.vo.HerbBatchImageVO;
import com.bdis.modules.collection.vo.HerbBatchVO;
import com.bdis.modules.growth.entity.DigitalLifeNarrationEntity;
import com.bdis.modules.growth.mapper.DigitalLifeNarrationMapper;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.growth.service.impl.HerbDigitalLifeArchiveServiceImpl;
import com.bdis.modules.growth.support.HerbDigitalLifeArchiveAssembler;
import com.bdis.modules.growth.vo.GrowthRecordVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicArchiveVO;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.vo.HerbImageVO;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import javax.imageio.ImageIO;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class HerbDigitalLifeArchiveServiceImplTest {

    @Mock private HerbCollectionTaskMapper taskMapper;
    @Mock private HerbBatchMapper batchMapper;
    @Mock private GrowthRecordMapper growthRecordMapper;
    @Mock private HerbImageMapper imageMapper;
    @Mock private HerbBatchImageMapper batchImageMapper;
    @Mock private CollectionAccessService collectionAccessService;
    @Mock private FileResourceService fileResourceService;
    @Mock private DigitalLifeNarrationMapper narrationMapper;

    private HerbDigitalLifeArchiveServiceImpl service;

    @BeforeEach
    void setUp() {
        service =
                new HerbDigitalLifeArchiveServiceImpl(
                        taskMapper,
                        batchMapper,
                        growthRecordMapper,
                        imageMapper,
                        batchImageMapper,
                        collectionAccessService,
                        new HerbDigitalLifeArchiveAssembler(),
                        fileResourceService,
                        narrationMapper);
    }

    @AfterEach
    void clearInterruptFlag() {
        Thread.interrupted();
    }

    @Test
    void publicQrCodeTargetsTaskLevelDigitalLifePage() throws Exception {
        HerbCollectionTaskEntity task = task(true);
        when(taskMapper.selectByTraceCode("DL-009")).thenReturn(task);
        ReflectionTestUtils.setField(
                service, "publicWebBaseUrl", "https://archive.example.com/");

        try (InputStream input = service.publicQrCode("DL-009").getResource().getInputStream()) {
            var image = ImageIO.read(input);
            var bitmap =
                    new BinaryBitmap(
                            new HybridBinarizer(new BufferedImageLuminanceSource(image)));

            assertThat(new MultiFormatReader().decode(bitmap).getText())
                    .isEqualTo(
                            "https://archive.example.com/trace/digital-life/DL-009");
        }
    }

    @Test
    void unpublishedArchiveCannotExposeTaskLevelQrCode() {
        when(taskMapper.selectByTraceCode("DL-009")).thenReturn(task(false));

        assertThatThrownBy(() -> service.publicQrCode("DL-009"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void managementArchiveAggregatesAndSortsWithOneBulkQueryPerResource() {
        HerbCollectionTaskEntity task = task(true);
        HerbBatchVO later = batch(2L, LocalDateTime.of(2026, 7, 2, 9, 0));
        HerbBatchVO earlier = batch(1L, LocalDateTime.of(2026, 7, 1, 9, 0));
        GrowthRecordVO laterRecord = record(22L, 2L, "approved");
        GrowthRecordVO earlierRecord = record(11L, 1L, "approved");
        HerbImageVO image1 = image(101L, 1L);
        HerbImageVO image2 = image(102L, 2L);
        HerbBatchImageVO binding1 = binding(1L, 101L);
        HerbBatchImageVO binding2 = binding(2L, 102L);
        when(taskMapper.selectById(9L)).thenReturn(task);
        when(batchMapper.selectByTaskId(9L)).thenReturn(List.of(later, earlier));
        when(growthRecordMapper.selectJoinedByBatchIds(List.of(2L, 1L)))
                .thenReturn(List.of(laterRecord, earlierRecord));
        when(imageMapper.selectByBatchIds(List.of(2L, 1L))).thenReturn(List.of(image2, image1));
        when(batchImageMapper.selectBatchImagesWithIdentificationByBatchIds(List.of(2L, 1L)))
                .thenReturn(List.of(binding2, binding1));

        HerbDigitalLifeArchiveVO archive = service.getByTaskId(9L);

        assertThat(archive.getStages())
                .extracting(stage -> stage.getBatchId())
                .containsExactly(1L, 2L);
        assertThat(archive.getStageCount()).isEqualTo(2);
        assertThat(archive.getImageCount()).isEqualTo(2);
        verify(collectionAccessService).requireTaskAccess(task);
        verify(batchMapper, times(1)).selectByTaskId(9L);
        verify(growthRecordMapper, times(1)).selectJoinedByBatchIds(anyList());
        verify(imageMapper, times(1)).selectByBatchIds(anyList());
        verify(batchImageMapper, times(1)).selectBatchImagesWithIdentificationByBatchIds(anyList());
        verify(narrationMapper, times(1)).selectList(any());
    }

    @Test
    void publicArchiveOnlyReturnsApprovedStagesAndChineseAuditStatus() {
        HerbCollectionTaskEntity task = task(true);
        HerbBatchVO approvedBatch = batch(1L, LocalDateTime.of(2026, 7, 1, 9, 0));
        HerbBatchVO submittedBatch = batch(2L, LocalDateTime.of(2026, 7, 2, 9, 0));
        when(taskMapper.selectByTraceCode("DL-009")).thenReturn(task);
        when(batchMapper.selectByTaskId(9L)).thenReturn(List.of(approvedBatch, submittedBatch));
        when(growthRecordMapper.selectJoinedByBatchIds(List.of(1L, 2L)))
                .thenReturn(List.of(record(11L, 1L, "approved"), record(22L, 2L, "submitted")));
        when(imageMapper.selectByBatchIds(List.of(1L))).thenReturn(List.of(image(101L, 1L)));
        when(batchImageMapper.selectBatchImagesWithIdentificationByBatchIds(List.of(1L)))
                .thenReturn(List.of(binding(1L, 101L)));
        DigitalLifeNarrationEntity narration = new DigitalLifeNarrationEntity();
        narration.setGrowthRecordId(11L);
        narration.setNarrationText("已缓存阶段解说");
        narration.setNarrationSource("template");
        narration.setGeneratedTime(LocalDateTime.of(2026, 7, 15, 10, 0));
        when(narrationMapper.selectList(any())).thenReturn(List.of(narration));

        HerbDigitalLifePublicArchiveVO archive = service.publicArchive("DL-009");

        assertThat(archive.getQrCodeUrl())
                .isEqualTo("/api/trace/digital-life/DL-009/qrcode");
        assertThat(archive.getStages()).hasSize(1);
        assertThat(archive.getStages().get(0).getBatchCode()).isEqualTo("BATCH-1");
        assertThat(archive.getStages().get(0).getAuditStatus()).isEqualTo("已通过");
        assertThat(archive.getStages().get(0).getCollectorName()).isEqualTo("采集员1");
        assertThat(archive.getStages().get(0).getLongitude()).isEqualByComparingTo("108.123456");
        assertThat(archive.getStages().get(0).getLatitude()).isEqualByComparingTo("30.123456");
        assertThat(archive.getStages().get(0).getDataStatus()).isEqualTo("complete");
        assertThat(archive.getStages().get(0).getImages().get(0).getImageUrl())
                .isEqualTo("/api/trace/digital-life/DL-009/images/101");
        assertThat(archive.getStages().get(0).getImages().get(0).getUploaderName())
                .isEqualTo("采集员1");
        assertThat(archive.getStages().get(0).getAiNarration()).isEqualTo("已缓存阶段解说");
        assertThat(archive.getStages().get(0).getNarrationSource()).isEqualTo("template");
        verify(imageMapper).selectByBatchIds(List.of(1L));
        verify(batchImageMapper).selectBatchImagesWithIdentificationByBatchIds(List.of(1L));
    }

    @Test
    void unpublishedArchiveCannotBeQueriedPublicly() {
        HerbCollectionTaskEntity task = task(false);
        when(taskMapper.selectByTraceCode("DL-009")).thenReturn(task);

        assertThatThrownBy(() -> service.publicArchive("DL-009"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("尚未公开");
        verify(batchMapper, never()).selectByTaskId(9L);
    }

    @Test
    void missingImagesAndRecognitionReturnUsableStage() {
        HerbCollectionTaskEntity task = task(true);
        HerbBatchVO batch = batch(1L, LocalDateTime.of(2026, 7, 1, 9, 0));
        when(taskMapper.selectById(9L)).thenReturn(task);
        when(batchMapper.selectByTaskId(9L)).thenReturn(List.of(batch));
        when(growthRecordMapper.selectJoinedByBatchIds(List.of(1L)))
                .thenReturn(List.of(record(11L, 1L, "approved")));
        when(imageMapper.selectByBatchIds(List.of(1L))).thenReturn(List.of());
        when(batchImageMapper.selectBatchImagesWithIdentificationByBatchIds(List.of(1L)))
                .thenReturn(List.of());

        HerbDigitalLifeArchiveVO archive = service.getByTaskId(9L);

        assertThat(archive.getStages()).hasSize(1);
        assertThat(archive.getStages().get(0).getDataStatus()).isEqualTo("missing_images");
        assertThat(archive.getStages().get(0).getRecognition()).isNull();
    }

    @Test
    void managementArchiveRejectsTaskOutsideCurrentScope() {
        HerbCollectionTaskEntity task = task(true);
        when(taskMapper.selectById(9L)).thenReturn(task);
        doThrow(new ForbiddenException("采集任务超出当前数据范围"))
                .when(collectionAccessService)
                .requireTaskAccess(task);

        assertThatThrownBy(() -> service.getByTaskId(9L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("数据范围");
        verify(batchMapper, never()).selectByTaskId(9L);
    }

    @Test
    void taskWithoutBatchesReturnsEmptyArchiveWithoutDetailQueries() {
        HerbCollectionTaskEntity task = task(true);
        when(taskMapper.selectById(9L)).thenReturn(task);
        when(batchMapper.selectByTaskId(9L)).thenReturn(List.of());

        HerbDigitalLifeArchiveVO archive = service.getByTaskId(9L);

        assertThat(archive.getArchiveStatus()).isEqualTo("empty");
        assertThat(archive.getStages()).isEmpty();
        verify(growthRecordMapper, never()).selectJoinedByBatchIds(anyList());
        verify(imageMapper, never()).selectByBatchIds(anyList());
        verify(batchImageMapper, never()).selectBatchImagesWithIdentificationByBatchIds(anyList());
    }

    @Test
    void missingPublicImageFileReturnsClearError() {
        HerbImageVO image = image(101L, 1L);
        when(imageMapper.selectPublicByTraceCodeAndImageId("DL-009", 101L)).thenReturn(image);
        when(fileResourceService.resolveFileId(image.getImageUrl())).thenReturn(null);

        assertThatThrownBy(() -> service.publicImage("DL-009", 101L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("图片文件不存在");
        verify(taskMapper, never()).selectByTraceCode("DL-009");
        verify(batchMapper, never()).selectByTaskId(any());
        verify(growthRecordMapper, never()).selectJoinedByBatchIds(anyList());
        verify(imageMapper, never()).selectByBatchIds(anyList());
    }

    @Test
    void missingTaskReturnsClearError() {
        when(taskMapper.selectById(404L)).thenReturn(null);

        assertThatThrownBy(() -> service.getByTaskId(404L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("采集任务不存在");
    }

    @Test
    void missingTraceCodeReturnsClearError() {
        when(taskMapper.selectByTraceCode("missing")).thenReturn(null);

        assertThatThrownBy(() -> service.publicArchive("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("溯源码不存在");
    }

    @Test
    void disabledTaskCannotBeQueriedPublicly() {
        HerbCollectionTaskEntity task = task(true);
        task.setStatus(0);
        when(taskMapper.selectByTraceCode("DL-009")).thenReturn(task);

        assertThatThrownBy(() -> service.publicArchive("DL-009"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("已禁用");
    }

    private static HerbCollectionTaskEntity task(boolean publicVisible) {
        HerbCollectionTaskEntity task = new HerbCollectionTaskEntity();
        task.setId(9L);
        task.setTaskCode("TASK-009");
        task.setTaskName("黄连连续观测");
        task.setSpeciesId(8L);
        task.setSpeciesName("黄连");
        task.setBaseName("石柱黄连基地");
        task.setCollectPlace("重庆石柱");
        task.setCollectorName("采集员1");
        task.setDescription("连续观测任务");
        task.setTraceCode("DL-009");
        task.setPublicVisible(publicVisible ? 1 : 0);
        task.setTaskStatus("in_progress");
        task.setStatus(1);
        return task;
    }

    private static HerbBatchVO batch(Long id, LocalDateTime collectedAt) {
        HerbBatchVO batch = new HerbBatchVO();
        batch.setId(id);
        batch.setTaskId(9L);
        batch.setBatchCode("BATCH-" + id);
        batch.setBatchName("观测批次" + id);
        batch.setBaseName("石柱黄连基地");
        batch.setOriginPlace("重庆石柱");
        batch.setCollectStartTime(collectedAt);
        batch.setCreateTime(collectedAt);
        return batch;
    }

    private static GrowthRecordVO record(Long id, Long batchId, String reviewStatus) {
        GrowthRecordVO record = new GrowthRecordVO();
        record.setId(id);
        record.setBatchId(batchId);
        record.setTaskId(9L);
        record.setGrowthStage("幼苗期");
        record.setCollectedAt(LocalDateTime.of(2026, 7, batchId.intValue(), 9, 0));
        record.setCollectorName("采集员1");
        record.setLongitude(new BigDecimal("108.123456"));
        record.setLatitude(new BigDecimal("30.123456"));
        record.setReviewStatus(reviewStatus);
        return record;
    }

    private static HerbImageVO image(Long id, Long batchId) {
        HerbImageVO image = new HerbImageVO();
        image.setId(id);
        image.setBatchId(batchId);
        image.setImageUrl("/api/files/" + id + "/content");
        image.setImageType("leaf");
        image.setUploaderName("采集员1");
        return image;
    }

    private static HerbBatchImageVO binding(Long batchId, Long imageId) {
        HerbBatchImageVO binding = new HerbBatchImageVO();
        binding.setBatchId(batchId);
        binding.setImageId(imageId);
        binding.setIsPrimary(1);
        return binding;
    }
}
