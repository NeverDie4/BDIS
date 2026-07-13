package com.bdis.modules;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bdis.common.security.JwtAuthenticationFilter;
import com.bdis.common.security.JwtUtils;
import com.bdis.common.security.TokenBlacklistService;
import com.bdis.config.SecurityConfig;
import com.bdis.modules.auth.service.CurrentUserService;
import com.bdis.modules.declaration.controller.DeclarationArchiveController;
import com.bdis.modules.declaration.controller.DeclarationController;
import com.bdis.modules.declaration.controller.DeclarationMaterialController;
import com.bdis.modules.declaration.service.DeclarationArchiveService;
import com.bdis.modules.declaration.service.DeclarationMaterialService;
import com.bdis.modules.declaration.service.DeclarationService;
import com.bdis.modules.evaluation.controller.EvaluationScoreController;
import com.bdis.modules.evaluation.controller.EvaluationStandardController;
import com.bdis.modules.evaluation.controller.EvaluationTaskController;
import com.bdis.modules.evaluation.service.EvaluationResultService;
import com.bdis.modules.evaluation.service.EvaluationScoreService;
import com.bdis.modules.evaluation.service.EvaluationStandardService;
import com.bdis.modules.evaluation.service.EvaluationTaskService;
import com.bdis.modules.performance.controller.PerformanceController;
import com.bdis.modules.performance.controller.PerformanceMaterialController;
import com.bdis.modules.performance.controller.PerformanceStandardController;
import com.bdis.modules.performance.service.PerformanceAuditService;
import com.bdis.modules.performance.service.PerformanceMaterialService;
import com.bdis.modules.performance.service.PerformanceService;
import com.bdis.modules.performance.service.PerformanceStandardService;
import com.bdis.modules.settings.service.UserSessionService;
import com.bdis.modules.user.mapper.UserMapper;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest
@ContextConfiguration(
        classes = {
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            EvaluationStandardController.class,
            EvaluationTaskController.class,
            EvaluationScoreController.class,
            DeclarationController.class,
            DeclarationMaterialController.class,
            DeclarationArchiveController.class,
            PerformanceController.class,
            PerformanceMaterialController.class,
            PerformanceStandardController.class
        })
class M16M17M18AuthenticationTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private JwtUtils jwtUtils;
    @MockBean private TokenBlacklistService tokenBlacklistService;
    @MockBean private CurrentUserService currentUserService;
    @MockBean private UserSessionService userSessionService;
    @MockBean private UserMapper userMapper;
    @MockBean private EvaluationStandardService evaluationStandardService;
    @MockBean private EvaluationTaskService evaluationTaskService;
    @MockBean private EvaluationScoreService evaluationScoreService;
    @MockBean private EvaluationResultService evaluationResultService;
    @MockBean private DeclarationService declarationService;
    @MockBean private DeclarationMaterialService declarationMaterialService;
    @MockBean private DeclarationArchiveService declarationArchiveService;
    @MockBean private PerformanceService performanceService;
    @MockBean private PerformanceAuditService performanceAuditService;
    @MockBean private PerformanceMaterialService performanceMaterialService;
    @MockBean private PerformanceStandardService performanceStandardService;

    @ParameterizedTest(name = "{0} {1} rejects anonymous access")
    @MethodSource("protectedInterfaces")
    void allTwentyEightInterfacesRequireAuthentication(String method, String path)
            throws Exception {
        mockMvc.perform(request(method, path)).andExpect(status().isUnauthorized());
    }

    private static Stream<Arguments> protectedInterfaces() {
        return Stream.of(
                Arguments.of("GET", "/evaluation-standards"),
                Arguments.of("POST", "/evaluation-standards"),
                Arguments.of("PUT", "/evaluation-standards/1"),
                Arguments.of("GET", "/evaluation-tasks"),
                Arguments.of("POST", "/evaluation-tasks"),
                Arguments.of("GET", "/evaluation-tasks/1"),
                Arguments.of("POST", "/evaluation-records"),
                Arguments.of("POST", "/evaluation-records/1/confirmations"),
                Arguments.of("GET", "/declarations"),
                Arguments.of("POST", "/declarations"),
                Arguments.of("GET", "/declarations/1"),
                Arguments.of("POST", "/declarations/1/submissions"),
                Arguments.of("POST", "/declarations/1/reviews"),
                Arguments.of("GET", "/declarations/1/summary"),
                Arguments.of("POST", "/declarations/1/materials"),
                Arguments.of("POST", "/declarations/1/archives"),
                Arguments.of("POST", "/declaration-archives/1/items"),
                Arguments.of("GET", "/performances"),
                Arguments.of("POST", "/performances"),
                Arguments.of("GET", "/performances/1"),
                Arguments.of("PUT", "/performances/1"),
                Arguments.of("POST", "/performances/1/submissions"),
                Arguments.of("POST", "/performances/1/audit-records"),
                Arguments.of("GET", "/performances/1/materials"),
                Arguments.of("POST", "/performances/1/materials"),
                Arguments.of("GET", "/performance-standards"),
                Arguments.of("POST", "/performance-standards"),
                Arguments.of("PUT", "/performance-standards/1"));
    }

    private static MockHttpServletRequestBuilder request(String method, String path) {
        MockHttpServletRequestBuilder request =
                switch (method) {
                    case "GET" -> get(path);
                    case "POST" -> post(path);
                    case "PUT" -> put(path);
                    default -> throw new IllegalArgumentException("Unsupported method: " + method);
                };
        if (!"GET".equals(method)) {
            request.contentType(MediaType.APPLICATION_JSON).content("{}");
        }
        return request.accept(MediaType.APPLICATION_JSON);
    }
}
