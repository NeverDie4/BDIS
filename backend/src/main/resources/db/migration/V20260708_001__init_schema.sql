-- BDIS initial schema migration

-- Target database: MySQL 8.0.x

-- Generated from docs/05_数据库详细设计说明书_V0.5_全组.md.

-- This migration creates schema objects only; it does not create databases, users, grants, or seed data.



SET NAMES utf8mb4;



-- 1. sys_organization - 机构表
CREATE TABLE `sys_organization` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `organization_no` VARCHAR(64) NOT NULL COMMENT '机构编号',
  `organization_name` VARCHAR(100) NOT NULL COMMENT '机构名称',
  `organization_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '机构类型：school、college、lab、base',
  `contact_name` VARCHAR(50) NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(30) NULL DEFAULT NULL COMMENT '联系电话',
  `address` VARCHAR(255) NULL DEFAULT NULL COMMENT '地址',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_organization_organization_no` (`organization_no`),
  KEY `idx_sys_organization_name` (`organization_name`),
  KEY `idx_sys_organization_status` (`status`)
) ENGINE=InnoDB COMMENT='机构表；保存学校、学院、实验室、合作基地等机构信息。';

-- 2. sys_department - 部门表
CREATE TABLE `sys_department` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `department_no` VARCHAR(64) NOT NULL COMMENT '部门编号',
  `department_name` VARCHAR(100) NOT NULL COMMENT '部门名称',
  `organization_id` BIGINT NOT NULL COMMENT '所属机构 ID',
  `parent_id` BIGINT NULL DEFAULT 0 COMMENT '父级部门 ID',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_department_department_no` (`department_no`),
  KEY `idx_sys_department_organization_id` (`organization_id`),
  KEY `idx_sys_department_parent_id` (`parent_id`)
) ENGINE=InnoDB COMMENT='部门表；保存部门、教研室、实验室、小组等组织结构。';

-- 3. sys_user - 用户表
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_no` VARCHAR(64) NOT NULL COMMENT '用户编号',
  `username` VARCHAR(64) NOT NULL COMMENT '登录账号',
  `password_hash` VARCHAR(255) NOT NULL COMMENT '密码哈希',
  `real_name` VARCHAR(50) NULL DEFAULT NULL COMMENT '真实姓名',
  `phone_number` VARCHAR(30) NULL DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) NULL DEFAULT NULL COMMENT '邮箱',
  `organization_id` BIGINT NULL DEFAULT NULL COMMENT '所属机构 ID',
  `department_id` BIGINT NULL DEFAULT NULL COMMENT '所属部门 ID',
  `user_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '用户类型：admin、teacher、student、collector、reviewer',
  `avatar_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '头像地址',
  `last_login_at` DATETIME NULL DEFAULT NULL COMMENT '最后登录时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_user_no` (`user_no`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_department_id` (`department_id`),
  KEY `idx_sys_user_organization_id` (`organization_id`),
  KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB COMMENT='用户表；保存系统用户，用于登录、采集、审核、上传、申报和审计。';

-- 4. auth_role - 角色表
CREATE TABLE `auth_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `role_code` VARCHAR(64) NOT NULL COMMENT '角色编码',
  `role_name` VARCHAR(100) NOT NULL COMMENT '角色名称',
  `role_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '角色类型',
  `data_scope` VARCHAR(50) NULL DEFAULT NULL COMMENT '简化数据范围：all、organization、department、self、custom',
  `description` VARCHAR(255) NULL DEFAULT NULL COMMENT '角色说明',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_role_role_code` (`role_code`),
  KEY `idx_auth_role_status` (`status`),
  KEY `idx_auth_role_data_scope` (`data_scope`)
) ENGINE=InnoDB COMMENT='角色表；保存系统角色定义。';

-- 5. auth_menu - 菜单表
CREATE TABLE `auth_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `parent_id` BIGINT NULL DEFAULT 0 COMMENT '父菜单 ID',
  `menu_code` VARCHAR(100) NOT NULL COMMENT '菜单编码',
  `menu_name` VARCHAR(100) NOT NULL COMMENT '菜单名称',
  `route_path` VARCHAR(255) NULL DEFAULT NULL COMMENT '前端路由',
  `component_path` VARCHAR(255) NULL DEFAULT NULL COMMENT '组件路径',
  `icon` VARCHAR(100) NULL DEFAULT NULL COMMENT '图标',
  `visible` TINYINT NULL DEFAULT 1 COMMENT '是否显示：0 否，1 是',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_menu_menu_code` (`menu_code`),
  KEY `idx_auth_menu_parent_id` (`parent_id`),
  KEY `idx_auth_menu_status` (`status`)
) ENGINE=InnoDB COMMENT='菜单表；保存前端菜单、路由和组件路径，支持树形菜单。';

-- 6. auth_permission - 权限表
CREATE TABLE `auth_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `permission_code` VARCHAR(100) NOT NULL COMMENT '权限编码，如 herb:species:add',
  `permission_name` VARCHAR(100) NOT NULL COMMENT '权限名称',
  `permission_type` VARCHAR(50) NOT NULL COMMENT '权限类型：menu、button、api',
  `menu_id` BIGINT NULL DEFAULT NULL COMMENT '所属菜单 ID',
  `api_path` VARCHAR(255) NULL DEFAULT NULL COMMENT '接口路径',
  `request_method` VARCHAR(20) NULL DEFAULT NULL COMMENT '请求方法',
  `description` VARCHAR(255) NULL DEFAULT NULL COMMENT '权限说明',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_permission_permission_code` (`permission_code`),
  KEY `idx_auth_permission_menu_id` (`menu_id`),
  KEY `idx_auth_permission_api_path` (`api_path`),
  KEY `idx_auth_permission_type` (`permission_type`)
) ENGINE=InnoDB COMMENT='权限表；保存菜单、按钮、接口等权限点。';

-- 7. rel_user_role - 用户角色关系表
CREATE TABLE `rel_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT NOT NULL COMMENT '用户 ID',
  `role_id` BIGINT NOT NULL COMMENT '角色 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rel_user_role_user_id_role_id` (`user_id`, `role_id`),
  KEY `idx_rel_user_role_user_id` (`user_id`),
  KEY `idx_rel_user_role_role_id` (`role_id`)
) ENGINE=InnoDB COMMENT='用户角色关系表；用户与角色多对多关系。';

-- 8. rel_role_permission - 角色权限关系表
CREATE TABLE `rel_role_permission` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `role_id` BIGINT NOT NULL COMMENT '角色 ID',
  `permission_id` BIGINT NOT NULL COMMENT '权限 ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rel_role_permission_role_id_permission_id` (`role_id`, `permission_id`),
  KEY `idx_rel_role_permission_role_id` (`role_id`),
  KEY `idx_rel_role_permission_permission_id` (`permission_id`)
) ENGINE=InnoDB COMMENT='角色权限关系表；角色与权限多对多关系。';

