package com.bdis.modules.collection.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.bdis.modules.collection.constant.HerbBatchActionConstants;
import com.bdis.modules.collection.constant.HerbBatchStatusConstants;
import com.bdis.modules.collection.entity.HerbBatchEntity;
import org.junit.jupiter.api.Test;

class HerbBatchStatusFlowUtilsTest {

    @Test
    void canTransitionFollowsBatchLifecycleRules() {
        assertThat(
                        HerbBatchStatusFlowUtils.canTransition(
                                HerbBatchStatusConstants.DRAFT,
                                HerbBatchStatusConstants.COLLECTING))
                .isTrue();
        assertThat(
                        HerbBatchStatusFlowUtils.canTransition(
                                HerbBatchStatusConstants.CONFIRMED,
                                HerbBatchStatusConstants.REVIEWING))
                .isTrue();
        assertThat(
                        HerbBatchStatusFlowUtils.canTransition(
                                HerbBatchStatusConstants.REVIEWING,
                                HerbBatchStatusConstants.IDENTIFYING))
                .isTrue();
        assertThat(
                        HerbBatchStatusFlowUtils.canTransition(
                                HerbBatchStatusConstants.ARCHIVED,
                                HerbBatchStatusConstants.REVIEWING))
                .isFalse();
        assertThat(
                        HerbBatchStatusFlowUtils.canTransition(
                                HerbBatchStatusConstants.CANCELLED,
                                HerbBatchStatusConstants.COLLECTING))
                .isFalse();
    }

    @Test
    void getAllowedActionsReturnsCurrentBusinessActions() {
        HerbBatchEntity batch = new HerbBatchEntity();
        batch.setBatchStatus(HerbBatchStatusConstants.COLLECTING);

        assertThat(HerbBatchStatusFlowUtils.getAllowedActions(batch))
                .containsExactly(
                        HerbBatchActionConstants.SUBMIT,
                        HerbBatchActionConstants.CANCEL);

        batch.setBatchStatus(HerbBatchStatusConstants.CONFIRMED);
        assertThat(HerbBatchStatusFlowUtils.getAllowedActions(batch))
                .containsExactly(
                        HerbBatchActionConstants.ARCHIVE,
                        HerbBatchActionConstants.REOPEN_REVIEW,
                        HerbBatchActionConstants.CANCEL);

        batch.setBatchStatus(HerbBatchStatusConstants.ARCHIVED);
        assertThat(HerbBatchStatusFlowUtils.getAllowedActions(batch)).isEmpty();
    }
}
