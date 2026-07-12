package com.bdis.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bdis.audit.service.AuditLogService;
import com.bdis.audit.service.FileAccessLogService;
import com.bdis.common.core.PageResult;
import com.bdis.file.query.FileResourceQuery;
import com.bdis.file.service.FileBusinessService;
import com.bdis.file.service.FileResourceService;
import com.bdis.file.service.FileStorageService;
import com.bdis.file.service.impl.FileResourceServiceImpl;
import com.bdis.file.support.FileAccessGuard;
import com.bdis.modules.file.mapper.FileResourceMapper;
import com.bdis.modules.file.vo.FileResourceVO;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FileResourceServiceImplTest {

    @Mock private FileResourceMapper fileResourceMapper;

    @Mock private FileStorageService fileStorageService;

    @Mock private FileAccessLogService fileAccessLogService;

    @Mock private AuditLogService auditLogService;

    @Mock private FileBusinessService fileBusinessService;

    @Mock private FileAccessGuard fileAccessGuard;

    private FileResourceService fileResourceService;

    @BeforeEach
    void setUp() {
        fileResourceService =
                new FileResourceServiceImpl(
                        fileResourceMapper,
                        fileStorageService,
                        fileAccessLogService,
                        auditLogService,
                        fileBusinessService,
                        fileAccessGuard);
    }

    @Test
    void pageByBusinessReturnsOnlyRequestedSlice() {
        when(fileBusinessService.listByBusiness("herb_image", 9L))
                .thenReturn(List.of(file(1L), file(2L), file(3L), file(4L), file(5L)));
        FileResourceQuery query = new FileResourceQuery();
        query.setBizType("herb_image");
        query.setBizId(9L);
        query.setPage(2);
        query.setSize(2);

        PageResult<FileResourceVO> result = fileResourceService.page(query);

        assertThat(result.getRecords()).extracting(FileResourceVO::getId).containsExactly(3L, 4L);
        assertThat(result.getPage()).isEqualTo(2);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotal()).isEqualTo(5);
    }

    private FileResourceVO file(Long id) {
        FileResourceVO file = new FileResourceVO();
        file.setId(id);
        return file;
    }
}