-- 9. auth_data_scope - 数据范围权限表
CREATE TABLE `auth_data_scope` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `role_id` BIGINT NOT NULL COMMENT '角色 ID',
  `resource_type` VARCHAR(100) NOT NULL COMMENT '资源类型，如 herb_growth_record、eval_application',
  `scope_type` VARCHAR(50) NOT NULL COMMENT '范围类型：all、organization、department、self、custom',
  `organization_id` BIGINT NULL DEFAULT NULL COMMENT '机构 ID',
  `department_id` BIGINT NULL DEFAULT NULL COMMENT '部门 ID',
  `custom_rule` TEXT NULL COMMENT '自定义规则 JSON',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_data_scope_role_resource` (`role_id`, `resource_type`),
  KEY `idx_auth_data_scope_scope_type` (`scope_type`)
) ENGINE=InnoDB COMMENT='数据范围权限表；保存角色在不同业务资源上的数据可见范围。';

-- 10. dict_type - 字典类型表
CREATE TABLE `dict_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `type_code` VARCHAR(64) NOT NULL COMMENT '字典类型编码',
  `type_name` VARCHAR(100) NOT NULL COMMENT '字典类型名称',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type_type_code` (`type_code`),
  KEY `idx_dict_type_status` (`status`)
) ENGINE=InnoDB COMMENT='字典类型表；保存字典分类，如 herb_category、growth_stage、review_status。';

-- 11. dict_item - 字典项表
CREATE TABLE `dict_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `type_id` BIGINT NOT NULL COMMENT '字典类型 ID',
  `item_code` VARCHAR(64) NOT NULL COMMENT '字典项编码',
  `item_name` VARCHAR(100) NOT NULL COMMENT '字典项名称',
  `item_value` VARCHAR(100) NULL DEFAULT NULL COMMENT '字典项值',
  `parent_id` BIGINT NULL DEFAULT 0 COMMENT '父字典项 ID',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_item_type_id_item_code` (`type_id`, `item_code`),
  KEY `idx_dict_item_type_id` (`type_id`),
  KEY `idx_dict_item_parent_id` (`parent_id`)
) ENGINE=InnoDB COMMENT='字典项表；保存具体字典项。';

-- 12. dict_region - 区域字典表
CREATE TABLE `dict_region` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `region_code` VARCHAR(64) NOT NULL COMMENT '区域编码',
  `region_name` VARCHAR(100) NOT NULL COMMENT '区域名称',
  `parent_id` BIGINT NULL DEFAULT 0 COMMENT '父级区域 ID',
  `region_level` VARCHAR(50) NULL DEFAULT NULL COMMENT '区域级别：province、city、district、town、base_area',
  `longitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '纬度',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_region_region_code` (`region_code`),
  KEY `idx_dict_region_parent_id` (`parent_id`),
  KEY `idx_dict_region_level` (`region_level`)
) ENGINE=InnoDB COMMENT='区域字典表；保存省、市、区县、乡镇、基地区域等地理层级。';

-- 13. sys_file_resource - 文件资源表
CREATE TABLE `sys_file_resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `file_no` VARCHAR(64) NOT NULL COMMENT '文件编号',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名称',
  `original_filename` VARCHAR(255) NULL DEFAULT NULL COMMENT '原始文件名',
  `file_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '文件类型：image、video、document、atlas、material',
  `file_format` VARCHAR(50) NULL DEFAULT NULL COMMENT '文件格式',
  `file_size` BIGINT NULL DEFAULT NULL COMMENT '文件大小',
  `file_url` VARCHAR(500) NOT NULL COMMENT '文件访问地址',
  `thumbnail_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '缩略图地址',
  `storage_type` VARCHAR(50) NULL DEFAULT 'local' COMMENT '存储类型：local、oss、cos',
  `uploader_id` BIGINT NULL DEFAULT NULL COMMENT '上传人 ID',
  `uploaded_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_file_resource_file_no` (`file_no`),
  KEY `idx_sys_file_resource_file_type` (`file_type`),
  KEY `idx_sys_file_resource_uploader_id` (`uploader_id`),
  KEY `idx_sys_file_resource_uploaded_at` (`uploaded_at`)
) ENGINE=InnoDB COMMENT='文件资源表；统一保存图片、文档、视频、课件、图谱、申报材料、业绩材料等文件元数据。';

-- 14. sys_file_business - 文件业务关联表
CREATE TABLE `sys_file_business` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `file_id` BIGINT NOT NULL COMMENT '文件 ID',
  `biz_type` VARCHAR(100) NOT NULL COMMENT '业务类型，如 herb_species、edu_course、eval_application',
  `biz_id` BIGINT NOT NULL COMMENT '业务记录 ID',
  `file_usage` VARCHAR(50) NULL DEFAULT NULL COMMENT '用途：cover、attachment、material、video、atlas',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_sys_file_business_file_id` (`file_id`),
  KEY `idx_sys_file_business_biz` (`biz_type`, `biz_id`)
) ENGINE=InnoDB COMMENT='文件业务关联表；统一维护文件与业务对象的关联。';

-- 15. log_login - 登录日志表
CREATE TABLE `log_login` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT NULL DEFAULT NULL COMMENT '用户 ID',
  `username` VARCHAR(64) NULL DEFAULT NULL COMMENT '登录账号',
  `login_result` VARCHAR(50) NOT NULL COMMENT '登录结果：success、failed',
  `fail_reason` VARCHAR(255) NULL DEFAULT NULL COMMENT '失败原因',
  `ip_address` VARCHAR(64) NULL DEFAULT NULL COMMENT 'IP 地址',
  `user_agent` VARCHAR(500) NULL DEFAULT NULL COMMENT '客户端信息',
  `logged_in_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_login_user_id` (`user_id`),
  KEY `idx_log_login_logged_in_at` (`logged_in_at`)
) ENGINE=InnoDB COMMENT='登录日志表；记录用户登录结果、失败原因、IP 和客户端信息。';

-- 16. log_operation - 操作日志表
CREATE TABLE `log_operation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `trace_id` VARCHAR(100) NULL DEFAULT NULL COMMENT '请求追踪 ID',
  `operator_id` BIGINT NULL DEFAULT NULL COMMENT '操作人 ID',
  `operator_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '操作人名称快照',
  `operation_module` VARCHAR(100) NULL DEFAULT NULL COMMENT '操作模块',
  `operation_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '操作类型',
  `operation_desc` VARCHAR(500) NULL DEFAULT NULL COMMENT '操作描述',
  `request_method` VARCHAR(20) NULL DEFAULT NULL COMMENT '请求方法',
  `request_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '请求地址',
  `request_param` TEXT NULL COMMENT '请求参数，敏感字段脱敏',
  `result_status` VARCHAR(50) NULL DEFAULT NULL COMMENT '结果状态',
  `error_message` TEXT NULL COMMENT '错误信息',
  `ip_address` VARCHAR(64) NULL DEFAULT NULL COMMENT 'IP 地址',
  `user_agent` VARCHAR(500) NULL DEFAULT NULL COMMENT '客户端信息',
  `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_operation_operator_id` (`operator_id`),
  KEY `idx_log_operation_module` (`operation_module`),
  KEY `idx_log_operation_time` (`operation_time`)
) ENGINE=InnoDB COMMENT='操作日志表；记录后台管理、审核、上传、下载、删除等关键业务操作。';

-- 17. log_data_change - 数据变更日志表
CREATE TABLE `log_data_change` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `table_name` VARCHAR(100) NOT NULL COMMENT '被修改表名',
  `record_id` BIGINT NOT NULL COMMENT '被修改记录 ID',
  `change_type` VARCHAR(50) NOT NULL COMMENT '变更类型：insert、update、delete',
  `before_data` LONGTEXT NULL COMMENT '修改前 JSON',
  `after_data` LONGTEXT NULL COMMENT '修改后 JSON',
  `operator_id` BIGINT NULL DEFAULT NULL COMMENT '操作人 ID',
  `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_data_change_table_record` (`table_name`, `record_id`),
  KEY `idx_log_data_change_time` (`operation_time`)
) ENGINE=InnoDB COMMENT='数据变更日志表；记录重要业务表数据修改前后内容。';

-- 18. log_file_access - 文件访问日志表
CREATE TABLE `log_file_access` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `file_id` BIGINT NOT NULL COMMENT '文件 ID',
  `operator_id` BIGINT NULL DEFAULT NULL COMMENT '操作人 ID',
  `access_type` VARCHAR(50) NOT NULL COMMENT '访问类型：preview、download、play',
  `ip_address` VARCHAR(64) NULL DEFAULT NULL COMMENT 'IP 地址',
  `user_agent` VARCHAR(500) NULL DEFAULT NULL COMMENT '客户端信息',
  `operation_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_file_access_file_id` (`file_id`),
  KEY `idx_log_file_access_operator_id` (`operator_id`),
  KEY `idx_log_file_access_time` (`operation_time`)
) ENGINE=InnoDB COMMENT='文件访问日志表；记录文件预览、下载、播放等访问行为。';

-- 19. log_data_sync - 数据同步日志表
CREATE TABLE `log_data_sync` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `trace_id` VARCHAR(100) NULL DEFAULT NULL COMMENT '请求追踪 ID',
  `sync_type` VARCHAR(50) NOT NULL COMMENT '同步类型：soap、app_upload、batch_import',
  `source_system` VARCHAR(100) NULL DEFAULT NULL COMMENT '来源系统',
  `request_data` LONGTEXT NULL COMMENT '请求数据，JSON 或 XML',
  `response_data` LONGTEXT NULL COMMENT '响应数据',
  `sync_status` VARCHAR(50) NOT NULL DEFAULT 'processing' COMMENT '同步状态：processing、success、failed',
  `error_message` TEXT NULL COMMENT '错误信息',
  `target_table` VARCHAR(100) NULL DEFAULT NULL COMMENT '目标表',
  `target_id` BIGINT NULL DEFAULT NULL COMMENT '目标记录 ID',
  `operator_id` BIGINT NULL DEFAULT NULL COMMENT '操作人 ID',
  `operation_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_data_sync_sync_type` (`sync_type`),
  KEY `idx_log_data_sync_status` (`sync_status`),
  KEY `idx_log_data_sync_target` (`target_table`, `target_id`)
) ENGINE=InnoDB COMMENT='数据同步日志表；保存 APP 上传、批量导入、外部系统同步等通用同步日志。';

-- 20. herb_species - 中药材品种基础信息表
CREATE TABLE `herb_species` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `herb_no` VARCHAR(50) NOT NULL COMMENT '药材编号，业务唯一编码',
  `herb_name` VARCHAR(100) NOT NULL COMMENT '药材名称',
  `alias_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '别名',
  `latin_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '拉丁学名',
  `category_id` BIGINT NULL DEFAULT NULL COMMENT '药材分类 ID，关联 dict_item.id',
  `category_code` VARCHAR(50) NULL DEFAULT NULL COMMENT '药材分类编码',
  `medicinal_part` VARCHAR(100) NULL DEFAULT NULL COMMENT '主要药用部位',
  `efficacy` TEXT NULL COMMENT '功效主治',
  `growth_environment` TEXT NULL COMMENT '适宜生长环境',
  `origin_area` VARCHAR(255) NULL DEFAULT NULL COMMENT '产地或分布区域说明',
  `growth_cycle` VARCHAR(100) NULL DEFAULT NULL COMMENT '生长周期',
  `description` TEXT NULL COMMENT '品种简介',
  `knowledge_entity_id` BIGINT NULL DEFAULT NULL COMMENT '对应知识图谱实体 ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_species_herb_no` (`herb_no`),
  KEY `idx_herb_species_herb_name` (`herb_name`),
  KEY `idx_herb_species_category_id` (`category_id`),
  KEY `idx_herb_species_knowledge_entity_id` (`knowledge_entity_id`)
) ENGINE=InnoDB COMMENT='中药材品种基础信息表；全系统唯一的中药材主数据表。';

