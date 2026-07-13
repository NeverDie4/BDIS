package com.bdis.modules.training.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.training.constant.TrainingPublishStatus;
import com.bdis.modules.training.entity.TrainingPlanEntity;
import com.bdis.modules.training.mapper.TrainingPlanMapper;
import com.bdis.modules.training.query.TrainingPlanQuery;
import com.bdis.modules.training.request.TrainingPlanCreateRequest;
import com.bdis.modules.training.request.TrainingPlanPublishRequest;
import com.bdis.modules.training.request.TrainingPlanCloseRequest;
import com.bdis.modules.training.request.TrainingPlanUpdateRequest;
import com.bdis.modules.training.service.impl.TrainingPlanServiceImpl;
import com.bdis.modules.training.vo.TrainingPlanMaterialVO;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TrainingPlanServiceTest {
    @Mock TrainingPlanMapper planMapper;
    @Mock UserMapper userMapper;
    @Mock CourseMapper courseMapper;
    @Mock TrainingPlanMaterialService planMaterialService;
    @Mock AuditLogService auditLogService;
    TrainingPlanService service;

    @BeforeEach
    void setUp() {
        service = new TrainingPlanServiceImpl(
                planMapper, userMapper, courseMapper, planMaterialService, auditLogService);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("7", "n/a"));
    }

    @AfterEach void clear() { SecurityContextHolder.clearContext(); }

    @Test
    void pageForwardsFiltersAndReturnsSummaryWithoutNPlusOne() {
        TrainingPlanEntity plan = plan(1L, TrainingPublishStatus.DRAFT);
        plan.setOwnerId(8L);
        plan.setCourseId(9L);
        when(planMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<TrainingPlanEntity> page = invocation.getArgument(0);
            page.setRecords(List.of(plan));
            page.setTotal(1);
            return page;
        });
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(8L)));
        CourseEntity course = new CourseEntity();
        course.setId(9L);
        course.setCourseName("Course");
        when(courseMapper.selectBatchIds(any())).thenReturn(List.of(course));
        TrainingPlanQuery query = new TrainingPlanQuery();
        query.setKeyword("IT");
        query.setPlanType("course");

        var result = service.page(query);

        assertEquals(1, result.getTotal());
        assertEquals("Course", result.getRecords().getFirst().getCourseName());
        verify(userMapper, times(1)).selectBatchIds(any());
        verify(courseMapper, times(1)).selectBatchIds(any());
    }

    @Test
    void pageRejectsInvalidTypeAndTimeRange() {
        TrainingPlanQuery type = new TrainingPlanQuery();
        type.setPlanType("bad");
        assertThrows(BusinessException.class, () -> service.page(type));
        TrainingPlanQuery range = new TrainingPlanQuery();
        range.setStartedFrom(LocalDateTime.now());
        range.setStartedTo(LocalDateTime.now().minusDays(1));
        assertThrows(BusinessException.class, () -> service.page(range));
    }

    @Test
    void detailReturnsParticipantCountAndRejectsDeleted() {
        TrainingPlanEntity entity = plan(1L, "draft");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        when(planMapper.countRecords(1L)).thenReturn(3L);
        var detail = service.getDetail(1L);
        assertEquals(3, detail.getParticipantCount());

        entity.setIsDeleted(1);
        assertEquals(ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getDetail(1L)).getResultCode());
    }

    @Test
    void detailAggregatesMaterialsAndRequiredCounts() {
        TrainingPlanEntity entity = plan(1L, "draft");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        TrainingPlanMaterialVO required = new TrainingPlanMaterialVO();
        required.setIsRequired(1);
        TrainingPlanMaterialVO optional = new TrainingPlanMaterialVO();
        optional.setIsRequired(0);
        when(planMaterialService.list(1L)).thenReturn(List.of(required, optional));

        var detail = service.getDetail(1L);

        assertEquals(2, detail.getMaterialCount());
        assertEquals(1, detail.getRequiredMaterialCount());
        assertEquals(2, detail.getMaterials().size());
    }

    @Test
    void createUsesPermanentNumberDefaultsAndAudits() {
        TrainingPlanCreateRequest request = createRequest();
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(userMapper.selectById(8L)).thenReturn(user(8L));
        when(courseMapper.selectById(9L)).thenReturn(course(9L));
        when(planMapper.insert(any(TrainingPlanEntity.class))).thenAnswer(invocation -> {
            TrainingPlanEntity entity = invocation.getArgument(0);
            entity.setId(11L);
            return 1;
        });

        assertEquals(11L, service.create(request));
        ArgumentCaptor<TrainingPlanEntity> captor = ArgumentCaptor.forClass(TrainingPlanEntity.class);
        verify(planMapper).insert(captor.capture());
        assertEquals("draft", captor.getValue().getPublishStatus());
        assertEquals(7L, captor.getValue().getCreatedBy());
        assertEquals(0, captor.getValue().getVersion());
        verify(planMapper).selectByPlanNoIncludingDeleted("IT-PLAN");
        verify(auditLogService).record(any());
    }

    @Test
    void createRejectsDeletedHistoricalNumberInvalidTypeAndTime() {
        TrainingPlanCreateRequest request = createRequest();
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.selectByPlanNoIncludingDeleted("IT-PLAN")).thenReturn(plan(99L, "draft"));
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(request)).getResultCode());

        request.setPlanNo("OTHER");
        request.setPlanType("bad");
        assertThrows(BusinessException.class, () -> service.create(request));
        request.setPlanType("course");
        request.setStartedAt(LocalDateTime.now());
        request.setEndedAt(request.getStartedAt());
        assertThrows(BusinessException.class, () -> service.create(request));
    }

    @Test
    void createRejectsInactiveOperatorOwnerAndCourse() {
        TrainingPlanCreateRequest request = createRequest();
        assertThrows(BusinessException.class, () -> service.create(request));
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(userMapper.selectById(8L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.create(request));
        when(userMapper.selectById(8L)).thenReturn(user(8L));
        when(courseMapper.selectById(9L)).thenReturn(null);
        assertThrows(BusinessException.class, () -> service.create(request));
    }

    @Test
    void updateDraftUsesVersionAndAuditFailurePropagates() {
        TrainingPlanEntity entity = plan(1L, "draft");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(userMapper.selectById(8L)).thenReturn(user(8L));
        when(courseMapper.selectById(9L)).thenReturn(course(9L));
        when(planMapper.updateById(any(TrainingPlanEntity.class))).thenReturn(1);
        TrainingPlanUpdateRequest request = updateRequest();

        service.update(1L, request);
        verify(planMapper).updateById(any(TrainingPlanEntity.class));
        verify(auditLogService).record(any());

        doThrow(new IllegalStateException("audit")).when(auditLogService).record(any());
        assertThrows(IllegalStateException.class, () -> service.update(1L, request));
    }

    @Test
    void updateRejectsPublishedAndVersionConflict() {
        TrainingPlanEntity entity = plan(1L, "published");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, updateRequest())).getResultCode());
        entity.setPublishStatus("draft");
        TrainingPlanUpdateRequest request = updateRequest();
        request.setVersion(2);
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, request)).getResultCode());
    }

    @Test
    void deleteDraftUsesAtomicLogicalDeleteAndBlocksParticipants() {
        TrainingPlanEntity entity = plan(1L, "draft");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        when(planMapper.countRecords(1L)).thenReturn(1L);
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.delete(1L)).getResultCode());

        when(planMapper.countRecords(1L)).thenReturn(0L);
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.logicalDelete(eq(1L), eq(0), any(), eq(7L))).thenReturn(1);
        service.delete(1L);
        verify(auditLogService).record(any());
    }

    @Test
    void deleteDraftBlocksExistingMaterialBindingsToPreserveReuseCount() {
        TrainingPlanEntity entity = plan(1L, "draft");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        when(planMapper.countRecords(1L)).thenReturn(0L);
        when(planMapper.countMaterials(1L)).thenReturn(1L);

        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.delete(1L)).getResultCode());

        verify(planMapper, never()).logicalDelete(anyLong(), anyInt(), any(), anyLong());
    }

    @Test
    void publishRequiresCourseOrMaterialAndUsesAtomicVersionUpdate() {
        TrainingPlanEntity entity = plan(1L, "draft");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        TrainingPlanPublishRequest request = new TrainingPlanPublishRequest();
        request.setVersion(0);
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.publish(1L, request)).getResultCode());

        entity.setCourseId(9L);
        when(courseMapper.selectById(9L)).thenReturn(course(9L));
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.publish(eq(1L), eq(0), any(), eq(7L))).thenReturn(1);
        service.publish(1L, request);
        verify(auditLogService).record(any());
    }

    @Test
    void publishWithoutCourseAcceptsOnlyValidMaterialBindings() {
        TrainingPlanEntity entity = plan(1L, "draft");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        TrainingPlanPublishRequest request = new TrainingPlanPublishRequest();
        request.setVersion(0);
        when(planMaterialService.hasValidMaterial(1L)).thenReturn(true);
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.publish(eq(1L), eq(0), any(), eq(7L))).thenReturn(1);

        service.publish(1L, request);

        verify(planMaterialService).hasValidMaterial(1L);
    }

    @Test
    void publishWithDeletedOrInactiveCourseDoesNotBypassMaterialRequirement() {
        TrainingPlanEntity entity = plan(1L, "draft");
        entity.setCourseId(9L);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        TrainingPlanPublishRequest request = new TrainingPlanPublishRequest();
        request.setVersion(0);
        when(courseMapper.selectById(9L)).thenReturn(null);

        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.publish(1L, request))
                        .getResultCode());
        verify(planMapper, never()).publish(anyLong(), anyInt(), any(), anyLong());
    }

    @Test
    void publishRejectsRepeatClosedAndZeroAffectedRows() {
        TrainingPlanEntity entity = plan(1L, "published");
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        TrainingPlanPublishRequest request = new TrainingPlanPublishRequest();
        request.setVersion(0);
        assertThrows(BusinessException.class, () -> service.publish(1L, request));
        entity.setPublishStatus("closed");
        assertThrows(BusinessException.class, () -> service.publish(1L, request));

        entity.setPublishStatus("draft");
        entity.setCourseId(9L);
        when(courseMapper.selectById(9L)).thenReturn(course(9L));
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.publish(anyLong(), anyInt(), any(), anyLong())).thenReturn(0);
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.publish(1L, request)).getResultCode());
    }

    @Test
    void closeUsesPublishedVersionConditionPreservesPublicationAndAudits() {
        TrainingPlanEntity entity = plan(1L, "published");
        LocalDateTime publishedAt = LocalDateTime.of(2026, 7, 1, 8, 0);
        entity.setPublishedAt(publishedAt);
        entity.setPublishedBy(6L);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.close(eq(1L), eq(0), any(), eq(7L))).thenReturn(1);
        TrainingPlanCloseRequest request = new TrainingPlanCloseRequest();
        request.setReason("training finished");
        request.setVersion(0);

        service.close(1L, request);

        verify(planMapper).close(eq(1L), eq(0), any(), eq(7L));
        assertEquals(publishedAt, entity.getPublishedAt());
        assertEquals(6L, entity.getPublishedBy());
        verify(auditLogService).record(any());
    }

    @Test
    void closeRejectsDraftClosedMissingReasonVersionAndZeroAffectedRows() {
        TrainingPlanCloseRequest request = new TrainingPlanCloseRequest();
        request.setReason("done");
        request.setVersion(0);
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan(1L, "draft"));
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.close(1L, request)).getResultCode());
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan(1L, "closed"));
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.close(1L, request)).getResultCode());
        when(planMapper.selectByIdIncludingDeleted(1L)).thenReturn(plan(1L, "published"));
        request.setReason(" ");
        assertThrows(BusinessException.class, () -> service.close(1L, request));
        request.setReason("done");
        request.setVersion(null);
        assertThrows(BusinessException.class, () -> service.close(1L, request));
        request.setVersion(0);
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(planMapper.close(eq(1L), eq(0), any(), eq(7L))).thenReturn(0);
        assertEquals(ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.close(1L, request)).getResultCode());
    }

    private TrainingPlanCreateRequest createRequest() {
        TrainingPlanCreateRequest r = new TrainingPlanCreateRequest();
        r.setPlanNo("IT-PLAN");
        r.setPlanName("Plan");
        r.setPlanType("course");
        r.setOwnerId(8L);
        r.setCourseId(9L);
        r.setStartedAt(LocalDateTime.now().plusDays(1));
        r.setEndedAt(LocalDateTime.now().plusDays(2));
        return r;
    }

    private TrainingPlanUpdateRequest updateRequest() {
        TrainingPlanUpdateRequest r = new TrainingPlanUpdateRequest();
        r.setPlanName("Updated");
        r.setPlanType("course");
        r.setOwnerId(8L);
        r.setCourseId(9L);
        r.setStartedAt(LocalDateTime.now().plusDays(1));
        r.setEndedAt(LocalDateTime.now().plusDays(2));
        r.setVersion(0);
        return r;
    }

    private TrainingPlanEntity plan(Long id, String status) {
        TrainingPlanEntity entity = new TrainingPlanEntity();
        entity.setId(id);
        entity.setPlanNo("IT-PLAN");
        entity.setPlanName("Plan");
        entity.setPlanType("course");
        entity.setPublishStatus(status);
        entity.setStatus(1);
        entity.setIsDeleted(0);
        entity.setVersion(0);
        return entity;
    }

    private UserEntity user(Long id) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setStatus(1);
        user.setIsDeleted(0);
        user.setRealName("User " + id);
        return user;
    }

    private CourseEntity course(Long id) {
        CourseEntity course = new CourseEntity();
        course.setId(id);
        course.setStatus(1);
        course.setIsDeleted(0);
        return course;
    }
}

