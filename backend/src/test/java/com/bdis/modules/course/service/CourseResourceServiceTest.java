package com.bdis.modules.course.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.service.AuditLogService;
import com.bdis.common.exception.BusinessException;
import com.bdis.file.service.FileBusinessService;
import com.bdis.modules.course.entity.CourseEntity;
import com.bdis.modules.course.entity.CourseResourceEntity;
import com.bdis.modules.course.mapper.CourseMapper;
import com.bdis.modules.course.mapper.CourseResourceMapper;
import com.bdis.modules.course.request.CourseResourceBindRequest;
import com.bdis.modules.course.service.impl.CourseResourceServiceImpl;
import com.bdis.modules.course.vo.CourseResourceVO;
import com.bdis.modules.file.entity.FileBusinessEntity;
import com.bdis.modules.file.entity.FileResourceEntity;
import com.bdis.modules.file.mapper.FileBusinessMapper;
import com.bdis.modules.file.mapper.FileResourceMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CourseResourceServiceTest {

    @Mock private CourseResourceMapper resourceMapper;

    @Mock private CourseMapper courseMapper;

    @Mock private FileResourceMapper fileResourceMapper;

    @Mock private FileBusinessMapper fileBusinessMapper;

    @Mock private FileBusinessService fileBusinessService;

    @Mock private AuditLogService auditLogService;

    private CourseResourceService resourceService;

    @BeforeEach
    void setUp() {
        resourceService =
                new CourseResourceServiceImpl(
                        resourceMapper,
                        courseMapper,
                        fileResourceMapper,
                        fileBusinessMapper,
                        fileBusinessService,
                        auditLogService);
    }

    @Test
    void bindResourceCallsM05BusinessBindingAndRecordsM12Audit() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(fileResourceMapper.selectById(31L)).thenReturn(activeFile());
        when(resourceMapper.selectOne(any())).thenReturn(null);
        when(resourceMapper.insert(any(CourseResourceEntity.class))).thenAnswer(invocation -> {
            CourseResourceEntity entity = invocation.getArgument(0);
            entity.setId(41L);
            return 1;
        });

        CourseResourceVO result = resourceService.bind(11L, bindRequest());

        verify(fileBusinessService).bind(any());
        ArgumentCaptor<AuditRecordDTO> auditCaptor = ArgumentCaptor.forClass(AuditRecordDTO.class);
        verify(auditLogService).record(auditCaptor.capture());
        assertThat(auditCaptor.getValue().getOperationType()).isEqualTo("BIND");
        assertThat(auditCaptor.getValue().getBizType()).isEqualTo("edu_course");
        assertThat(result.getId()).isEqualTo(41L);
        assertThat(result.getFileName()).isEqualTo("handout.pdf");
    }

    @Test
    void bindResourceRejectsInactiveFile() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        FileResourceEntity file = activeFile();
        file.setStatus(0);
        when(fileResourceMapper.selectById(31L)).thenReturn(file);

        assertThatThrownBy(() -> resourceService.bind(11L, bindRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("File not found or inactive");
    }

    @Test
    void bindResourceRejectsDuplicateFile() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(fileResourceMapper.selectById(31L)).thenReturn(activeFile());
        when(resourceMapper.selectOne(any())).thenReturn(activeResource());

        assertThatThrownBy(() -> resourceService.bind(11L, bindRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already bound");
    }

    @Test
    void unbindResourceUnbindsM05RelationWithoutDeletingFile() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(resourceMapper.selectById(41L)).thenReturn(activeResource());
        FileBusinessEntity relation = new FileBusinessEntity();
        relation.setId(51L);
        when(fileBusinessMapper.selectOne(any())).thenReturn(relation);
        when(resourceMapper.deleteById(41L)).thenReturn(1);

        resourceService.unbind(11L, 41L);

        verify(fileBusinessService).unbind(51L);
        verify(resourceMapper).deleteById(41L);
        verify(auditLogService).record(any(AuditRecordDTO.class));
    }

    @Test
    void publishedCourseCannotBindResource() {
        CourseEntity course = activeCourse();
        course.setPublishStatus("published");
        when(courseMapper.selectById(11L)).thenReturn(course);

        assertThatThrownBy(() -> resourceService.bind(11L, bindRequest()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Published course cannot be edited");
    }

    @Test
    void listResourcesUsesBatchFileLookup() {
        when(courseMapper.selectById(11L)).thenReturn(activeCourse());
        when(resourceMapper.selectList(any())).thenReturn(List.of(activeResource()));
        when(fileResourceMapper.selectBatchIds(any())).thenReturn(List.of(activeFile()));

        List<CourseResourceVO> result = resourceService.listByCourseId(11L, null);

        assertThat(result).singleElement().satisfies(vo -> {
            assertThat(vo.getFileId()).isEqualTo(31L);
            assertThat(vo.getFileName()).isEqualTo("handout.pdf");
        });
    }

    private CourseResourceBindRequest bindRequest() {
        CourseResourceBindRequest request = new CourseResourceBindRequest();
        request.setFileId(31L);
        request.setResourceName("Experiment handout");
        request.setResourceType("handout");
        return request;
    }

    private CourseEntity activeCourse() {
        CourseEntity entity = new CourseEntity();
        entity.setId(11L);
        entity.setPublishStatus("draft");
        entity.setStatus(1);
        entity.setIsDeleted(0);
        return entity;
    }

    private FileResourceEntity activeFile() {
        FileResourceEntity entity = new FileResourceEntity();
        entity.setId(31L);
        entity.setFileName("handout.pdf");
        entity.setFileUrl("/files/handout.pdf");
        entity.setStatus(1);
        entity.setIsDeleted(0);
        return entity;
    }

    private CourseResourceEntity activeResource() {
        CourseResourceEntity entity = new CourseResourceEntity();
        entity.setId(41L);
        entity.setCourseId(11L);
        entity.setFileId(31L);
        entity.setResourceName("Experiment handout");
        entity.setResourceType("handout");
        entity.setStatus(1);
        entity.setIsDeleted(0);
        return entity;
    }
}
