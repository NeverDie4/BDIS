package com.bdis.modules.training.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.modules.course.entity.CourseResourceEntity;
import com.bdis.modules.course.mapper.CourseResourceMapper;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.training.entity.TrainingMaterialEntity;
import com.bdis.modules.training.mapper.TrainingMaterialMapper;
import com.bdis.modules.training.mapper.TrainingPlanMaterialMapper;
import com.bdis.modules.training.query.TrainingMaterialQuery;
import com.bdis.modules.training.request.TrainingMaterialCreateRequest;
import com.bdis.modules.training.request.TrainingMaterialUpdateRequest;
import com.bdis.modules.training.service.impl.TrainingMaterialServiceImpl;
import com.bdis.modules.user.entity.UserEntity;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.List;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class TrainingMaterialServiceTest {
    @Mock TrainingMaterialMapper materialMapper;
    @Mock TrainingPlanMaterialMapper relationMapper;
    @Mock FileResourceMapper fileMapper;
    @Mock FileAccessGuard fileAccessGuard;
    @Mock CourseResourceMapper courseResourceMapper;
    @Mock UserMapper userMapper;
    @Mock AuditLogService auditLogService;
    TrainingMaterialService service;

    @BeforeEach
    void setUp() {
        service =
                new TrainingMaterialServiceImpl(
                        materialMapper,
                        relationMapper,
                        fileMapper,
                        courseResourceMapper,
                        userMapper,
                        fileAccessGuard,
                        auditLogService);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("7", "n/a"));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void pageUsesBatchLookupsAndSupportsFilters() {
        TrainingMaterialEntity entity = material(1L);
        when(materialMapper.selectPage(any(), any()))
                .thenAnswer(
                        inv -> {
                            Page<TrainingMaterialEntity> page = inv.getArgument(0);
                            page.setRecords(List.of(entity));
                            page.setTotal(1);
                            return page;
                        });
        when(fileMapper.selectBatchIds(any())).thenReturn(List.of(file(10L, 1)));
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(user(7L)));
        TrainingMaterialQuery query = new TrainingMaterialQuery();
        query.setKeyword("MAT");
        query.setMaterialType("video");
        query.setSourceType("upload");
        query.setUploaderId(7L);
        query.setStatus(1);
        query.setSortField("materialName");
        query.setSortOrder("asc");
        var result = service.page(query);
        assertEquals(1, result.getTotal());
        assertEquals("file.pdf", result.getRecords().getFirst().getFileName());
        verify(fileMapper, times(1)).selectBatchIds(any());
        verify(userMapper, times(1)).selectBatchIds(any());
    }

    @Test
    void pageRejectsInvalidTypeSourceAndSort() {
        TrainingMaterialQuery q = new TrainingMaterialQuery();
        q.setMaterialType("bad");
        assertThrows(BusinessException.class, () -> service.page(q));
        q.setMaterialType("video");
        q.setSourceType("bad");
        assertThrows(BusinessException.class, () -> service.page(q));
        q.setSourceType("upload");
        q.setSortField("drop table");
        assertThrows(BusinessException.class, () -> service.page(q));
    }

    @Test
    void detailAggregatesFileUploaderAndPlanCount() {
        when(materialMapper.selectByIdIncludingDeleted(1L)).thenReturn(material(1L));
        when(fileMapper.selectById(10L)).thenReturn(file(10L, 1));
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(relationMapper.countActivePlanBindings(1L)).thenReturn(2L);
        var detail = service.getDetail(1L);
        assertEquals(2, detail.getPlanCount());
        assertEquals("file.pdf", detail.getFileName());
        assertEquals("User 7", detail.getUploaderName());
    }

    @Test
    void detailRejectsMissingOrDeleted() {
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getDetail(1L)).getResultCode());
        TrainingMaterialEntity deleted = material(1L);
        deleted.setIsDeleted(1);
        when(materialMapper.selectByIdIncludingDeleted(1L)).thenReturn(deleted);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.getDetail(1L)).getResultCode());
    }

    @Test
    void createDefaultsMetadataAndAudits() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(fileMapper.selectById(10L)).thenReturn(file(10L, 1));
        when(materialMapper.insert(any(TrainingMaterialEntity.class)))
                .thenAnswer(
                        inv -> {
                            TrainingMaterialEntity e = inv.getArgument(0);
                            e.setId(11L);
                            return 1;
                        });
        assertEquals(11L, service.create(create("upload", null)));
        ArgumentCaptor<TrainingMaterialEntity> c =
                ArgumentCaptor.forClass(TrainingMaterialEntity.class);
        verify(materialMapper).insert(c.capture());
        assertEquals(0, c.getValue().getReuseCount());
        assertEquals(1, c.getValue().getStatus());
        assertEquals(7L, c.getValue().getUploaderId());
        verify(auditLogService).record(any());
    }

    @Test
    void createEnforcesPermanentMaterialNumber() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        TrainingMaterialEntity deleted = material(99L);
        deleted.setIsDeleted(1);
        when(materialMapper.selectByMaterialNoIncludingDeleted("MAT-1")).thenReturn(deleted);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.create(create("upload", null)))
                        .getResultCode());
        verify(fileMapper, never()).selectById(anyLong());
    }

    @Test
    void createRejectsInvalidTypeSourceAndFile() {
        TrainingMaterialCreateRequest r = create("upload", null);
        r.setMaterialType("bad");
        assertThrows(BusinessException.class, () -> service.create(r));
        r.setMaterialType("video");
        r.setSourceType("bad");
        assertThrows(BusinessException.class, () -> service.create(r));
        r.setSourceType("upload");
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.create(r)).getResultCode());
        FileResourceEntity deleted = file(10L, 1);
        deleted.setIsDeleted(1);
        when(fileMapper.selectById(10L)).thenReturn(deleted);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.create(r)).getResultCode());
        FileResourceEntity inactive = file(10L, 0);
        when(fileMapper.selectById(10L)).thenReturn(inactive);
        assertEquals(
                ResultCodeEnum.NOT_FOUND,
                assertThrows(BusinessException.class, () -> service.create(r)).getResultCode());
    }

    @Test
    void courseResourceSourceRequiresMatchingActiveResourceAndFile() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(fileMapper.selectById(10L)).thenReturn(file(10L, 1));
        CourseResourceEntity resource = new CourseResourceEntity();
        resource.setId(20L);
        resource.setFileId(10L);
        resource.setStatus(1);
        resource.setIsDeleted(0);
        when(courseResourceMapper.selectById(20L)).thenReturn(resource);
        when(materialMapper.insert(any(TrainingMaterialEntity.class)))
                .thenAnswer(
                        inv -> {
                            ((TrainingMaterialEntity) inv.getArgument(0)).setId(1L);
                            return 1;
                        });
        assertEquals(1L, service.create(create("course_resource", 20L)));
        resource.setFileId(99L);
        assertThrows(BusinessException.class, () -> service.create(create("course_resource", 20L)));
    }

    @Test
    void uploadAndGeneratedRejectSourceResourceButCourseResourceRequiresIt() {
        TrainingMaterialCreateRequest upload = create("upload", 20L);
        assertThrows(BusinessException.class, () -> service.create(upload));
        TrainingMaterialCreateRequest generated = create("generated", 20L);
        assertThrows(BusinessException.class, () -> service.create(generated));
        TrainingMaterialCreateRequest course = create("course_resource", null);
        assertThrows(BusinessException.class, () -> service.create(course));
    }

    @Test
    void updateUsesVersionAllowsDisableAndRejectsImmutableNumber() {
        TrainingMaterialEntity entity = material(1L);
        when(materialMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(fileMapper.selectById(10L)).thenReturn(file(10L, 1));
        when(materialMapper.updateById(any(TrainingMaterialEntity.class))).thenReturn(1);
        TrainingMaterialUpdateRequest r = update();
        r.setStatus(0);
        service.update(1L, r);
        verify(materialMapper).updateById(any(TrainingMaterialEntity.class));
        verify(auditLogService).record(any());
        r.setMaterialNo("OTHER");
        assertThrows(BusinessException.class, () -> service.update(1L, r));
    }

    @Test
    void updateRejectsVersionConflictAndZeroAffectedRows() {
        TrainingMaterialEntity entity = material(1L);
        when(materialMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        TrainingMaterialUpdateRequest r = update();
        r.setVersion(9);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, r)).getResultCode());
        r.setVersion(0);
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(fileMapper.selectById(10L)).thenReturn(file(10L, 1));
        when(materialMapper.updateById(any(TrainingMaterialEntity.class))).thenReturn(0);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.update(1L, r)).getResultCode());
    }

    @Test
    void deleteUnreferencedUsesLogicalDeleteAndNeverDeletesFile() {
        TrainingMaterialEntity entity = material(1L);
        when(materialMapper.selectByIdIncludingDeleted(1L)).thenReturn(entity);
        when(relationMapper.countActivePlanBindings(1L)).thenReturn(0L);
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(materialMapper.logicalDelete(eq(1L), eq(0), any(), eq(7L))).thenReturn(1);
        service.delete(1L);
        verify(materialMapper).logicalDelete(eq(1L), eq(0), any(), eq(7L));
        verifyNoMoreInteractions(fileMapper);
        verify(auditLogService).record(any());
    }

    @Test
    void deleteReferencedMaterialReturnsConflict() {
        when(materialMapper.selectByIdIncludingDeleted(1L)).thenReturn(material(1L));
        when(relationMapper.countActivePlanBindings(1L)).thenReturn(1L);
        assertEquals(
                ResultCodeEnum.CONFLICT,
                assertThrows(BusinessException.class, () -> service.delete(1L)).getResultCode());
        verify(materialMapper, never()).logicalDelete(anyLong(), anyInt(), any(), anyLong());
    }

    @Test
    void auditFailurePropagates() {
        when(userMapper.selectById(7L)).thenReturn(user(7L));
        when(fileMapper.selectById(10L)).thenReturn(file(10L, 1));
        when(materialMapper.insert(any(TrainingMaterialEntity.class)))
                .thenAnswer(
                        inv -> {
                            ((TrainingMaterialEntity) inv.getArgument(0)).setId(1L);
                            return 1;
                        });
        doThrow(new IllegalStateException("audit")).when(auditLogService).record(any());
        assertThrows(IllegalStateException.class, () -> service.create(create("upload", null)));
    }

    private TrainingMaterialCreateRequest create(String source, Long sourceId) {
        TrainingMaterialCreateRequest r = new TrainingMaterialCreateRequest();
        r.setMaterialNo("MAT-1");
        r.setMaterialName("Material");
        r.setMaterialType("video");
        r.setFileId(10L);
        r.setSourceType(source);
        r.setSourceResourceId(sourceId);
        return r;
    }

    private TrainingMaterialUpdateRequest update() {
        TrainingMaterialUpdateRequest r = new TrainingMaterialUpdateRequest();
        r.setMaterialName("Updated");
        r.setMaterialType("video");
        r.setFileId(10L);
        r.setSourceType("upload");
        r.setStatus(1);
        r.setVersion(0);
        return r;
    }

    private TrainingMaterialEntity material(Long id) {
        TrainingMaterialEntity e = new TrainingMaterialEntity();
        e.setId(id);
        e.setMaterialNo("MAT-1");
        e.setMaterialName("Material");
        e.setMaterialType("video");
        e.setFileId(10L);
        e.setSourceType("upload");
        e.setUploaderId(7L);
        e.setReuseCount(0);
        e.setStatus(1);
        e.setIsDeleted(0);
        e.setVersion(0);
        return e;
    }

    private FileResourceEntity file(Long id, int status) {
        FileResourceEntity f = new FileResourceEntity();
        f.setId(id);
        f.setFileName("file.pdf");
        f.setOriginalFilename("original.pdf");
        f.setStatus(status);
        f.setIsDeleted(0);
        return f;
    }

    private UserEntity user(Long id) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setRealName("User " + id);
        u.setStatus(1);
        u.setIsDeleted(0);
        return u;
    }
}
