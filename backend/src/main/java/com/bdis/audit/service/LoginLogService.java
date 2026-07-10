package com.bdis.audit.service;

import com.bdis.audit.query.LoginLogQuery;
import com.bdis.audit.vo.LoginLogVO;
import com.bdis.common.response.PageResult;

public interface LoginLogService {

    void record(Long userId, String username, String result, String failureReason);

    PageResult<LoginLogVO> page(LoginLogQuery query);
}
