package com.bdis.modules.assistant.agent.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AgentActionMigrationTest {

  private static final Path MIGRATION =
      Path.of(
          "src/main/resources/db/migration/"
              + "V20260716_011__add_agent_business_links_and_requirements.sql");

  @Test
  void migrationAddsActionVersionBusinessLinkAndMobileRequirementsWithoutSeedData()
      throws Exception {
    String sql = Files.readString(MIGRATION, StandardCharsets.UTF_8);

    assertThat(sql).contains("ADD COLUMN `version` INT NOT NULL DEFAULT 0");
    assertThat(sql).contains("CREATE TABLE `assistant_agent_business_link`");
    assertThat(sql).contains("UNIQUE KEY `uk_agent_action_relation`");
    assertThat(sql).contains("CREATE TABLE `assistant_agent_collection_requirement`");
    assertThat(sql).contains("UNIQUE KEY `uk_agent_collection_requirement`");
    assertThat(sql).doesNotContain("INSERT INTO");
    assertThat(sql).doesNotContain("\nDROP TABLE");
  }

  @Test
  void waitConditionMigrationPersistsRecoveryStateAndOptimisticVersion() throws Exception {
    Path migration =
        Path.of("src/main/resources/db/migration/" + "V20260716_012__add_agent_wait_condition.sql");
    String sql = Files.readString(migration, StandardCharsets.UTF_8);

    assertThat(sql).contains("CREATE TABLE `assistant_agent_wait_condition`");
    assertThat(sql).contains("`condition_json` LONGTEXT NOT NULL");
    assertThat(sql).contains("`current_snapshot_json` LONGTEXT NULL");
    assertThat(sql).contains("`version` INT NOT NULL DEFAULT 0");
    assertThat(sql).contains("UNIQUE KEY `uk_agent_wait_task_type`");
    assertThat(sql).doesNotContain("INSERT INTO");
    assertThat(sql).doesNotContain("\nDROP TABLE");
  }

  @Test
  void analysisRoundMigrationStoresCanonicalSnapshotsAndHashesWithoutSeedData() throws Exception {
    Path migration =
        Path.of("src/main/resources/db/migration/" + "V20260716_013__add_agent_analysis_round.sql");
    String sql = Files.readString(migration, StandardCharsets.UTF_8);

    assertThat(sql).contains("CREATE TABLE `assistant_agent_analysis_round`");
    assertThat(sql).contains("`baseline_snapshot` LONGTEXT");
    assertThat(sql).contains("`baseline_hash` VARCHAR(64)");
    assertThat(sql).contains("`current_hash` VARCHAR(64)");
    assertThat(sql).contains("UNIQUE KEY `uk_agent_analysis_round`");
    assertThat(sql).doesNotContain("INSERT INTO");
    assertThat(sql).doesNotContain("\nDROP TABLE");
  }
}
