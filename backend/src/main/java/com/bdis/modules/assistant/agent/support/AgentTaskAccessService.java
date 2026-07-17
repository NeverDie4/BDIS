package com.bdis.modules.assistant.agent.support;

import com.bdis.common.exception.ForbiddenException;
import com.bdis.common.security.CurrentUser;
import com.bdis.common.security.SecurityUtils;
import com.bdis.modules.assistant.agent.entity.AgentTaskEntity;
import com.bdis.modules.collection.entity.HerbCollectionTaskEntity;
import com.bdis.modules.collection.support.CollectionAccessService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentTaskAccessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AgentTaskAccessService.class);
    private final CollectionAccessService collectionAccessService;

    public AgentTaskAccessService(CollectionAccessService collectionAccessService) {
        this.collectionAccessService = collectionAccessService;
    }

    public void requireCreateAccess(HerbCollectionTaskEntity collectionTask) {
        CurrentUser current = SecurityUtils.currentUser();
        if (isAdmin(current)) {
            return;
        }
        if (current.getRoleCodes().contains("TEACHER")) {
            collectionAccessService.requireTaskManage(collectionTask);
            return;
        }
        deny(current, collectionTask.getId(), "只有管理员或教师可以创建数字孪生科研 Agent 任务");
    }

    public void requireViewAccess(
            AgentTaskEntity agentTask, HerbCollectionTaskEntity collectionTask) {
        CurrentUser current = SecurityUtils.currentUser();
        if (isAdmin(current)) {
            return;
        }
        boolean directlyRelated =
                current.getUserId().equals(agentTask.getUserId())
                        || current.getUserId().equals(collectionTask.getCreatedBy())
                        || current.getUserId().equals(collectionTask.getCollectorId());
        if (directlyRelated || current.getRoleCodes().contains("REVIEWER")) {
            collectionAccessService.requireTaskAccess(collectionTask);
            return;
        }
        deny(current, collectionTask.getId(), "无权访问该 Agent 任务");
    }

    public void requireCancelAccess(AgentTaskEntity agentTask) {
        CurrentUser current = SecurityUtils.currentUser();
        if (isAdmin(current) || current.getUserId().equals(agentTask.getUserId())) {
            return;
        }
        deny(current, agentTask.getCollectionTaskId(), "只能取消本人创建的 Agent 任务");
    }

    public boolean isAdmin() {
        return isAdmin(SecurityUtils.currentUser());
    }

    public boolean isReviewer() {
        return SecurityUtils.currentUser().getRoleCodes().contains("REVIEWER");
    }

    private boolean isAdmin(CurrentUser current) {
        return current.getRoleCodes().contains("ADMIN") || current.getPermissions().contains("*");
    }

    private void deny(CurrentUser current, Long collectionTaskId, String message) {
        LOGGER.warn(
                "Agent task access denied, userId={}, collectionTaskId={}, reason={}",
                current.getUserId(),
                collectionTaskId,
                message);
        throw new ForbiddenException(message);
    }
}
