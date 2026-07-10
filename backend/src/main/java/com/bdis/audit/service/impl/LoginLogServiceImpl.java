package com.bdis.audit.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bdis.audit.entity.LoginLogEntity;
import com.bdis.audit.mapper.LoginLogMapper;
import com.bdis.audit.query.LoginLogQuery;
import com.bdis.audit.service.LoginLogService;
import com.bdis.audit.vo.LoginLogVO;
import com.bdis.common.response.PageResult;
import com.bdis.common.utils.CurrentUserUtils;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

@Service
public class LoginLogServiceImpl implements LoginLogService {

    private final LoginLogMapper loginLogMapper;

    public LoginLogServiceImpl(LoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public void record(Long userId, String username, String result, String failureReason) {
        LoginLogEntity entity = new LoginLogEntity();
        entity.setUserId(userId);
        entity.setUsername(username);
        entity.setLoginResult(result);
        entity.setFailureReason(failureReason);
        entity.setIpAddress(CurrentUserUtils.currentIp());
        entity.setUserAgent(CurrentUserUtils.currentUserAgent());
        entity.setLoggedInAt(LocalDateTime.now());
        loginLogMapper.insert(entity);
    }

    @Override
    public PageResult<LoginLogVO> page(LoginLogQuery query) {
        Page<LoginLogEntity> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<LoginLogEntity> wrapper =
                new LambdaQueryWrapper<LoginLogEntity>()
                        .eq(query.getUserId() != null, LoginLogEntity::getUserId, query.getUserId())
                        .eq(query.getUsername() != null, LoginLogEntity::getUsername, query.getUsername())
                        .eq(query.getLoginResult() != null, LoginLogEntity::getLoginResult, query.getLoginResult())
                        .orderByDesc(LoginLogEntity::getLoggedInAt);
        Page<LoginLogEntity> result = loginLogMapper.selectPage(page, wrapper);
        List<LoginLogVO> records =
                result.getRecords().stream()
                        .map(entity -> {
                            LoginLogVO vo = new LoginLogVO();
                            BeanUtils.copyProperties(entity, vo);
                            return vo;
                        })
                        .toList();
        return PageResult.of(records, result);
    }
}