-- 21. herb_base - 中药材基地表
CREATE TABLE `herb_base` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `base_no` VARCHAR(64) NOT NULL COMMENT '基地编号',
  `base_name` VARCHAR(150) NOT NULL COMMENT '基地名称',
  `base_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '基地类型：planting、collection、teaching、research',
  `region_id` BIGINT NULL DEFAULT NULL COMMENT '区域 ID',
  `address` VARCHAR(255) NULL DEFAULT NULL COMMENT '详细地址',
  `longitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '纬度',
  `contact_name` VARCHAR(50) NULL DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(30) NULL DEFAULT NULL COMMENT '联系电话',
  `description` TEXT NULL COMMENT '基地说明',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_base_base_no` (`base_no`),
  KEY `idx_herb_base_region_id` (`region_id`),
  KEY `idx_herb_base_base_type` (`base_type`)
) ENGINE=InnoDB COMMENT='中药材基地表；保存种植基地、采集基地、教学实践基地等基地信息。';

-- 22. herb_distribution - 中药材地图分布点表
CREATE TABLE `herb_distribution` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `species_id` BIGINT NOT NULL COMMENT '药材品种 ID',
  `base_id` BIGINT NULL DEFAULT NULL COMMENT '所属基地 ID',
  `region_id` BIGINT NULL DEFAULT NULL COMMENT '区域 ID',
  `location_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '地点名称',
  `longitude` DECIMAL(10,7) NOT NULL COMMENT '经度',
  `latitude` DECIMAL(10,7) NOT NULL COMMENT '纬度',
  `province` VARCHAR(64) NULL DEFAULT '重庆市' COMMENT '省份',
  `city` VARCHAR(64) NULL DEFAULT '重庆市' COMMENT '城市',
  `district` VARCHAR(64) NULL DEFAULT NULL COMMENT '区县',
  `address` VARCHAR(255) NULL DEFAULT NULL COMMENT '详细地址',
  `altitude` DECIMAL(8,2) NULL DEFAULT NULL COMMENT '海拔',
  `distribution_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '分布类型：wild、cultivated、specimen',
  `distribution_level` VARCHAR(50) NULL DEFAULT NULL COMMENT '分布等级',
  `distribution_desc` VARCHAR(500) NULL DEFAULT NULL COMMENT '分布说明',
  `cover_image_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '封面图片 URL',
  `last_collected_at` DATETIME NULL DEFAULT NULL COMMENT '最近采集时间',
  `source_type` VARCHAR(50) NULL DEFAULT 'pc' COMMENT '来源类型：pc、app、soap',
  `data_source` VARCHAR(100) NULL DEFAULT NULL COMMENT '数据来源说明',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_herb_distribution_species_id` (`species_id`),
  KEY `idx_herb_distribution_region_id` (`region_id`),
  KEY `idx_herb_distribution_longitude_latitude` (`longitude`, `latitude`),
  KEY `idx_herb_distribution_district` (`district`)
) ENGINE=InnoDB COMMENT='中药材地图分布点表；保存药材地图点位、分布区域、经纬度、地址、海拔、来源和封面图。';

-- 23. herb_growth_record - 中药材生长采集记录表
CREATE TABLE `herb_growth_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `species_id` BIGINT NOT NULL COMMENT '药材品种 ID',
  `distribution_id` BIGINT NULL DEFAULT NULL COMMENT '地图分布点 ID',
  `collector_id` BIGINT NULL DEFAULT NULL COMMENT '采集人用户 ID',
  `collector_name_snapshot` VARCHAR(64) NULL DEFAULT NULL COMMENT '采集人姓名快照',
  `region_id` BIGINT NULL DEFAULT NULL COMMENT '区域 ID',
  `longitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '纬度',
  `growth_stage` VARCHAR(50) NULL DEFAULT NULL COMMENT '生长阶段',
  `soil_type` VARCHAR(64) NULL DEFAULT NULL COMMENT '土壤类型',
  `soil_ph` DECIMAL(4,2) NULL DEFAULT NULL COMMENT '土壤 pH',
  `temperature` DECIMAL(5,2) NULL DEFAULT NULL COMMENT '温度',
  `humidity` DECIMAL(5,2) NULL DEFAULT NULL COMMENT '湿度',
  `weather` VARCHAR(50) NULL DEFAULT NULL COMMENT '天气',
  `sample_weight` DECIMAL(10,3) NULL DEFAULT NULL COMMENT '样本重量 g',
  `device_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '设备类型：app、pc、iot',
  `data_source` VARCHAR(50) NULL DEFAULT 'manual' COMMENT '数据来源',
  `review_status` VARCHAR(50) NULL DEFAULT 'draft' COMMENT '审核状态',
  `submitted_at` DATETIME NULL DEFAULT NULL COMMENT '提交时间',
  `reviewed_at` DATETIME NULL DEFAULT NULL COMMENT '最近审核时间',
  `archived_at` DATETIME NULL DEFAULT NULL COMMENT '归档时间',
  `collected_at` DATETIME NULL DEFAULT NULL COMMENT '采集时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_herb_growth_record_species_id` (`species_id`),
  KEY `idx_herb_growth_record_distribution_id` (`distribution_id`),
  KEY `idx_herb_growth_record_collected_at` (`collected_at`),
  KEY `idx_herb_growth_record_review_status` (`review_status`)
) ENGINE=InnoDB COMMENT='中药材生长采集记录表；保存移动网页版、PC 或设备采集的中药材生长环境、形态和位置数据。';

