package com.bdis.modules.experiment.service;

import com.bdis.common.core.PageResult;
import com.bdis.file.vo.FileBusinessVO;
import com.bdis.modules.experiment.query.ExperimentRecordQuery;
import com.bdis.modules.experiment.request.ExperimentRecordArchiveRequest;
import com.bdis.modules.experiment.request.ExperimentRecordAttachmentBindRequest;
import com.bdis.modules.experiment.request.ExperimentRecordCreateRequest;
import com.bdis.modules.experiment.request.ExperimentRecordSubmitRequest;
import com.bdis.modules.experiment.request.ExperimentRecordUpdateRequest;
import com.bdis.modules.experiment.vo.ExperimentRecordDetailVO;
import com.bdis.modules.experiment.vo.ExperimentRecordListVO;
import com.bdis.modules.file.vo.FileResourceVO;
import java.util.List;

public interface ExperimentRecordService {

    PageResult<ExperimentRecordListVO> page(ExperimentRecordQuery query);

    ExperimentRecordDetailVO getDetail(Long id);

    Long create(ExperimentRecordCreateRequest request);

    void update(Long id, ExperimentRecordUpdateRequest request);

    void delete(Long id);

    void submit(Long id, ExperimentRecordSubmitRequest request);

    void archive(Long id, ExperimentRecordArchiveRequest request);

    List<FileResourceVO> listAttachments(Long id, String fileUsage);

    FileBusinessVO bindAttachment(Long id, ExperimentRecordAttachmentBindRequest request);

    void unbindAttachment(Long id, Long fileId);
}
