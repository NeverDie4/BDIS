package com.bdis.modules.growth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.security.CurrentUser;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.growth.entity.DigitalLifeHashArchiveEntity;
import com.bdis.modules.growth.entity.DigitalLifeHashEventEntity;
import com.bdis.modules.growth.mapper.DigitalLifeHashArchiveMapper;
import com.bdis.modules.growth.mapper.DigitalLifeHashEventMapper;
import com.bdis.modules.growth.service.impl.DigitalLifeIntegrityServiceImpl;
import com.bdis.modules.growth.support.DigitalLifeHashChain;
import com.bdis.modules.growth.support.DigitalLifeHashChain.CanonicalEvent;
import com.bdis.modules.growth.support.DigitalLifeHashChain.HashedEvent;
import com.bdis.modules.growth.support.DigitalLifeIntegrityEventCollector;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifePublicArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class DigitalLifeIntegrityServiceImplTest {

    @Mock private HerbDigitalLifeArchiveService archiveService;
    @Mock private HerbCollectionTaskMapper taskMapper;
    @Mock private DigitalLifeIntegrityEventCollector eventCollector;
    @Mock private DigitalLifeHashArchiveMapper archiveMapper;
    @Mock private DigitalLifeHashEventMapper eventMapper;

    private DigitalLifeIntegrityServiceImpl service;
    private DigitalLifeHashChain hashChain;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        hashChain = new DigitalLifeHashChain(objectMapper);
        service =
                new DigitalLifeIntegrityServiceImpl(
                        archiveService,
                        taskMapper,
                        eventCollector,
                        objectMapper,
                        archiveMapper,
                        eventMapper);
        CurrentUser user =
                new CurrentUser(
                        7L,
                        "admin",
                        "管理员",
                        1L,
                        1L,
                        Set.of("ADMIN"),
                        Set.of(1L),
                        Set.of("growth:record:view"));
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void approvedArchiveGeneratesVersionedRootAndEvents() {
        HerbDigitalLifeArchiveVO archive = approvedArchive();
        when(archiveService.getByTaskId(9L)).thenReturn(archive);
        when(taskMapper.selectByIdForUpdate(9L)).thenReturn(new HerbCollectionTaskEntity());
        when(eventCollector.collect(archive)).thenReturn(events("21.5"));
        when(archiveMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        DigitalLifeIntegrityVO result = service.generate(9L);

        assertThat(result.verified()).isTrue();
        assertThat(result.eventCount()).isEqualTo(2);
        assertThat(result.rootHash()).hasSize(64);
        assertThat(result.hashVersion()).isEqualTo("sha256-v1:1");
        ArgumentCaptor<DigitalLifeHashArchiveEntity> archiveCaptor =
                ArgumentCaptor.forClass(DigitalLifeHashArchiveEntity.class);
        verify(archiveMapper).insert(archiveCaptor.capture());
        assertThat(archiveCaptor.getValue().getRootHash()).isEqualTo(result.rootHash());
        verify(eventMapper).insert(any(Collection.class));
        InOrder versionAllocation = inOrder(taskMapper, archiveMapper);
        versionAllocation.verify(taskMapper).selectByIdForUpdate(9L);
        versionAllocation.verify(archiveMapper).selectOne(any(Wrapper.class));
    }

    @Test
    void unchangedBusinessEventsVerifySuccessfully() {
        HerbDigitalLifeArchiveVO archive = approvedArchive();
        List<CanonicalEvent> currentEvents = events("21.5");
        List<HashedEvent> chain = hashChain.build(currentEvents);
        DigitalLifeHashArchiveEntity savedArchive = savedArchive(chain);
        when(archiveService.getByTaskId(9L)).thenReturn(archive);
        when(archiveMapper.selectOne(any(Wrapper.class))).thenReturn(savedArchive);
        when(eventMapper.selectList(any(Wrapper.class))).thenReturn(savedEvents(chain));
        when(eventCollector.collect(archive)).thenReturn(currentEvents);

        DigitalLifeIntegrityVO result = service.verify(9L);

        assertThat(result.verified()).isTrue();
        assertThat(result.message()).contains("校验通过");
    }

    @Test
    void changedBusinessPayloadFailsVerification() {
        HerbDigitalLifeArchiveVO archive = approvedArchive();
        List<HashedEvent> original = hashChain.build(events("21.5"));
        when(archiveService.getByTaskId(9L)).thenReturn(archive);
        when(archiveMapper.selectOne(any(Wrapper.class))).thenReturn(savedArchive(original));
        when(eventMapper.selectList(any(Wrapper.class))).thenReturn(savedEvents(original));
        when(eventCollector.collect(archive)).thenReturn(events("99.9"));

        DigitalLifeIntegrityVO result = service.verify(9L);

        assertThat(result.verified()).isFalse();
        assertThat(result.failedSequence()).isNotNull();
        assertThat(result.message()).contains("数据已发生变化");
    }

    @Test
    void archiveWithoutGeneratedChainReturnsExplicitState() {
        HerbDigitalLifeArchiveVO archive = approvedArchive();
        when(archiveService.getByTaskId(9L)).thenReturn(archive);
        when(archiveMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        DigitalLifeIntegrityVO result = service.verify(9L);

        assertThat(result.verified()).isFalse();
        assertThat(result.eventCount()).isZero();
        assertThat(result.message()).contains("尚未生成");
        verify(eventMapper, never()).selectList(any(Wrapper.class));
    }

    @Test
    void publicVerificationFirstChecksPublicArchive() {
        HerbCollectionTaskEntity task = new HerbCollectionTaskEntity();
        task.setId(9L);
        when(archiveService.publicArchive("DL-009"))
                .thenReturn(new HerbDigitalLifePublicArchiveVO());
        when(taskMapper.selectByTraceCode("DL-009")).thenReturn(task);
        when(archiveMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        DigitalLifeIntegrityVO result = service.verifyPublic("DL-009");

        assertThat(result.message()).contains("尚未生成");
        verify(archiveService).publicArchive("DL-009");
    }

    @Test
    void unapprovedArchiveCannotGenerateChain() {
        HerbDigitalLifeArchiveVO archive = approvedArchive();
        archive.getStages().get(0).setAuditStatus("submitted");
        when(archiveService.getByTaskId(9L)).thenReturn(archive);

        assertThatThrownBy(() -> service.generate(9L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("审核通过");
        verify(eventCollector, never()).collect(archive);
    }

    private HerbDigitalLifeArchiveVO approvedArchive() {
        HerbDigitalLifeStageVO stage = new HerbDigitalLifeStageVO();
        stage.setGrowthRecordId(11L);
        stage.setAuditStatus("approved");
        HerbDigitalLifeArchiveVO archive = new HerbDigitalLifeArchiveVO();
        archive.setTaskId(9L);
        archive.setStages(List.of(stage));
        return archive;
    }

    private List<CanonicalEvent> events(String height) {
        return List.of(
                new CanonicalEvent(
                        "collection_task",
                        9L,
                        "task_created",
                        LocalDateTime.of(2026, 7, 1, 9, 0),
                        7L,
                        "管理员",
                        Map.of("taskName", "黄连连续观测")),
                new CanonicalEvent(
                        "growth_record",
                        11L,
                        "growth_record_created",
                        LocalDateTime.of(2026, 7, 2, 9, 0),
                        8L,
                        "采集员1",
                        Map.of("plantHeight", new BigDecimal(height))));
    }

    private DigitalLifeHashArchiveEntity savedArchive(List<HashedEvent> chain) {
        DigitalLifeHashArchiveEntity archive = new DigitalLifeHashArchiveEntity();
        archive.setTaskId(9L);
        archive.setHashVersion(1);
        archive.setRootHash(chain.get(chain.size() - 1).eventHash());
        archive.setEventCount(chain.size());
        archive.setGeneratedTime(LocalDateTime.of(2026, 7, 15, 10, 0));
        return archive;
    }

    private List<DigitalLifeHashEventEntity> savedEvents(List<HashedEvent> chain) {
        List<DigitalLifeHashEventEntity> result = new ArrayList<>();
        for (HashedEvent event : chain) {
            DigitalLifeHashEventEntity entity = new DigitalLifeHashEventEntity();
            entity.setTaskId(9L);
            entity.setHashVersion(1);
            entity.setTargetType(event.event().targetType());
            entity.setTargetId(event.event().targetId());
            entity.setEventType(event.event().eventType());
            entity.setEventTime(event.event().eventTime());
            entity.setActorId(event.event().actorId());
            entity.setActorName(event.event().actorName());
            entity.setPayloadSnapshot(event.canonicalData());
            entity.setPreviousHash(event.previousHash());
            entity.setEventHash(event.eventHash());
            entity.setSequence(event.sequence());
            result.add(entity);
        }
        return result;
    }
}