-- 24. herb_growth_review_record - 采集审核记录表
CREATE TABLE `herb_growth_review_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `growth_record_id` BIGINT NOT NULL COMMENT '生长采集记录 ID',
  `reviewer_id` BIGINT NULL DEFAULT NULL COMMENT '审核人 ID',
  `review_action` VARCHAR(50) NOT NULL COMMENT '动作：submit、approve、reject、archive',
  `before_status` VARCHAR(50) NULL DEFAULT NULL COMMENT '操作前状态',
  `after_status` VARCHAR(50) NULL DEFAULT NULL COMMENT '操作后状态',
  `review_comment` VARCHAR(500) NULL DEFAULT NULL COMMENT '审核意见',
  `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_herb_growth_review_record_record_id` (`growth_record_id`),
  KEY `idx_herb_growth_review_record_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB COMMENT='采集审核记录表；保存采集记录提交、审核通过、退回、归档等流程历史。';

-- 25. herb_ai_model_version - AI 模型版本表
CREATE TABLE `herb_ai_model_version` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `model_code` VARCHAR(100) NOT NULL COMMENT '模型编码',
  `model_name` VARCHAR(100) NOT NULL COMMENT '模型名称',
  `model_type` VARCHAR(50) NOT NULL COMMENT '模型类型',
  `model_version` VARCHAR(100) NULL DEFAULT NULL COMMENT '模型版本号',
  `provider` VARCHAR(100) NULL DEFAULT NULL COMMENT '模型提供方',
  `api_endpoint` VARCHAR(500) NULL DEFAULT NULL COMMENT '模型服务地址',
  `feature_dim` INT NULL DEFAULT NULL COMMENT '特征向量维度',
  `is_default` TINYINT NULL DEFAULT 0 COMMENT '是否默认模型',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_ai_model_version_model_code` (`model_code`),
  KEY `idx_herb_ai_model_version_model_type` (`model_type`),
  KEY `idx_herb_ai_model_version_status` (`status`)
) ENGINE=InnoDB COMMENT='AI 模型版本表；记录图像识别模型、特征提取模型和图谱比对模型版本。';

-- 26. herb_atlas - 中药材标准图谱表
CREATE TABLE `herb_atlas` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `atlas_no` VARCHAR(50) NOT NULL COMMENT '标准图谱编号',
  `species_id` BIGINT NOT NULL COMMENT '药材品种 ID',
  `herb_name` VARCHAR(100) NOT NULL COMMENT '药材名称冗余展示字段',
  `atlas_title` VARCHAR(255) NOT NULL COMMENT '图谱标题',
  `image_url` VARCHAR(500) NOT NULL COMMENT '标准图谱图片地址',
  `thumbnail_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '缩略图',
  `image_type` VARCHAR(50) NOT NULL COMMENT '图片类型',
  `growth_stage` VARCHAR(50) NULL DEFAULT NULL COMMENT '生长阶段',
  `medicinal_part` VARCHAR(50) NULL DEFAULT NULL COMMENT '药用部位',
  `health_status` VARCHAR(50) NULL DEFAULT 'normal' COMMENT '健康状态',
  `form_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '形态',
  `color_feature` VARCHAR(255) NULL DEFAULT NULL COMMENT '颜色特征',
  `texture_feature` VARCHAR(255) NULL DEFAULT NULL COMMENT '纹理特征',
  `shape_feature` VARCHAR(255) NULL DEFAULT NULL COMMENT '形态特征',
  `identification_points` TEXT NULL COMMENT '人工识别要点',
  `region_id` BIGINT NULL DEFAULT NULL COMMENT '采集区域 ID',
  `collector_id` BIGINT NULL DEFAULT NULL COMMENT '采集人 ID',
  `collected_at` DATETIME NULL DEFAULT NULL COMMENT '采集时间',
  `source_type` VARCHAR(50) NULL DEFAULT 'manual' COMMENT '来源类型',
  `source_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '来源 URL',
  `license_desc` VARCHAR(255) NULL DEFAULT NULL COMMENT '授权说明',
  `has_watermark` TINYINT NULL DEFAULT 0 COMMENT '是否有水印',
  `image_quality` VARCHAR(50) NULL DEFAULT 'normal' COMMENT '图片质量',
  `usable_for_feature` TINYINT NULL DEFAULT 1 COMMENT '是否参与特征比对',
  `feature_vector` LONGTEXT NULL COMMENT '图像特征向量 JSON',
  `feature_dim` INT NULL DEFAULT NULL COMMENT '向量维度',
  `feature_model_id` BIGINT NULL DEFAULT NULL COMMENT '特征模型 ID',
  `quality_status` VARCHAR(50) NULL DEFAULT 'available' COMMENT '图谱质量状态',
  `knowledge_entity_id` BIGINT NULL DEFAULT NULL COMMENT '知识图谱实体 ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_atlas_atlas_no` (`atlas_no`),
  KEY `idx_herb_atlas_species_id` (`species_id`),
  KEY `idx_herb_atlas_image_type_growth_stage` (`image_type`, `growth_stage`),
  KEY `idx_herb_atlas_usable_for_feature` (`usable_for_feature`)
) ENGINE=InnoDB COMMENT='中药材标准图谱表；保存标准图片、结构化特征、识别要点、来源信息和特征向量。';

-- 27. herb_atlas_tag - 标准图谱标签表
CREATE TABLE `herb_atlas_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `atlas_id` BIGINT NOT NULL COMMENT '标准图谱 ID',
  `tag_name` VARCHAR(100) NOT NULL COMMENT '标签名称',
  `tag_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '标签类型',
  `tag_source` VARCHAR(50) NULL DEFAULT 'manual' COMMENT '标签来源',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_herb_atlas_tag_atlas_id` (`atlas_id`),
  KEY `idx_herb_atlas_tag_tag_name` (`tag_name`)
) ENGINE=InnoDB COMMENT='标准图谱标签表；维护标准图谱扩展标签。';

-- 28. herb_image - 用户上传/采集中药材图片表
CREATE TABLE `herb_image` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `image_no` VARCHAR(50) NOT NULL COMMENT '图片业务编号',
  `species_id` BIGINT NULL DEFAULT NULL COMMENT '药材品种 ID',
  `distribution_id` BIGINT NULL DEFAULT NULL COMMENT '地图分布点 ID',
  `growth_record_id` BIGINT NULL DEFAULT NULL COMMENT '生长采集记录 ID',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片地址',
  `thumbnail_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '缩略图',
  `original_filename` VARCHAR(255) NULL DEFAULT NULL COMMENT '原始文件名',
  `file_size` BIGINT NULL DEFAULT NULL COMMENT '文件大小',
  `file_format` VARCHAR(20) NULL DEFAULT NULL COMMENT '文件格式',
  `image_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '图片类型',
  `image_purpose` VARCHAR(50) NULL DEFAULT NULL COMMENT '图片用途',
  `upload_source` VARCHAR(50) NULL DEFAULT 'pc' COMMENT '上传来源',
  `uploader_id` BIGINT NULL DEFAULT NULL COMMENT '上传人 ID',
  `region_id` BIGINT NULL DEFAULT NULL COMMENT '采集区域 ID',
  `collected_location` VARCHAR(255) NULL DEFAULT NULL COMMENT '采集地点文字说明',
  `longitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '经度',
  `latitude` DECIMAL(10,7) NULL DEFAULT NULL COMMENT '纬度',
  `collected_at` DATETIME NULL DEFAULT NULL COMMENT '采集时间',
  `growth_stage` VARCHAR(50) NULL DEFAULT NULL COMMENT '生长阶段',
  `health_status` VARCHAR(50) NULL DEFAULT NULL COMMENT '健康状态',
  `form_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '形态',
  `feature_vector` LONGTEXT NULL COMMENT '图片特征向量 JSON',
  `feature_dim` INT NULL DEFAULT NULL COMMENT '向量维度',
  `feature_model_id` BIGINT NULL DEFAULT NULL COMMENT '特征模型 ID',
  `feature_model_code` VARCHAR(100) NULL DEFAULT NULL COMMENT '特征模型编码',
  `feature_model_version` VARCHAR(100) NULL DEFAULT NULL COMMENT '特征模型版本',
  `process_status` VARCHAR(50) NULL DEFAULT 'pending' COMMENT '处理状态',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_image_image_no` (`image_no`),
  KEY `idx_herb_image_species_id` (`species_id`),
  KEY `idx_herb_image_distribution_id` (`distribution_id`),
  KEY `idx_herb_image_process_status` (`process_status`)
) ENGINE=InnoDB COMMENT='用户上传/采集中药材图片表；保存 PC、移动网页版或 APP 上传的药材图片。';

