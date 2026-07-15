package com.bdis.modules.growth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bdis.common.exception.BusinessException;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.mapper.HerbCollectionTaskMapper;
import com.bdis.modules.growth.entity.DigitalLifeHashArchiveEntity;
import com.bdis.modules.growth.entity.DigitalLifeHashEventEntity;
import com.bdis.modules.growth.mapper.DigitalLifeHashArchiveMapper;
import com.bdis.modules.growth.mapper.DigitalLifeHashEventMapper;
import com.bdis.modules.growth.service.DigitalLifeIntegrityService;
import com.bdis.modules.growth.service.HerbDigitalLifeArchiveService;
import com.bdis.modules.growth.support.DigitalLifeHashChain;
import com.bdis.modules.growth.support.DigitalLifeHashChain.CanonicalEvent;
import com.bdis.modules.growth.support.DigitalLifeHashChain.ChainVerification;
import com.bdis.modules.growth.support.DigitalLifeHashChain.HashedEvent;
import com.bdis.modules.growth.support.DigitalLifeIntegrityEventCollector;
import com.bdis.modules.growth.vo.DigitalLifeIntegrityVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeArchiveVO;
import com.bdis.modules.growth.vo.HerbDigitalLifeStageVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DigitalLifeIntegrityServiceImpl implements DigitalLifeIntegrityService {

    private final HerbDigitalLifeArchiveService archiveService;
    private final HerbCollectionTaskMapper taskMapper;
    private final DigitalLifeIntegrityEventCollector eventCollector;
    private final DigitalLifeHashChain hashChain;
    private final DigitalLifeHashArchiveMapper archiveMapper;
    private final DigitalLifeHashEventMapper eventMapper;

    public DigitalLifeIntegrityServiceImpl(
            HerbDigitalLifeArchiveService archiveService,
            HerbCollectionTaskMapper taskMapper,
            DigitalLifeIntegrityEventCollector eventCollector,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper,
            DigitalLifeHashArchiveMapper archiveMapper,
            DigitalLifeHashEventMapper eventMapper) {
        this.archiveService = archiveService;
        this.taskMapper = taskMapper;
        this.eventCollector = eventCollector;
        this.hashChain = new DigitalLifeHashChain(objectMapper);
        this.archiveMapper = archiveMapper;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public DigitalLifeIntegrityVO generate(Long taskId) {
        CurrentUser currentUser = requireManagementRole();
        HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(taskId);
        requireApprovedArchive(archive);
        if (taskMapper.selectByIdForUpdate(taskId) == null) {
            throw new BusinessException("采集任务不存在或已删除");
        }
        List<HashedEvent> chain = hashChain.build(eventCollector.collect(archive));
        if (chain.isEmpty()) {
            throw new BusinessException("档案没有可纳入证据链的真实业务事件");
        }

        DigitalLifeHashArchiveEntity latest = latestArchive(taskId);
        int version = latest == null ? 1 : latest.getHashVersion() + 1;
        LocalDateTime now = LocalDateTime.now();
        String rootHash = chain.get(chain.size() - 1).eventHash();
        DigitalLifeHashArchiveEntity integrityArchive = new DigitalLifeHashArchiveEntity();
        integrityArchive.setTaskId(taskId);
        integrityArchive.setHashVersion(version);
        integrityArchive.setRootHash(rootHash);
        integrityArchive.setEventCount(chain.size());
        integrityArchive.setGeneratedTime(now);
        integrityArchive.setGeneratedBy(currentUser.getUserId());
        integrityArchive.setIntegrityStatus("generated");
        initializeEntity(integrityArchive, currentUser.getUserId(), now);
        archiveMapper.insert(integrityArchive);

        List<DigitalLifeHashEventEntity> entities =
                chain.stream()
                        .map(
                                event ->
                                        toEntity(
                                                taskId,
                                                version,
                                                event,
                                                currentUser.getUserId(),
                                                now))
                        .toList();
        eventMapper.insert(entities);
        return response(true, chain.size(), rootHash, version, now, null, null, "防篡改数字档案证据链已生成");
    }

    @Override
    public DigitalLifeIntegrityVO verify(Long taskId) {
        HerbDigitalLifeArchiveVO archive = archiveService.getByTaskId(taskId);
        return verifyArchive(taskId, archive);
    }

    @Override
    public DigitalLifeIntegrityVO verifyPublic(String traceCode) {
        archiveService.publicArchive(traceCode);
        HerbCollectionTaskEntity task = taskMapper.selectByTraceCode(traceCode);
        HerbDigitalLifeArchiveVO archive = new HerbDigitalLifeArchiveVO();
        archive.setTaskId(task.getId());
        return verifyArchive(task.getId(), archive);
    }

    private DigitalLifeIntegrityVO verifyArchive(Long taskId, HerbDigitalLifeArchiveVO archive) {
        DigitalLifeHashArchiveEntity latest = latestArchive(taskId);
        if (latest == null) {
            return new DigitalLifeIntegrityVO(
                    false,
                    0,
                    null,
                    DigitalLifeHashChain.HASH_VERSION,
                    null,
                    null,
                    null,
                    "该档案尚未生成哈希证据链");
        }
        List<DigitalLifeHashEventEntity> stored =
                eventMapper.selectList(
                        new LambdaQueryWrapper<DigitalLifeHashEventEntity>()
                                .eq(DigitalLifeHashEventEntity::getTaskId, taskId)
                                .eq(
                                        DigitalLifeHashEventEntity::getHashVersion,
                                        latest.getHashVersion())
                                .orderByAsc(DigitalLifeHashEventEntity::getSequence));
        List<HashedEvent> storedChain = stored.stream().map(this::fromEntity).toList();
        ChainVerification storedVerification = hashChain.verifyStored(storedChain);
        if (!storedVerification.verified()) {
            return failed(
                    latest,
                    storedVerification.failedSequence(),
                    storedVerification.failedEventType(),
                    "证据链存储校验失败");
        }

        List<HashedEvent> current = hashChain.build(eventCollector.collect(archive));
        if (stored.size() != current.size()) {
            return failed(
                    latest, firstDifference(stored.size(), current.size()), null, "关键事件数量已变化");
        }
        for (int index = 0; index < current.size(); index++) {
            DigitalLifeHashEventEntity saved = stored.get(index);
            HashedEvent actual = current.get(index);
            if (!sameEvent(saved, actual)) {
                return failed(latest, index + 1, actual.event().eventType(), "档案关键事件数据已发生变化");
            }
        }
        String currentRoot = current.isEmpty() ? null : current.get(current.size() - 1).eventHash();
        if (!Objects.equals(latest.getRootHash(), currentRoot)) {
            return failed(latest, current.size(), null, "档案根哈希不一致");
        }
        return response(
                true,
                latest.getEventCount(),
                latest.getRootHash(),
                latest.getHashVersion(),
                latest.getGeneratedTime(),
                null,
                null,
                "防篡改档案校验通过");
    }

    private boolean sameEvent(DigitalLifeHashEventEntity saved, HashedEvent actual) {
        CanonicalEvent event = actual.event();
        return Objects.equals(saved.getSequence(), actual.sequence())
                && Objects.equals(saved.getTargetType(), event.targetType())
                && Objects.equals(saved.getTargetId(), event.targetId())
                && Objects.equals(saved.getEventType(), event.eventType())
                && Objects.equals(saved.getEventTime(), event.eventTime())
                && Objects.equals(saved.getPayloadSnapshot(), actual.canonicalData())
                && Objects.equals(saved.getPreviousHash(), actual.previousHash())
                && Objects.equals(saved.getEventHash(), actual.eventHash());
    }

    private DigitalLifeHashArchiveEntity latestArchive(Long taskId) {
        return archiveMapper.selectOne(
                new LambdaQueryWrapper<DigitalLifeHashArchiveEntity>()
                        .eq(DigitalLifeHashArchiveEntity::getTaskId, taskId)
                        .orderByDesc(DigitalLifeHashArchiveEntity::getHashVersion)
                        .last("LIMIT 1"));
    }

    private CurrentUser requireManagementRole() {
        CurrentUser user = SecurityUtils.currentUser();
        if (user.getRoleCodes().stream()
                .noneMatch(role -> Set.of("ADMIN", "TEACHER", "REVIEWER").contains(role))) {
            throw new ForbiddenException("仅管理端角色可以生成数字生命档案证据链");
        }
        return user;
    }

    private void requireApprovedArchive(HerbDigitalLifeArchiveVO archive) {
        if (archive.getStages().isEmpty()
                || archive.getStages().stream().anyMatch(this::isNotApprovedStage)) {
            throw new BusinessException("仅全部阶段审核通过的数字生命档案可以生成证据链");
        }
    }

    private boolean isNotApprovedStage(HerbDigitalLifeStageVO stage) {
        return stage.getGrowthRecordId() == null
                || !("approved".equals(stage.getAuditStatus())
                        || "已通过".equals(stage.getAuditStatus()));
    }

    private DigitalLifeHashEventEntity toEntity(
            Long taskId, int hashVersion, HashedEvent event, Long userId, LocalDateTime now) {
        DigitalLifeHashEventEntity entity = new DigitalLifeHashEventEntity();
        entity.setTaskId(taskId);
        entity.setHashVersion(hashVersion);
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
        initializeEntity(entity, userId, now);
        return entity;
    }

    private HashedEvent fromEntity(DigitalLifeHashEventEntity entity) {
        CanonicalEvent event =
                new CanonicalEvent(
                        entity.getTargetType(),
                        entity.getTargetId(),
                        entity.getEventType(),
                        entity.getEventTime(),
                        entity.getActorId(),
                        entity.getActorName(),
                        Map.of());
        return new HashedEvent(
                entity.getSequence(),
                event,
                entity.getPayloadSnapshot(),
                entity.getPreviousHash(),
                entity.getEventHash());
    }

    private void initializeEntity(
            com.bdis.common.core.BaseEntity entity, Long userId, LocalDateTime now) {
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setVersion(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
    }

    private int firstDifference(int storedSize, int currentSize) {
        return Math.min(storedSize, currentSize) + 1;
    }

    private DigitalLifeIntegrityVO failed(
            DigitalLifeHashArchiveEntity archive,
            Integer sequence,
            String eventType,
            String message) {
        return response(
                false,
                archive.getEventCount(),
                archive.getRootHash(),
                archive.getHashVersion(),
                archive.getGeneratedTime(),
                sequence,
                eventType,
                message);
    }

    private DigitalLifeIntegrityVO response(
            boolean verified,
            int eventCount,
            String rootHash,
            int archiveVersion,
            LocalDateTime generatedTime,
            Integer failedSequence,
            String failedEventType,
            String message) {
        return new DigitalLifeIntegrityVO(
                verified,
                eventCount,
                rootHash,
                DigitalLifeHashChain.HASH_VERSION + ":" + archiveVersion,
                generatedTime,
                failedSequence,
                failedEventType,
                message);
    }
}
