package com.bdis.audit.service;

import com.bdis.audit.dto.AuditRecordDTO;
import com.bdis.audit.query.AuditLogQuery;
import com.bdis.audit.vo.AuditLogVO;
import com.bdis.common.response.PageResult;

public interface AuditLogService {

    void record(AuditRecordDTO dto);

    PageResult<AuditLogVO> page(AuditLogQuery query);

    AuditLogVO detail(Long logId);
}