-- 29. herb_image_recognition - 用户图片 AI 识别结果表
CREATE TABLE `herb_image_recognition` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `image_id` BIGINT NOT NULL COMMENT '用户图片 ID',
  `model_id` BIGINT NULL DEFAULT NULL COMMENT '识别模型 ID',
  `model_code` VARCHAR(100) NULL DEFAULT NULL COMMENT '识别模型编码',
  `model_version` VARCHAR(100) NULL DEFAULT NULL COMMENT '模型版本',
  `rank_no` INT NOT NULL COMMENT '识别排名',
  `species_id` BIGINT NULL DEFAULT NULL COMMENT '识别药材 ID',
  `recognized_herb_name` VARCHAR(100) NOT NULL COMMENT '识别药材名称',
  `confidence` DECIMAL(6,4) NULL DEFAULT NULL COMMENT '置信度 0-1',
  `image_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '图片类型',
  `growth_stage` VARCHAR(50) NULL DEFAULT NULL COMMENT '生长阶段',
  `medicinal_part` VARCHAR(50) NULL DEFAULT NULL COMMENT '药用部位',
  `health_status` VARCHAR(50) NULL DEFAULT NULL COMMENT '健康状态',
  `form_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '形态',
  `color_feature` VARCHAR(255) NULL DEFAULT NULL COMMENT '颜色特征',
  `texture_feature` VARCHAR(255) NULL DEFAULT NULL COMMENT '纹理特征',
  `shape_feature` VARCHAR(255) NULL DEFAULT NULL COMMENT '形态特征',
  `reason` TEXT NULL COMMENT '判断依据',
  `suggestion` TEXT NULL COMMENT '模型建议',
  `raw_response` LONGTEXT NULL COMMENT '模型原始返回',
  `is_uncertain` TINYINT NULL DEFAULT 0 COMMENT '是否不确定',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_herb_image_recognition_image_id` (`image_id`),
  KEY `idx_herb_image_recognition_rank_no` (`rank_no`)
) ENGINE=InnoDB COMMENT='用户图片 AI 识别结果表；保存 AI 对用户上传图片的 TopN 候选识别结果。';

-- 30. herb_image_match - 用户图片与标准图谱比对结果表
CREATE TABLE `herb_image_match` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `image_id` BIGINT NOT NULL COMMENT '用户图片 ID',
  `recognition_id` BIGINT NULL DEFAULT NULL COMMENT 'AI 识别结果 ID',
  `atlas_id` BIGINT NOT NULL COMMENT '匹配标准图谱 ID',
  `species_id` BIGINT NULL DEFAULT NULL COMMENT '匹配药材 ID',
  `herb_name` VARCHAR(100) NULL DEFAULT NULL COMMENT '匹配药材名称',
  `match_rank` INT NOT NULL COMMENT '匹配排名',
  `image_similarity` DECIMAL(8,6) NULL DEFAULT NULL COMMENT '图像相似度',
  `metadata_score` DECIMAL(8,6) NULL DEFAULT NULL COMMENT '结构化字段匹配分',
  `final_score` DECIMAL(8,4) NULL DEFAULT NULL COMMENT '最终匹配分',
  `match_level` VARCHAR(50) NULL DEFAULT NULL COMMENT '匹配等级',
  `is_image_type_matched` TINYINT NULL DEFAULT 0 COMMENT '图片类型是否匹配',
  `is_growth_stage_matched` TINYINT NULL DEFAULT 0 COMMENT '生长阶段是否匹配',
  `is_medicinal_part_matched` TINYINT NULL DEFAULT 0 COMMENT '药用部位是否匹配',
  `is_health_status_matched` TINYINT NULL DEFAULT 0 COMMENT '健康状态是否匹配',
  `is_form_type_matched` TINYINT NULL DEFAULT 0 COMMENT '形态是否匹配',
  `match_method` VARCHAR(50) NULL DEFAULT 'hybrid' COMMENT '匹配方式',
  `warning_msg` VARCHAR(500) NULL DEFAULT NULL COMMENT '提示信息',
  `suggestion` TEXT NULL COMMENT '系统建议',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_herb_image_match_image_id` (`image_id`),
  KEY `idx_herb_image_match_atlas_id` (`atlas_id`),
  KEY `idx_herb_image_match_level` (`match_level`)
) ENGINE=InnoDB COMMENT='用户图片与标准图谱比对结果表；保存用户图片与标准图谱之间的 TopN 相似度比对结果。';

-- 31. herb_knowledge_entity - 中药材知识图谱实体表
CREATE TABLE `herb_knowledge_entity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `entity_no` VARCHAR(50) NOT NULL COMMENT '实体编号',
  `entity_name` VARCHAR(255) NOT NULL COMMENT '实体名称',
  `entity_type` VARCHAR(100) NOT NULL COMMENT '实体类型：herb、region、effect、course、project、atlas、record',
  `ref_table` VARCHAR(100) NULL DEFAULT NULL COMMENT '关联业务表名',
  `ref_id` BIGINT NULL DEFAULT NULL COMMENT '关联业务记录 ID',
  `description` TEXT NULL COMMENT '实体描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_knowledge_entity_entity_no` (`entity_no`),
  KEY `idx_herb_knowledge_entity_type` (`entity_type`),
  KEY `idx_herb_knowledge_entity_ref` (`ref_table`, `ref_id`)
) ENGINE=InnoDB COMMENT='中药材知识图谱实体表；保存知识图谱节点实体。';

-- 32. herb_knowledge_relation - 中药材知识图谱关系类型表
CREATE TABLE `herb_knowledge_relation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `relation_code` VARCHAR(100) NOT NULL COMMENT '关系编码',
  `relation_name` VARCHAR(100) NOT NULL COMMENT '关系名称',
  `description` TEXT NULL COMMENT '关系说明',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_knowledge_relation_relation_code` (`relation_code`),
  UNIQUE KEY `uk_herb_knowledge_relation_relation_name` (`relation_name`)
) ENGINE=InnoDB COMMENT='中药材知识图谱关系类型表；维护知识图谱关系类型。';

-- 33. herb_knowledge_triple - 中药材知识图谱三元组表
CREATE TABLE `herb_knowledge_triple` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `subject_entity_id` BIGINT NOT NULL COMMENT '主语实体 ID',
  `relation_id` BIGINT NOT NULL COMMENT '关系 ID',
  `object_entity_id` BIGINT NOT NULL COMMENT '宾语实体 ID',
  `confidence` DECIMAL(6,4) NULL DEFAULT 1.0000 COMMENT '关系可信度',
  `source_type` VARCHAR(100) NULL DEFAULT NULL COMMENT '来源类型：manual、system、document_extract、ai',
  `source_desc` VARCHAR(255) NULL DEFAULT NULL COMMENT '来源说明',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_knowledge_triple_sro` (`subject_entity_id`, `relation_id`, `object_entity_id`),
  KEY `idx_herb_knowledge_triple_subject` (`subject_entity_id`),
  KEY `idx_herb_knowledge_triple_object` (`object_entity_id`),
  KEY `idx_herb_knowledge_triple_relation` (`relation_id`)
) ENGINE=InnoDB COMMENT='中药材知识图谱三元组表；保存实体-关系-实体三元组数据。';

-- 34. edu_course - 实验课程表
CREATE TABLE `edu_course` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `course_no` VARCHAR(64) NOT NULL COMMENT '课程编号',
  `course_name` VARCHAR(150) NOT NULL COMMENT '课程名称',
  `course_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '课程类型',
  `teacher_id` BIGINT NULL DEFAULT NULL COMMENT '授课教师 ID',
  `description` TEXT NULL COMMENT '课程简介',
  `video_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '视频链接',
  `publish_status` VARCHAR(50) NULL DEFAULT 'draft' COMMENT '发布状态',
  `started_at` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `ended_at` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edu_course_course_no` (`course_no`),
  KEY `idx_edu_course_teacher_id` (`teacher_id`),
  KEY `idx_edu_course_publish_status` (`publish_status`)
) ENGINE=InnoDB COMMENT='实验课程表；保存实验课程、培训课程和课程发布信息。';

