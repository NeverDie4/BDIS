package com.bdis.modules.evaluation.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bdis.common.security.BusinessAccessService;
import com.bdis.file.support.BusinessReferenceValidator;
import com.bdis.modules.evaluation.dto.EvaluationTaskRequest;
import com.bdis.modules.evaluation.entity.EvaluationTaskEntity;
import com.bdis.modules.evaluation.mapper.EvaluationResultMapper;
import com.bdis.modules.evaluation.mapper.EvaluationScoreRecordMapper;
import com.bdis.modules.evaluation.mapper.EvaluationTaskMapper;
import com.bdis.modules.evaluation.service.impl.EvaluationTaskServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationTaskServiceTest {

    @Mock private EvaluationTaskMapper taskMapper;
    @Mock private EvaluationScoreRecordMapper scoreRecordMapper;
    @Mock private EvaluationResultMapper resultMapper;
    @Mock private BusinessAccessService accessService;
    @Mock private BusinessReferenceValidator referenceValidator;
    @InjectMocks private EvaluationTaskServiceImpl service;

    @Test
    void validatesEvaluationTargetBeforeCreation() {
        when(accessService.currentUserId()).thenReturn(3L);
        doAnswer(
                        invocation -> {
                            EvaluationTaskEntity entity = invocation.getArgument(0);
                            entity.setId(7L);
                            return 1;
                        })
                .when(taskMapper)
                .insert(any(EvaluationTaskEntity.class));
        when(taskMapper.selectById(7L)).thenReturn(new EvaluationTaskEntity());
        EvaluationTaskRequest request = new EvaluationTaskRequest();
        request.setTaskName("质量评价");
        request.setTargetType("herb_species");
        request.setTargetId(5L);

        service.createTask(request);

        verify(referenceValidator).validate("herb_species", 5L);
    }
}
