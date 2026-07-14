package com.bdis.modules.experiment.constant;

import com.bdis.common.enums.ResultCodeEnum;
import com.bdis.common.exception.BusinessException;

public final class ExperimentArchiveStatus {

    public static final String DRAFT = "draft";
    public static final String SUBMITTED = "submitted";
    public static final String ARCHIVED = "archived";

    private ExperimentArchiveStatus() {}

    public static void assertMutable(String status) {
        if (!DRAFT.equals(status)) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Only draft experiment records can be modified");
        }
    }

    public static void assertDeletable(String status) {
        if (!DRAFT.equals(status)) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Only draft experiment records can be deleted");
        }
    }

    public static void assertSubmittable(String status) {
        if (!DRAFT.equals(status)) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Only draft experiment records can be submitted");
        }
    }

    public static void assertArchivable(String status) {
        if (!SUBMITTED.equals(status)) {
            throw new BusinessException(
                    ResultCodeEnum.CONFLICT, "Only submitted experiment records can be archived");
        }
    }
}
