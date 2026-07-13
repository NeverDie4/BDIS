package com.bdis.modules.course.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.core.PageResult;
import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.exception.GlobalExceptionHandler;
import com.bdis.modules.course.service.CourseService;
import com.bdis.modules.course.vo.CourseDetailVO;
import com.bdis.modules.course.vo.CourseListVO;
import com.bdis.modules.permission.service.AuthorizationService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    private MockMvc mockMvc;

    @Mock private CourseService courseService;

    @Mock private AuthorizationService authorizationService;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.standaloneSetup(new CourseController(courseService, authorizationService))
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .build();
    }

    @Test
    void createCourseReturnsUnifiedResultAndChecksPermission() throws Exception {
        CourseDetailVO vo = new CourseDetailVO();
        vo.setId(11L);
        vo.setCourseNo("C-001");
        when(courseService.create(any())).thenReturn(vo);

        mockMvc.perform(
                        post("/courses")
                                .contentType("application/json")
                                .content(
                                        "{\"courseNo\":\"C-001\",\"courseName\":\"Basic experiment\","
                                                + "\"courseType\":\"experiment\",\"teacherId\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.courseNo").value("C-001"));

        verify(authorizationService).requirePermission("edu:course:add");

    }

    @Test
    void pageCourseReturnsUnifiedPageResult() throws Exception {
        CourseListVO vo = new CourseListVO();
        vo.setId(11L);
        vo.setCourseNo("C-001");
        when(courseService.page(any())).thenReturn(new PageResult<>(List.of(vo), 1, 10, 1));

        mockMvc.perform(get("/courses").param("pageNo", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].courseNo").value("C-001"));

        verify(authorizationService).requirePermission("edu:course:list");

    }

    @Test
    void publishAndOfflineCourseUseDedicatedPermission() throws Exception {
        mockMvc.perform(post("/courses/11/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        verify(authorizationService).requirePermission("edu:course:publish");

        mockMvc.perform(post("/courses/11/offline"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"));
        verify(authorizationService, org.mockito.Mockito.times(2))
                .requirePermission("edu:course:publish");
    }

    @Test
    void insufficientCoursePermissionReturnsForbidden() throws Exception {
        doThrow(new ForbiddenException("no permission"))
                .when(authorizationService)
                .requirePermission("edu:course:list");

        mockMvc.perform(get("/courses")).andExpect(status().isForbidden());
    }

}
