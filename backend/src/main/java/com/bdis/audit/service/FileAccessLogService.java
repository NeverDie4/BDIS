package com.bdis.audit.service;

import com.bdis.audit.dto.FileAccessRecordDTO;
import com.bdis.audit.query.FileAccessLogQuery;
import com.bdis.audit.vo.FileAccessLogVO;
import com.bdis.common.response.PageResult;

public interface FileAccessLogService {

    void record(FileAccessRecordDTO dto);

    PageResult<FileAccessLogVO> page(FileAccessLogQuery query);
}
