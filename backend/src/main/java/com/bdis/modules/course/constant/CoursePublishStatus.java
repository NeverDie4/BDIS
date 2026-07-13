package com.bdis.modules.course.constant;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;

public final class CoursePublishStatus {

    public static final String DRAFT = "draft";
    public static final String PUBLISHED = "published";
    public static final String OFFLINE = "offline";

    private CoursePublishStatus() {}

    public static void requireEditable(String status) {
        if (PUBLISHED.equals(status)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Published course cannot be edited");
        }
        if (!DRAFT.equals(status) && !OFFLINE.equals(status)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Invalid course publish status");
        }
    }

    public static void requirePublishable(String status) {
        if (!DRAFT.equals(status) && !OFFLINE.equals(status)) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Only draft or offline course can be published");
        }
    }

    public static void requireDeletable(String status) {
        if (PUBLISHED.equals(status)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Published course cannot be deleted");
        }
        if (!DRAFT.equals(status) && !OFFLINE.equals(status)) {
            throw new BusinessException(ResultCodeEnum.CONFLICT, "Invalid course publish status");
        }
    }

    public static void requireOfflineable(String status) {
        if (!PUBLISHED.equals(status)) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT,
                    "Only published course can be taken offline");
        }
    }
}