-- 35. edu_experiment_step - 实验步骤表
CREATE TABLE `edu_experiment_step` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `course_id` BIGINT NOT NULL COMMENT '课程 ID',
  `step_no` VARCHAR(50) NOT NULL COMMENT '步骤编号',
  `step_title` VARCHAR(200) NOT NULL COMMENT '步骤标题',
  `step_content` TEXT NULL COMMENT '步骤内容',
  `expected_result` TEXT NULL COMMENT '预期结果',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edu_experiment_step_course_step_no` (`course_id`, `step_no`),
  KEY `idx_edu_experiment_step_course_id` (`course_id`)
) ENGINE=InnoDB COMMENT='实验步骤表；保存课程实验步骤、步骤内容和预期结果。';

-- 36. edu_course_resource - 教学课程资源表
CREATE TABLE `edu_course_resource` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `course_id` BIGINT NOT NULL COMMENT '课程 ID',
  `resource_name` VARCHAR(150) NOT NULL COMMENT '资源名称',
  `resource_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '资源类型',
  `file_id` BIGINT NULL DEFAULT NULL COMMENT '统一文件资源 ID',
  `file_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '文件地址兼容字段',
  `file_size` BIGINT NULL DEFAULT NULL COMMENT '文件大小',
  `file_format` VARCHAR(50) NULL DEFAULT NULL COMMENT '文件格式',
  `uploader_id` BIGINT NULL DEFAULT NULL COMMENT '上传人 ID',
  `uploaded_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `download_count` INT NULL DEFAULT 0 COMMENT '下载次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_edu_course_resource_course_id` (`course_id`),
  KEY `idx_edu_course_resource_type` (`resource_type`)
) ENGINE=InnoDB COMMENT='教学课程资源表；保存课件、文档、视频、实验指导书等课程资源。';

-- 37. edu_training_plan - 培训计划表
CREATE TABLE `edu_training_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `plan_no` VARCHAR(64) NOT NULL COMMENT '培训计划编号',
  `plan_name` VARCHAR(200) NOT NULL COMMENT '培训计划名称',
  `plan_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '培训类型',
  `owner_id` BIGINT NULL DEFAULT NULL COMMENT '负责人 ID',
  `description` TEXT NULL COMMENT '培训说明',
  `started_at` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `ended_at` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `publish_status` VARCHAR(50) NULL DEFAULT 'draft' COMMENT '发布状态',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edu_training_plan_plan_no` (`plan_no`),
  KEY `idx_edu_training_plan_owner_id` (`owner_id`),
  KEY `idx_edu_training_plan_publish_status` (`publish_status`)
) ENGINE=InnoDB COMMENT='培训计划表；保存培训计划、时间、负责人和培训目标。';

-- 38. edu_training_record - 培训参与记录表
CREATE TABLE `edu_training_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` BIGINT NOT NULL COMMENT '参与用户 ID',
  `course_id` BIGINT NULL DEFAULT NULL COMMENT '课程 ID',
  `plan_id` BIGINT NULL DEFAULT NULL COMMENT '培训计划 ID',
  `progress` DECIMAL(5,2) NULL DEFAULT 0.00 COMMENT '学习进度',
  `training_status` VARCHAR(50) NULL DEFAULT 'not_started' COMMENT '状态',
  `score` DECIMAL(5,2) NULL DEFAULT NULL COMMENT '成绩',
  `started_at` DATETIME NULL DEFAULT NULL COMMENT '开始学习时间',
  `completed_at` DATETIME NULL DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_edu_training_record_user_id` (`user_id`),
  KEY `idx_edu_training_record_course_id` (`course_id`),
  KEY `idx_edu_training_record_plan_id` (`plan_id`)
) ENGINE=InnoDB COMMENT='培训参与记录表；保存用户参与课程或培训计划的进度、成绩和状态。';

-- 39. edu_training_feedback - 培训反馈表
CREATE TABLE `edu_training_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `training_record_id` BIGINT NOT NULL COMMENT '培训记录 ID',
  `user_id` BIGINT NOT NULL COMMENT '反馈用户 ID',
  `rating` DECIMAL(4,2) NULL DEFAULT NULL COMMENT '评分',
  `feedback_content` TEXT NULL COMMENT '反馈内容',
  `submitted_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_edu_training_feedback_record_id` (`training_record_id`),
  KEY `idx_edu_training_feedback_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='培训反馈表；保存培训参与人员的反馈评价。';

-- 40. edu_experiment_record - 实验记录表
CREATE TABLE `edu_experiment_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `record_no` VARCHAR(64) NOT NULL COMMENT '实验记录编号',
  `course_id` BIGINT NULL DEFAULT NULL COMMENT '课程 ID',
  `project_id` BIGINT NULL DEFAULT NULL COMMENT '课题 ID',
  `experiment_title` VARCHAR(200) NOT NULL COMMENT '实验标题',
  `experiment_process` TEXT NULL COMMENT '实验过程',
  `experiment_result` TEXT NULL COMMENT '实验结果',
  `recorder_id` BIGINT NULL DEFAULT NULL COMMENT '记录人 ID',
  `recorded_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `archive_status` VARCHAR(50) NULL DEFAULT 'draft' COMMENT '归档状态',
  `archived_at` DATETIME NULL DEFAULT NULL COMMENT '归档时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edu_experiment_record_record_no` (`record_no`),
  KEY `idx_edu_experiment_record_course_id` (`course_id`),
  KEY `idx_edu_experiment_record_project_id` (`project_id`)
) ENGINE=InnoDB COMMENT='实验记录表；记录课程或课题下的实验过程、结果、附件和归档信息。';

-- 41. research_project - 科研课题表
CREATE TABLE `research_project` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `project_no` VARCHAR(64) NOT NULL COMMENT '课题编号',
  `project_name` VARCHAR(200) NOT NULL COMMENT '课题名称',
  `project_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '课题类型',
  `leader_id` BIGINT NULL DEFAULT NULL COMMENT '负责人 ID',
  `species_id` BIGINT NULL DEFAULT NULL COMMENT '关联药材 ID',
  `description` TEXT NULL COMMENT '课题简介',
  `started_at` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `ended_at` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `project_status` VARCHAR(50) NULL DEFAULT 'planning' COMMENT '课题状态',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_research_project_project_no` (`project_no`),
  KEY `idx_research_project_leader_id` (`leader_id`),
  KEY `idx_research_project_species_id` (`species_id`)
) ENGINE=InnoDB COMMENT='科研课题表；保存科研课题、负责人、周期、状态和关联药材信息。';

