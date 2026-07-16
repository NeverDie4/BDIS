package com.bdis.modules.assistant.agent.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgentMapperContractTest {

  private static final Path MAPPER_ROOT = Path.of("src/main/resources/mapper/assistant/agent");

  @Test
  void taskStateUpdatesUseStatusAndVersionCompareAndSet() throws Exception {
    String xml = read("AgentTaskMapper.xml");

    assertThat(xml).contains("version = version + 1");
    assertThat(xml).contains("AND status = #{expectedStatus}");
    assertThat(xml).contains("AND version = #{expectedVersion}");
    assertThat(xml).contains("AND ct.id IS NOT NULL");
  }

  @Test
  void cancellationSqlOnlyTouchesUnfinishedChildren() throws Exception {
    String steps = read("AgentStepMapper.xml");
    String actions = read("AgentActionMapper.xml");

    assertThat(steps).contains("status IN ('PENDING', 'RUNNING', 'WAITING')");
    assertThat(actions).contains("status IN ('PROPOSED', 'WAITING_CONFIRMATION', 'CONFIRMED')");
  }

  @Test
  void allAgentMapperResourcesExist() {
    assertThat(MAPPER_ROOT.resolve("AgentTaskMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentStepMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentFindingMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentActionMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentToolCallLogMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentCollectionPlanMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentBusinessLinkMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentCollectionRequirementMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentWaitConditionMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentFieldDataMapper.xml")).exists();
    assertThat(MAPPER_ROOT.resolve("AgentAnalysisRoundMapper.xml")).exists();
  }

  @Test
  void toolCallLogMapperPersistsSuccessAndFailureAuditFields() throws Exception {
    String xml = read("AgentToolCallLogMapper.xml");

    assertThat(xml).contains("assistant_agent_tool_call_log");
    assertThat(xml).contains("input_hash");
    assertThat(xml).contains("output_json");
    assertThat(xml).contains("error_code");
    assertThat(xml).contains("error_message");
  }

  @Test
  void diagnosisStepsUseIdempotentInsertAndStatusCompareAndSet() throws Exception {
    String xml = read("AgentStepMapper.xml");

    assertThat(xml).contains("INSERT IGNORE INTO assistant_agent_step");
    assertThat(xml).contains("AND status = 'PENDING'");
    assertThat(xml).contains("AND status = 'RUNNING'");
  }

  @Test
  void diagnosisFindingsAreUpdatedThroughBusinessUniqueKey() throws Exception {
    String xml = read("AgentFindingMapper.xml");

    assertThat(xml).contains("ON DUPLICATE KEY UPDATE");
    assertThat(xml).contains("evidence_json = VALUES(evidence_json)");
    assertThat(xml).contains("description = VALUES(description)");
  }

  @Test
  void collectionPlanUpdatesUseOwnershipStatusAndVersionCompareAndSet() throws Exception {
    String plan = read("AgentCollectionPlanMapper.xml");
    String action = read("AgentActionMapper.xml");

    assertThat(plan).contains("AND agent_task_id = #{plan.agentTaskId}");
    assertThat(plan).contains("AND status IN ('DRAFT', 'PROPOSED')");
    assertThat(plan).contains("AND version = #{expectedVersion}");
    assertThat(action).contains("action_type = 'CREATE_FOLLOW_UP_COLLECTION_TASK'");
  }

  @Test
  void confirmedCollectionActionUsesCompareAndSetAndPersistentBusinessLink() throws Exception {
    String action = read("AgentActionMapper.xml");
    String link = read("AgentBusinessLinkMapper.xml");
    String requirement = read("AgentCollectionRequirementMapper.xml");

    assertThat(action).contains("status = 'WAITING_CONFIRMATION'");
    assertThat(action).contains("status = 'EXECUTING'");
    assertThat(action).contains("version = version + 1");
    assertThat(action).contains("AND version = #{expectedVersion}");
    assertThat(link).contains("assistant_agent_business_link");
    assertThat(link).contains("WHERE agent_action_id = #{agentActionId}");
    assertThat(requirement).contains("assistant_agent_collection_requirement");
    assertThat(requirement).contains("WHERE collection_task_id = #{collectionTaskId}");
  }

  @Test
  void waitConditionUsesPersistentRecoveryScanAndVersionCompareAndSet() throws Exception {
    String wait = read("AgentWaitConditionMapper.xml");
    String step = read("AgentStepMapper.xml");

    assertThat(wait).contains("assistant_agent_wait_condition");
    assertThat(wait).contains("t.is_deleted = 0");
    assertThat(wait).doesNotContain("t.deleted = 0");
    assertThat(wait).contains("t.status = 'WAITING_FIELD_DATA'");
    assertThat(wait).contains("check_count = check_count + 1");
    assertThat(wait).contains("AND version = #{expectedVersion}");
    assertThat(step).contains("AND status = 'WAITING'");
    assertThat(step).contains("INSERT IGNORE INTO assistant_agent_step");
  }

  @Test
  void taskRecoveryScanOnlySelectsEvidenceCompletedTasksWithoutActivePlan() throws Exception {
    String task = read("AgentTaskMapper.xml");

    assertThat(task).contains("selectPlanRecoveryCandidates");
    assertThat(task).contains("t.status = 'RUNNING'");
    assertThat(task).contains("t.current_phase = 'EVIDENCE_ANALYSIS_COMPLETED'");
    assertThat(task).contains("NOT EXISTS");
    assertThat(task).contains("assistant_agent_collection_plan p");
    assertThat(task).contains("t.is_deleted = 0");
  }

  @Test
  void analysisRoundUsesUniqueInsertAndStatusCompareAndSet() throws Exception {
    String round = read("AgentAnalysisRoundMapper.xml");

    assertThat(round).contains("INSERT IGNORE INTO assistant_agent_analysis_round");
    assertThat(round).contains("WHERE id = #{id} AND status = 'CREATED'");
    assertThat(round).contains("WHERE id = #{id} AND status = 'RUNNING'");
    assertThat(round).contains("baseline_hash");
    assertThat(round).contains("current_hash");
  }

  private String read(String fileName) throws Exception {
    return Files.readString(MAPPER_ROOT.resolve(fileName), StandardCharsets.UTF_8);
  }
}
