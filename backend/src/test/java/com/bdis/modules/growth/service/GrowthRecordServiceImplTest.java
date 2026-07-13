package com.bdis.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbBatchMapper;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.dictionary.support.DictionaryReferenceValidator;
import com.bdis.modules.growth.dto.GrowthAuditCommentRequest;
import com.bdis.modules.growth.dto.GrowthRecordUpsertRequest;
import com.bdis.modules.growth.entity.GrowthAuditRecordEntity;
import com.bdis.modules.growth.entity.GrowthRecordEntity;
import com.bdis.modules.growth.entity.GrowthTraceEventEntity;
import com.bdis.modules.growth.mapper.GrowthAuditRecordMapper;
import com.bdis.modules.growth.mapper.GrowthRecordMapper;
import com.bdis.modules.growth.mapper.GrowthTraceEventMapper;
import com.bdis.modules.growth.service.impl.GrowthRecordServiceImpl;
import com.bdis.modules.growth.vo.GrowthChartPointVO;
import com.bdis.modules.growth.vo.GrowthPublicTraceArchiveVO;
import com.bdis.modules.growth.vo.GrowthTraceQrCodeVO;
import com.bdis.modules.herb.vo.HerbImageVO;
import com.bdis.modules.herb.entity.HerbEntity;
import com.bdis.modules.herb.mapper.HerbImageMapper;
import com.bdis.modules.herb.mapper.HerbMapper;
import com.bdis.modules.map.mapper.MapPointMapper;
import com.bdis.modules.permission.service.DataScopeService;
import com.bdis.modules.permission.vo.DataScopeResultVO;
import com.bdis.modules.user.mapper.UserMapper;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GrowthRecordServiceImplTest {

    @Mock private GrowthRecordMapper growthRecordMapper;
    @Mock private GrowthAuditRecordMapper growthAuditRecordMapper;
    @Mock private GrowthTraceEventMapper growthTraceEventMapper;
    @Mock private HerbBatchMapper herbBatchMapper;
    @Mock private HerbCollectionTaskMapper herbCollectionTaskMapper;
    @Mock private MapPointMapper mapPointMapper;
    @Mock private HerbMapper herbMapper;
    @Mock private HerbImageMapper herbImageMapper;
    @Mock private UserMapper userMapper;
    @Mock private DataScopeService dataScopeService;
    @Mock private DictionaryReferenceValidator dictionaryReferenceValidator;

    private GrowthRecordServiceImpl service;
    @org.junit.jupiter.api.io.TempDir Path tempDir;

    @BeforeEach
    void setUp() {
        service =
                new GrowthRecordServiceImpl(
                        growthRecordMapper,
                        growthAuditRecordMapper,
                        growthTraceEventMapper,
                        herbBatchMapper,
                        herbCollectionTaskMapper,
                        mapPointMapper,
                        herbMapper,
                        herbImageMapper,
                        userMapper,
                        dataScopeService,
                        dictionaryReferenceValidator);
        ReflectionTestUtils.setField(service, "storagePath", tempDir.toString());
        DataScopeResultVO allScope = new DataScopeResultVO();
        allScope.setAllIncluded(true);
        lenient()
                .when(dataScopeService.resolveForCurrentUser("herb_growth_record"))
                .thenReturn(allScope);
        HerbEntity herb = new HerbEntity();
        herb.setId(10L);
        herb.setHerbName("黄连");
        lenient().when(herbMapper.selectById(10L)).thenReturn(herb);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRequiresBatchId() {
        assertThatThrownBy(() -> service.create(new GrowthRecordUpsertRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("采集批次不能为空");
    }

    @Test
    void createForBatchRejectsSecondGrowthRecord() {
        HerbBatchEntity batch = new HerbBatchEntity();
        batch.setId(20L);
        batch.setTaskId(30L);
        when(herbBatchMapper.selectById(20L)).thenReturn(batch);
        when(growthRecordMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.createForBatch(20L, new GrowthRecordUpsertRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已存在生长记录");
    }

    @Test
    void chartUsesSelectedMetricAsPointValue() {
        HerbCollectionTaskEntity task = new HerbCollectionTaskEntity();
        task.setId(30L);
        when(herbCollectionTaskMapper.selectById(30L)).thenReturn(task);
        GrowthRecordEntity record = new GrowthRecordEntity();
        record.setId(40L);
        when(growthRecordMapper.selectList(any())).thenReturn(List.of(record));
        GrowthChartPointVO point = new GrowthChartPointVO();
        point.setRecordId(40L);
        point.setTemperature(new BigDecimal("22.50"));
        when(growthRecordMapper.selectChartPoints(30L, List.of(40L))).thenReturn(List.of(point));

        List<GrowthChartPointVO> result = service.getChartByTaskId(30L, "temperature");

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getMetric()).isEqualTo("temperature");
        assertThat(result.getFirst().getValue()).isEqualByComparingTo("22.50");
    }

    @Test
    void draftCanSubmitAndWritesAuditAndTrace() {
        login(1L, "COLLECTOR");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("draft", 1L));

        service.submit(100L);

        assertAudit("submit", "draft", "submitted");
        assertTrace("submitted", "draft", "submitted");
    }

    @Test
    void rejectedCanResubmitAndWritesDistinctAuditAndTrace() {
        login(1L, "COLLECTOR");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("rejected", 1L));

        service.submit(100L);

        assertAudit("resubmit", "rejected", "submitted");
        assertTrace("resubmitted", "rejected", "submitted");
    }

    @Test
    void submittedCanApproveAndWritesAuditAndTrace() {
        login(2L, "REVIEWER");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("submitted", 1L));
        GrowthAuditCommentRequest request = comment("数据完整，审核通过");

        service.approve(100L, request);

        assertAudit("approve", "submitted", "approved");
        assertTrace("approved", "submitted", "approved");
    }

    @Test
    void submittedCanRejectAndWritesAuditAndTrace() {
        login(2L, "REVIEWER");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("submitted", 1L));

        service.reject(100L, comment("图片不清晰"));

        assertAudit("reject", "submitted", "rejected");
        assertTrace("rejected", "submitted", "rejected");
    }

    @Test
    void approvedCanArchiveByAdminAndWritesAuditAndTrace() {
        login(3L, "ADMIN");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("approved", 1L));

        service.archive(100L, comment("审核完成，归档保存"));

        assertAudit("archive", "approved", "archived");
        assertTrace("archived", "approved", "archived");
    }

    @Test
    void archivedCannotBeUpdated() {
        login(1L, "COLLECTOR");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("archived", 1L));

        assertThatThrownBy(() -> service.update(100L, upsert()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void draftCannotBeApproved() {
        login(2L, "REVIEWER");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("draft", 1L));

        assertThatThrownBy(() -> service.approve(100L, comment("ok")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectedCannotBeArchived() {
        login(3L, "ADMIN");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("rejected", 1L));

        assertThatThrownBy(() -> service.archive(100L, comment("archive")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void submittedCannotBeUpdated() {
        login(1L, "COLLECTOR");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("submitted", 1L));

        assertThatThrownBy(() -> service.update(100L, upsert()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void rejectRequiresComment() {
        login(2L, "REVIEWER");

        assertThatThrownBy(() -> service.reject(100L, new GrowthAuditCommentRequest()))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("resultCode", ResultCodeEnum.VALIDATION_ERROR);
    }

    @Test
    void reviewerWithoutAdminRoleCannotArchive() {
        login(2L, "REVIEWER");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("approved", 1L));

        assertThatThrownBy(() -> service.archive(100L, comment("archive")))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void updateWritesTraceMetadataSummary() {
        login(1L, "COLLECTOR");
        GrowthRecordEntity record = record("draft", 1L);
        record.setRemark("old");
        when(growthRecordMapper.selectById(100L)).thenReturn(record);

        service.update(100L, upsert());

        ArgumentCaptor<GrowthTraceEventEntity> captor =
                ArgumentCaptor.forClass(GrowthTraceEventEntity.class);
        verify(growthTraceEventMapper).insert(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("updated");
        assertThat(captor.getValue().getMetadataJson()).contains("修改了备注");
    }

    @Test
    void traceEventsReturnInMapperOrder() {
        login(1L, "COLLECTOR");
        when(growthRecordMapper.selectById(100L)).thenReturn(record("approved", 1L));
        GrowthTraceEventEntity first =
                traceEvent("created", LocalDateTime.parse("2026-07-12T10:00:00"));
        GrowthTraceEventEntity second =
                traceEvent("approved", LocalDateTime.parse("2026-07-12T11:00:00"));
        when(growthTraceEventMapper.selectList(any())).thenReturn(List.of(first, second));

        assertThat(service.trace(100L))
                .extracting("eventType")
                .containsExactly("created", "approved");
    }

    @Test
    void generateTraceCodeCreatesCodeAndEvent() {
        login(3L, "ADMIN");
        GrowthRecordEntity record = record("approved", 1L);
        when(growthRecordMapper.selectById(100L)).thenReturn(record);
        when(growthRecordMapper.selectCount(any())).thenReturn(0L);

        GrowthTraceQrCodeVO result = service.generateTraceCode(100L);

        assertThat(result.getRecordId()).isEqualTo(100L);
        assertThat(result.getTraceCode()).startsWith("TRACE_GROWTH_");
        assertThat(result.getTraceUrl()).isEqualTo("/trace/growth/" + result.getTraceCode());
        assertThat(result.getPublicVisible()).isZero();
        assertTrace("trace_code_generated", "approved", "approved");
    }

    @Test
    void generateTraceCodeReturnsExistingCode() {
        login(3L, "ADMIN");
        GrowthRecordEntity record = record("approved", 1L);
        record.setTraceCode("TRACE_GROWTH_EXISTING");
        record.setTracePublicUrl("/trace/growth/TRACE_GROWTH_EXISTING");
        when(growthRecordMapper.selectById(100L)).thenReturn(record);

        GrowthTraceQrCodeVO result = service.generateTraceCode(100L);

        assertThat(result.getTraceCode()).isEqualTo("TRACE_GROWTH_EXISTING");
        org.mockito.Mockito.verify(growthTraceEventMapper, org.mockito.Mockito.never())
                .insert(any(GrowthTraceEventEntity.class));
    }

    @Test
    void generateQrCodeCreatesTraceCodeWhenMissing() {
        login(3L, "ADMIN");
        GrowthRecordEntity record = record("approved", 1L);
        when(growthRecordMapper.selectById(100L)).thenReturn(record);
        when(growthRecordMapper.selectCount(any())).thenReturn(0L);

        GrowthTraceQrCodeVO result =
                service.generateTraceQrCode(100L, "http://localhost:8080/api");

        assertThat(result.getTraceCode()).startsWith("TRACE_GROWTH_");
        assertThat(result.getQrCodeUrl()).startsWith("/api/files/trace/qrcode/growth_100_");
        ArgumentCaptor<GrowthTraceEventEntity> captor =
                ArgumentCaptor.forClass(GrowthTraceEventEntity.class);
        verify(growthTraceEventMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(GrowthTraceEventEntity::getEventType)
                .containsExactly("trace_code_generated", "trace_qrcode_generated");
    }

    @Test
    void enableAndDisablePublicTraceWriteEvents() {
        login(3L, "ADMIN");
        GrowthRecordEntity record = record("approved", 1L);
        record.setTraceCode("TRACE_GROWTH_EXISTING");
        record.setTracePublicUrl("/trace/growth/TRACE_GROWTH_EXISTING");
        when(growthRecordMapper.selectById(100L)).thenReturn(record);

        service.enablePublicTrace(100L);
        service.disablePublicTrace(100L);

        ArgumentCaptor<GrowthTraceEventEntity> captor =
                ArgumentCaptor.forClass(GrowthTraceEventEntity.class);
        verify(growthTraceEventMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(GrowthTraceEventEntity::getEventType)
                .containsExactly("public_trace_enabled", "public_trace_disabled");
        assertThat(record.getPublicVisible()).isZero();
    }

    @Test
    void publicTraceRejectsHiddenRecord() {
        GrowthRecordEntity record = record("approved", 1L);
        record.setTraceCode("TRACE_GROWTH_EXISTING");
        record.setPublicVisible(0);
        when(growthRecordMapper.selectOne(any())).thenReturn(record);

        assertThatThrownBy(() -> service.publicTrace("TRACE_GROWTH_EXISTING"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("暂未公开");
    }

    @Test
    void publicTraceReturnsArchiveImagesAuditAndEvents() {
        GrowthRecordEntity record = record("approved", 1L);
        record.setTraceCode("TRACE_GROWTH_EXISTING");
        record.setPublicVisible(1);
        record.setBatchId(20L);
        record.setTaskId(30L);
        record.setBaseName("本草基地");
        record.setPlantHeight(new BigDecimal("18.50"));
        when(growthRecordMapper.selectOne(any())).thenReturn(record);
        HerbBatchEntity batch = new HerbBatchEntity();
        batch.setId(20L);
        batch.setBatchName("采集批次A");
        when(herbBatchMapper.selectById(20L)).thenReturn(batch);
        HerbCollectionTaskEntity task = new HerbCollectionTaskEntity();
        task.setId(30L);
        task.setTaskName("采集任务A");
        task.setCollectPlace("标本园");
        when(herbCollectionTaskMapper.selectById(30L)).thenReturn(task);
        HerbImageVO image = new HerbImageVO();
        image.setImageUrl("/api/files/1/content");
        image.setImageType("现场图");
        image.setImageRole("whole_plant");
        image.setUploaderName("上传人");
        image.setUploadTime(LocalDateTime.parse("2026-07-13T10:00:00"));
        when(herbImageMapper.selectByBatchId(20L)).thenReturn(List.of(image));
        GrowthAuditRecordEntity audit = new GrowthAuditRecordEntity();
        audit.setReviewAction("approve");
        audit.setBeforeStatus("submitted");
        audit.setAfterStatus("approved");
        audit.setReviewComment("通过");
        audit.setReviewerName("审核人");
        audit.setReviewedAt(LocalDateTime.parse("2026-07-13T11:00:00"));
        when(growthAuditRecordMapper.selectList(any())).thenReturn(List.of(audit));
        when(growthTraceEventMapper.selectList(any()))
                .thenReturn(List.of(traceEvent("public_trace_enabled", LocalDateTime.now())));

        GrowthPublicTraceArchiveVO archive = service.publicTrace("TRACE_GROWTH_EXISTING");

        assertThat(archive.getRecordId()).isEqualTo(100L);
        assertThat(archive.getBatchName()).isEqualTo("采集批次A");
        assertThat(archive.getTaskName()).isEqualTo("采集任务A");
        assertThat(archive.getImages()).hasSize(1);
        assertThat(archive.getAuditHistory()).hasSize(1);
        assertThat(archive.getTraceTimeline()).hasSize(1);
        assertThat(archive.getLatestAuditResult()).isEqualTo("approved");
    }

    private void assertAudit(String action, String before, String after) {
        ArgumentCaptor<GrowthAuditRecordEntity> captor =
                ArgumentCaptor.forClass(GrowthAuditRecordEntity.class);
        verify(growthAuditRecordMapper).insert(captor.capture());
        GrowthAuditRecordEntity audit = captor.getValue();
        assertThat(audit.getReviewAction()).isEqualTo(action);
        assertThat(audit.getBeforeStatus()).isEqualTo(before);
        assertThat(audit.getAfterStatus()).isEqualTo(after);
        assertThat(audit.getReviewerId()).isNotNull();
        assertThat(audit.getReviewerName()).isNotBlank();
        assertThat(audit.getReviewerRole()).isNotBlank();
    }

    private void assertTrace(String eventType, String before, String after) {
        ArgumentCaptor<GrowthTraceEventEntity> captor =
                ArgumentCaptor.forClass(GrowthTraceEventEntity.class);
        verify(growthTraceEventMapper).insert(captor.capture());
        GrowthTraceEventEntity event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(eventType);
        assertThat(event.getBeforeStatus()).isEqualTo(before);
        assertThat(event.getAfterStatus()).isEqualTo(after);
        assertThat(event.getOperatorId()).isNotNull();
        assertThat(event.getOperatorName()).isNotBlank();
        assertThat(event.getOperatorRole()).isNotBlank();
    }

    private GrowthRecordEntity record(String status, Long collectorId) {
        GrowthRecordEntity record = new GrowthRecordEntity();
        record.setId(100L);
        record.setSpeciesId(10L);
        record.setCollectorId(collectorId);
        record.setCollectorNameSnapshot("采集员");
        record.setReviewStatus(status);
        record.setCreatedAt(LocalDateTime.parse("2026-07-12T09:00:00"));
        return record;
    }

    private GrowthRecordUpsertRequest upsert() {
        GrowthRecordUpsertRequest request = new GrowthRecordUpsertRequest();
        request.setSpeciesId(10L);
        request.setGrowthStage("seedling");
        request.setRemark("new");
        return request;
    }

    private GrowthAuditCommentRequest comment(String value) {
        GrowthAuditCommentRequest request = new GrowthAuditCommentRequest();
        request.setComment(value);
        return request;
    }

    private GrowthTraceEventEntity traceEvent(String eventType, LocalDateTime eventTime) {
        GrowthTraceEventEntity event = new GrowthTraceEventEntity();
        event.setEventType(eventType);
        event.setEventTitle(eventType);
        event.setEventContent(eventType);
        event.setEventTime(eventTime);
        return event;
    }

    private void login(Long userId, String roleCode) {
        CurrentUser currentUser =
                new CurrentUser(
                        userId,
                        "user" + userId,
                        "用户" + userId,
                        1L,
                        1L,
                        Set.of(roleCode),
                        Set.of(1L),
                        Set.of("growth:record:view"));
        SecurityContextHolder.getContext()
                .setAuthentication(
                        new UsernamePasswordAuthenticationToken(currentUser, null, List.of()));
    }
}
