ALTER TABLE stat_dashboard_snapshot
    ADD COLUMN distribution_count INT DEFAULT 0 AFTER base_count;
