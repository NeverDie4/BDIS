package com.bdis.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.event.LoginAuditPublisher;
import com.bdis.audit.query.LoginLogQuery;
import com.bdis.audit.service.LoginLogService;
import com.bdis.audit.vo.LoginLogVO;
import com.bdis.common.core.PageResult;
import com.bdis.modules.audit.entity.LoginLogEntity;
import com.bdis.modules.audit.mapper.LoginLogMapper;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;
    private final LoginAuditPublisher loginAuditPublisher;

    public LoginLogServiceImpl(
            LoginLogMapper loginLogMapper, LoginAuditPublisher loginAuditPublisher) {
        this.loginLogMapper = loginLogMapper;
        this.loginAuditPublisher = loginAuditPublisher;
    }

    @Override
    public void record(Long userId, String username, String result, String failureReason) {
        loginAuditPublisher.publish(userId, username, result, failureReason);
    }

    @Override
    public PageResult<LoginLogVO> page(LoginLogQuery query) {
        String loginResult = normalizeResult(query.getLoginResult());
        Page<LoginLogEntity> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<LoginLogEntity> wrapper =
                new LambdaQueryWrapper<LoginLogEntity>()
                        .eq(query.getUserId() != null, LoginLogEntity::getUserId, query.getUserId())
                        .eq(
                                query.getUsername() != null,
                                LoginLogEntity::getUsername,
                                query.getUsername())
                        .eq(loginResult != null, LoginLogEntity::getLoginResult, loginResult)
                        .ge(
                                query.getStartTime() != null,
                                LoginLogEntity::getLoggedInAt,
                                query.getStartTime())
                        .le(
                                query.getEndTime() != null,
                                LoginLogEntity::getLoggedInAt,
                                query.getEndTime())
                        .orderByDesc(LoginLogEntity::getLoggedInAt);
        Page<LoginLogEntity> result = loginLogMapper.selectPage(page, wrapper);
        List<LoginLogVO> records =
                result.getRecords().stream()
                        .map(
                                entity -> {
                                    LoginLogVO vo = new LoginLogVO();
                                    BeanUtils.copyProperties(entity, vo);
                                    vo.setFailureReason(entity.getFailReason());
                                    return vo;
                                })
                        .toList();
        return PageResult.of(records, result);
    }

    private String normalizeResult(String value) {
        return value == null || value.isBlank() ? null : value.toUpperCase(Locale.ROOT);
    }
}