-- 42. rel_project_member - 课题成员关系表
CREATE TABLE `rel_project_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `project_id` BIGINT NOT NULL COMMENT '课题 ID',
  `user_id` BIGINT NOT NULL COMMENT '成员用户 ID',
  `member_role` VARCHAR(50) NULL DEFAULT NULL COMMENT '成员角色',
  `joined_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rel_project_member_project_id_user_id` (`project_id`, `user_id`),
  KEY `idx_rel_project_member_project_id` (`project_id`),
  KEY `idx_rel_project_member_user_id` (`user_id`)
) ENGINE=InnoDB COMMENT='课题成员关系表；课题与用户成员多对多关系。';

-- 43. research_achievement - 科研成果表
CREATE TABLE `research_achievement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `achievement_no` VARCHAR(64) NOT NULL COMMENT '成果编号',
  `project_id` BIGINT NULL DEFAULT NULL COMMENT '所属课题 ID',
  `owner_id` BIGINT NULL DEFAULT NULL COMMENT '成果负责人 ID',
  `achievement_name` VARCHAR(200) NOT NULL COMMENT '成果名称',
  `achievement_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '成果类型',
  `published_at` DATETIME NULL DEFAULT NULL COMMENT '发表或完成时间',
  `file_id` BIGINT NULL DEFAULT NULL COMMENT '成果附件文件 ID',
  `file_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '附件地址兼容字段',
  `description` TEXT NULL COMMENT '成果说明',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_research_achievement_achievement_no` (`achievement_no`),
  KEY `idx_research_achievement_project_id` (`project_id`),
  KEY `idx_research_achievement_owner_id` (`owner_id`)
) ENGINE=InnoDB COMMENT='科研成果表；保存论文、专利、报告、实验成果等科研成果。';

-- 44. eval_indicator - 评价指标表
CREATE TABLE `eval_indicator` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `indicator_no` VARCHAR(64) NOT NULL COMMENT '指标编号',
  `indicator_name` VARCHAR(150) NOT NULL COMMENT '指标名称',
  `indicator_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '指标类型',
  `parent_id` BIGINT NULL DEFAULT 0 COMMENT '父级指标 ID',
  `weight` DECIMAL(6,2) NULL DEFAULT 0.00 COMMENT '权重',
  `max_score` DECIMAL(8,2) NULL DEFAULT 100.00 COMMENT '满分',
  `score_desc` TEXT NULL COMMENT '评分说明',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_indicator_indicator_no` (`indicator_no`),
  KEY `idx_eval_indicator_parent_id` (`parent_id`),
  KEY `idx_eval_indicator_type` (`indicator_type`)
) ENGINE=InnoDB COMMENT='评价指标表；评价指标数据。';

-- 45. eval_task - 评价任务表
CREATE TABLE `eval_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_no` VARCHAR(64) NOT NULL COMMENT '评价任务编号',
  `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `task_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '任务类型',
  `target_type` VARCHAR(100) NOT NULL COMMENT '评价对象类型',
  `target_id` BIGINT NOT NULL COMMENT '评价对象 ID',
  `owner_id` BIGINT NULL DEFAULT NULL COMMENT '负责人 ID',
  `started_at` DATETIME NULL DEFAULT NULL COMMENT '开始时间',
  `ended_at` DATETIME NULL DEFAULT NULL COMMENT '结束时间',
  `task_status` VARCHAR(50) NULL DEFAULT 'draft' COMMENT '任务状态',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_task_task_no` (`task_no`),
  KEY `idx_eval_task_target` (`target_type`, `target_id`),
  KEY `idx_eval_task_status` (`task_status`)
) ENGINE=InnoDB COMMENT='评价任务表；评价任务数据。';

-- 46. eval_score_record - 评价评分记录表
CREATE TABLE `eval_score_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_id` BIGINT NOT NULL COMMENT '评价任务 ID',
  `indicator_id` BIGINT NOT NULL COMMENT '评价指标 ID',
  `evaluator_id` BIGINT NULL DEFAULT NULL COMMENT '评分人 ID',
  `score` DECIMAL(8,2) NULL DEFAULT NULL COMMENT '评分',
  `score_comment` VARCHAR(500) NULL DEFAULT NULL COMMENT '评分说明',
  `scored_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_score_record_task_indicator_evaluator` (`task_id`, `indicator_id`, `evaluator_id`),
  KEY `idx_eval_score_record_task_id` (`task_id`),
  KEY `idx_eval_score_record_evaluator_id` (`evaluator_id`)
) ENGINE=InnoDB COMMENT='评价评分记录表；评价评分记录数据。';

-- 47. eval_result - 评价结果表
CREATE TABLE `eval_result` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_id` BIGINT NOT NULL COMMENT '评价任务 ID',
  `total_score` DECIMAL(8,2) NULL DEFAULT NULL COMMENT '总分',
  `result_level` VARCHAR(50) NULL DEFAULT NULL COMMENT '评价等级',
  `result_desc` TEXT NULL COMMENT '评价结果说明',
  `confirmed_by` BIGINT NULL DEFAULT NULL COMMENT '确认人',
  `confirmed_at` DATETIME NULL DEFAULT NULL COMMENT '确认时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_result_task_id` (`task_id`),
  KEY `idx_eval_result_result_level` (`result_level`)
) ENGINE=InnoDB COMMENT='评价结果表；评价结果数据。';

-- 48. eval_application - 评价申报表
CREATE TABLE `eval_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `application_no` VARCHAR(64) NOT NULL COMMENT '申报编号',
  `application_title` VARCHAR(200) NOT NULL COMMENT '申报标题',
  `application_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '申报类型',
  `applicant_id` BIGINT NOT NULL COMMENT '申报人 ID',
  `review_status` VARCHAR(50) NULL DEFAULT 'draft' COMMENT '审核状态',
  `submitted_at` DATETIME NULL DEFAULT NULL COMMENT '提交时间',
  `reviewer_id` BIGINT NULL DEFAULT NULL COMMENT '当前审核人 ID',
  `reviewed_at` DATETIME NULL DEFAULT NULL COMMENT '最近审核时间',
  `review_comment` VARCHAR(500) NULL DEFAULT NULL COMMENT '最近审核意见',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_application_application_no` (`application_no`),
  KEY `idx_eval_application_applicant_id` (`applicant_id`),
  KEY `idx_eval_application_review_status_submitted_at` (`review_status`, `submitted_at`)
) ENGINE=InnoDB COMMENT='评价申报表；保存申报项目主信息、申报人、当前审核状态和最近审核结果。';

-- 49. eval_review_record - 申报审核记录表
CREATE TABLE `eval_review_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `application_id` BIGINT NOT NULL COMMENT '申报 ID',
  `reviewer_id` BIGINT NOT NULL COMMENT '审核人 ID',
  `review_action` VARCHAR(50) NOT NULL COMMENT '审核动作',
  `before_status` VARCHAR(50) NULL DEFAULT NULL COMMENT '审核前状态',
  `review_status` VARCHAR(50) NOT NULL COMMENT '审核后状态',
  `review_comment` VARCHAR(500) NULL DEFAULT NULL COMMENT '审核意见',
  `reviewed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_eval_review_record_application_id` (`application_id`),
  KEY `idx_eval_review_record_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB COMMENT='申报审核记录表；保存申报项目每一次审核动作和意见。';

-- 50. eval_attachment - 申报附件表
CREATE TABLE `eval_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `application_id` BIGINT NOT NULL COMMENT '申报 ID',
  `file_id` BIGINT NULL DEFAULT NULL COMMENT '文件资源 ID',
  `file_name` VARCHAR(255) NULL DEFAULT NULL COMMENT '文件名',
  `file_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '文件类型',
  `file_url` VARCHAR(500) NULL DEFAULT NULL COMMENT '文件地址兼容字段',
  `file_size` BIGINT NULL DEFAULT NULL COMMENT '文件大小',
  `uploader_id` BIGINT NULL DEFAULT NULL COMMENT '上传人 ID',
  `uploaded_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_eval_attachment_application_id` (`application_id`),
  KEY `idx_eval_attachment_file_id` (`file_id`)
) ENGINE=InnoDB COMMENT='申报附件表；保存申报材料附件清单。';

-- 51. eval_archive - 申报档案袋表
CREATE TABLE `eval_archive` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `archive_no` VARCHAR(64) NOT NULL COMMENT '档案编号',
  `application_id` BIGINT NULL DEFAULT NULL COMMENT '关联申报 ID',
  `archive_title` VARCHAR(200) NOT NULL COMMENT '档案标题',
  `owner_id` BIGINT NULL DEFAULT NULL COMMENT '档案所属人',
  `archive_status` VARCHAR(50) NULL DEFAULT 'draft' COMMENT '档案状态',
  `generated_at` DATETIME NULL DEFAULT NULL COMMENT '生成时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_archive_archive_no` (`archive_no`),
  KEY `idx_eval_archive_application_id` (`application_id`),
  KEY `idx_eval_archive_owner_id` (`owner_id`)
) ENGINE=InnoDB COMMENT='申报档案袋表；汇总药材、采集、图谱、评价、课程课题和附件形成申报档案袋。';

