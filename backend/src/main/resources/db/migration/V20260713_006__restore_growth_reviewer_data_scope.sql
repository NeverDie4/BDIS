UPDATE `auth_data_scope` AS `scope`
JOIN `auth_role` AS `role`
  ON `role`.`id` = `scope`.`role_id`
  AND `role`.`is_deleted` = 0
SET `scope`.`scope_type` = `role`.`data_scope`,
    `scope`.`organization_id` = NULL,
    `scope`.`department_id` = NULL,
    `scope`.`custom_rule` = NULL,
    `scope`.`updated_at` = CURRENT_TIMESTAMP
WHERE `role`.`role_code` = 'REVIEWER'
  AND `scope`.`resource_type` = 'herb_growth_record'
  AND `scope`.`scope_type` = 'all'
  AND `scope`.`remark` = 'Reviewers can access growth records pending review'
  AND `role`.`data_scope` <> 'all'
  AND `scope`.`is_deleted` = 0;