-- 52. eval_archive_item - 申报档案材料项表
CREATE TABLE `eval_archive_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `archive_id` BIGINT NOT NULL COMMENT '档案 ID',
  `source_type` VARCHAR(100) NOT NULL COMMENT '来源类型',
  `source_id` BIGINT NOT NULL COMMENT '来源记录 ID',
  `item_name` VARCHAR(200) NULL DEFAULT NULL COMMENT '材料名称',
  `item_desc` VARCHAR(500) NULL DEFAULT NULL COMMENT '材料说明',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_eval_archive_item_archive_id` (`archive_id`),
  KEY `idx_eval_archive_item_source` (`source_type`, `source_id`)
) ENGINE=InnoDB COMMENT='申报档案材料项表；保存档案袋中来自药材、采集、图谱、评价、课程、课题、文件等来源的材料项。';

-- 53. perf_standard - 业绩认定标准表
CREATE TABLE `perf_standard` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `standard_no` VARCHAR(64) NOT NULL COMMENT '标准编号',
  `standard_name` VARCHAR(150) NOT NULL COMMENT '标准名称',
  `performance_type` VARCHAR(50) NOT NULL COMMENT '业绩类型',
  `standard_desc` TEXT NULL COMMENT '标准说明',
  `score_rule` TEXT NULL COMMENT '评分或认定规则',
  `sort_order` INT NULL DEFAULT 0 COMMENT '排序号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perf_standard_standard_no` (`standard_no`),
  KEY `idx_perf_standard_performance_type` (`performance_type`)
) ENGINE=InnoDB COMMENT='业绩认定标准表；保存不同业绩类型的认定标准和评分规则。';

-- 54. perf_record - 业绩记录表
CREATE TABLE `perf_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `performance_no` VARCHAR(64) NOT NULL COMMENT '业绩编号',
  `user_id` BIGINT NOT NULL COMMENT '所属用户 ID',
  `performance_title` VARCHAR(200) NOT NULL COMMENT '业绩标题',
  `performance_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '业绩类型',
  `standard_id` BIGINT NULL DEFAULT NULL COMMENT '认定标准 ID',
  `source_type` VARCHAR(50) NULL DEFAULT NULL COMMENT '来源类型',
  `source_id` BIGINT NULL DEFAULT NULL COMMENT '来源对象 ID',
  `identify_status` VARCHAR(50) NULL DEFAULT 'draft' COMMENT '认定状态',
  `submitted_at` DATETIME NULL DEFAULT NULL COMMENT '提交时间',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perf_record_performance_no` (`performance_no`),
  KEY `idx_perf_record_user_id` (`user_id`),
  KEY `idx_perf_record_identify_status` (`identify_status`),
  KEY `idx_perf_record_source` (`source_type`, `source_id`)
) ENGINE=InnoDB COMMENT='业绩记录表；保存用户填报或由成果、申报、课程资源转化而来的业绩记录。';

-- 55. perf_identification - 业绩认定记录表
CREATE TABLE `perf_identification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `performance_id` BIGINT NOT NULL COMMENT '业绩 ID',
  `identifier_id` BIGINT NOT NULL COMMENT '认定人 ID',
  `identify_action` VARCHAR(50) NULL DEFAULT 'identify' COMMENT '认定动作',
  `identify_result` VARCHAR(50) NOT NULL COMMENT '认定结果',
  `identify_comment` VARCHAR(500) NULL DEFAULT NULL COMMENT '认定意见',
  `identified_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '认定时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_perf_identification_performance_id` (`performance_id`),
  KEY `idx_perf_identification_identifier_id` (`identifier_id`)
) ENGINE=InnoDB COMMENT='业绩认定记录表；保存业绩审核认定结果、意见、认定人和时间。';

-- 56. soap_sync_task - SOAP 同步任务表
CREATE TABLE `soap_sync_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_no` VARCHAR(64) NOT NULL COMMENT '任务编号',
  `task_name` VARCHAR(150) NOT NULL COMMENT '任务名称',
  `service_name` VARCHAR(150) NOT NULL COMMENT 'SOAP 服务名称',
  `method_name` VARCHAR(150) NULL DEFAULT NULL COMMENT 'SOAP 方法名称',
  `sync_direction` VARCHAR(50) NULL DEFAULT 'inbound' COMMENT '同步方向',
  `sync_status` VARCHAR(50) NULL DEFAULT 'pending' COMMENT '同步状态',
  `last_sync_at` DATETIME NULL DEFAULT NULL COMMENT '最近同步时间',
  `retry_count` INT NULL DEFAULT 0 COMMENT '重试次数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1 启用，0 禁用',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` BIGINT NULL DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` BIGINT NULL DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` DATETIME NULL DEFAULT NULL COMMENT '删除时间',
  `deleted_by` BIGINT NULL DEFAULT NULL COMMENT '删除人 ID',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_soap_sync_task_task_no` (`task_no`),
  KEY `idx_soap_sync_task_service_name` (`service_name`),
  KEY `idx_soap_sync_task_status` (`sync_status`)
) ENGINE=InnoDB COMMENT='SOAP 同步任务表；保存 SOAP 同步任务配置、同步方向、状态和最近同步时间。';

-- 57. soap_exchange_record - SOAP 交换记录表
CREATE TABLE `soap_exchange_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_id` BIGINT NULL DEFAULT NULL COMMENT '同步任务 ID',
  `exchange_no` VARCHAR(64) NOT NULL COMMENT '交换编号',
  `service_name` VARCHAR(150) NOT NULL COMMENT '服务名称',
  `method_name` VARCHAR(150) NULL DEFAULT NULL COMMENT '方法名称',
  `request_xml` LONGTEXT NULL COMMENT '请求 XML',
  `response_xml` LONGTEXT NULL COMMENT '响应 XML',
  `sync_status` VARCHAR(50) NULL DEFAULT 'pending' COMMENT '同步状态',
  `error_message` TEXT NULL COMMENT '错误信息',
  `called_by` BIGINT NULL DEFAULT NULL COMMENT '调用人 ID',
  `called_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_soap_exchange_record_exchange_no` (`exchange_no`),
  KEY `idx_soap_exchange_record_task_id` (`task_id`),
  KEY `idx_soap_exchange_record_called_at` (`called_at`)
) ENGINE=InnoDB COMMENT='SOAP 交换记录表；保存 SOAP 请求 XML、响应 XML、交换状态和错误信息。';

-- 58. stat_dashboard_snapshot - 首页看板统计快照表
CREATE TABLE `stat_dashboard_snapshot` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `snapshot_date` DATE NOT NULL COMMENT '统计日期',
  `herb_count` INT NULL DEFAULT 0 COMMENT '药材数量',
  `base_count` INT NULL DEFAULT 0 COMMENT '基地数量',
  `distribution_count` INT NULL DEFAULT 0 COMMENT '地图点位数量',
  `growth_record_count` INT NULL DEFAULT 0 COMMENT '采集记录数量',
  `course_count` INT NULL DEFAULT 0 COMMENT '课程数量',
  `pending_review_count` INT NULL DEFAULT 0 COMMENT '待审核数量',
  `dashboard_data` LONGTEXT NULL COMMENT '首页统计 JSON 快照',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` VARCHAR(500) NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_dashboard_snapshot_date` (`snapshot_date`)
) ENGINE=InnoDB COMMENT='首页看板统计快照表；保存首页看板统计快照，用于缓存药材数量、基地数量、采集数量、待办事项等统计数据。';

