-- MySQL dump 10.13  Distrib 8.0.46, for Linux (x86_64)
--
-- Host: localhost    Database: biomed_dev
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `auth_data_scope`
--

DROP TABLE IF EXISTS `auth_data_scope`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_data_scope` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `role_id` bigint NOT NULL COMMENT '角色 ID',
  `resource_type` varchar(100) NOT NULL COMMENT '资源类型，如 herb_growth_record、eval_application',
  `scope_type` varchar(50) NOT NULL COMMENT '范围类型：all、organization、department、self、custom',
  `organization_id` bigint DEFAULT NULL COMMENT '机构 ID',
  `department_id` bigint DEFAULT NULL COMMENT '部门 ID',
  `custom_rule` text COMMENT '自定义规则 JSON',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_data_scope_role_resource` (`role_id`,`resource_type`),
  KEY `idx_auth_data_scope_scope_type` (`scope_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据范围权限表；保存角色在不同业务资源上的数据可见范围。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auth_data_scope`
--

LOCK TABLES `auth_data_scope` WRITE;
/*!40000 ALTER TABLE `auth_data_scope` DISABLE KEYS */;
/*!40000 ALTER TABLE `auth_data_scope` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auth_menu`
--

DROP TABLE IF EXISTS `auth_menu`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单 ID',
  `menu_code` varchar(100) NOT NULL COMMENT '菜单编码',
  `menu_name` varchar(100) NOT NULL COMMENT '菜单名称',
  `route_path` varchar(255) DEFAULT NULL COMMENT '前端路由',
  `component_path` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `visible` tinyint DEFAULT '1' COMMENT '是否显示：0 否，1 是',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_menu_menu_code` (`menu_code`),
  KEY `idx_auth_menu_parent_id` (`parent_id`),
  KEY `idx_auth_menu_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='菜单表；保存前端菜单、路由和组件路径，支持树形菜单。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auth_menu`
--

LOCK TABLES `auth_menu` WRITE;
/*!40000 ALTER TABLE `auth_menu` DISABLE KEYS */;
/*!40000 ALTER TABLE `auth_menu` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auth_permission`
--

DROP TABLE IF EXISTS `auth_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `permission_code` varchar(100) NOT NULL COMMENT '权限编码，如 herb:species:add',
  `permission_name` varchar(100) NOT NULL COMMENT '权限名称',
  `permission_type` varchar(50) NOT NULL COMMENT '权限类型：menu、button、api',
  `menu_id` bigint DEFAULT NULL COMMENT '所属菜单 ID',
  `api_path` varchar(255) DEFAULT NULL COMMENT '接口路径',
  `request_method` varchar(20) DEFAULT NULL COMMENT '请求方法',
  `description` varchar(255) DEFAULT NULL COMMENT '权限说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_permission_permission_code` (`permission_code`),
  KEY `idx_auth_permission_menu_id` (`menu_id`),
  KEY `idx_auth_permission_api_path` (`api_path`),
  KEY `idx_auth_permission_type` (`permission_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限表；保存菜单、按钮、接口等权限点。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auth_permission`
--

LOCK TABLES `auth_permission` WRITE;
/*!40000 ALTER TABLE `auth_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `auth_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `auth_role`
--

DROP TABLE IF EXISTS `auth_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `auth_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `role_code` varchar(64) NOT NULL COMMENT '角色编码',
  `role_name` varchar(100) NOT NULL COMMENT '角色名称',
  `role_type` varchar(50) DEFAULT NULL COMMENT '角色类型',
  `data_scope` varchar(50) DEFAULT NULL COMMENT '简化数据范围：all、organization、department、self、custom',
  `description` varchar(255) DEFAULT NULL COMMENT '角色说明',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_auth_role_role_code` (`role_code`),
  KEY `idx_auth_role_status` (`status`),
  KEY `idx_auth_role_data_scope` (`data_scope`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色表；保存系统角色定义。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `auth_role`
--

LOCK TABLES `auth_role` WRITE;
/*!40000 ALTER TABLE `auth_role` DISABLE KEYS */;
INSERT INTO `auth_role` VALUES (1,'ADMIN','系统管理员','system','all','系统初始化管理员角色',1,1,0,'2026-07-12 21:23:43','2026-07-12 21:23:43',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `auth_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dict_item`
--

DROP TABLE IF EXISTS `dict_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dict_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `type_id` bigint NOT NULL COMMENT '字典类型 ID',
  `item_code` varchar(64) NOT NULL COMMENT '字典项编码',
  `item_name` varchar(100) NOT NULL COMMENT '字典项名称',
  `item_value` varchar(100) DEFAULT NULL COMMENT '字典项值',
  `parent_id` bigint DEFAULT '0' COMMENT '父字典项 ID',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_item_type_id_item_code` (`type_id`,`item_code`),
  KEY `idx_dict_item_type_id` (`type_id`),
  KEY `idx_dict_item_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典项表；保存具体字典项。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dict_item`
--

LOCK TABLES `dict_item` WRITE;
/*!40000 ALTER TABLE `dict_item` DISABLE KEYS */;
/*!40000 ALTER TABLE `dict_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dict_region`
--

DROP TABLE IF EXISTS `dict_region`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dict_region` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `region_code` varchar(64) NOT NULL COMMENT '区域编码',
  `region_name` varchar(100) NOT NULL COMMENT '区域名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父级区域 ID',
  `region_level` varchar(50) DEFAULT NULL COMMENT '区域级别：province、city、district、town、base_area',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_region_region_code` (`region_code`),
  KEY `idx_dict_region_parent_id` (`parent_id`),
  KEY `idx_dict_region_level` (`region_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='区域字典表；保存省、市、区县、乡镇、基地区域等地理层级。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dict_region`
--

LOCK TABLES `dict_region` WRITE;
/*!40000 ALTER TABLE `dict_region` DISABLE KEYS */;
/*!40000 ALTER TABLE `dict_region` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `dict_type`
--

DROP TABLE IF EXISTS `dict_type`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `dict_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `type_code` varchar(64) NOT NULL COMMENT '字典类型编码',
  `type_name` varchar(100) NOT NULL COMMENT '字典类型名称',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dict_type_type_code` (`type_code`),
  KEY `idx_dict_type_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='字典类型表；保存字典分类，如 herb_category、growth_stage、review_status。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `dict_type`
--

LOCK TABLES `dict_type` WRITE;
/*!40000 ALTER TABLE `dict_type` DISABLE KEYS */;
/*!40000 ALTER TABLE `dict_type` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_course`
--

DROP TABLE IF EXISTS `edu_course`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_course` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `course_no` varchar(64) NOT NULL COMMENT '课程编号',
  `course_name` varchar(150) NOT NULL COMMENT '课程名称',
  `course_type` varchar(50) DEFAULT NULL COMMENT '课程类型',
  `teacher_id` bigint DEFAULT NULL COMMENT '授课教师 ID',
  `description` text COMMENT '课程简介',
  `video_url` varchar(500) DEFAULT NULL COMMENT '视频链接',
  `publish_status` varchar(50) DEFAULT 'draft' COMMENT '发布状态',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `ended_at` datetime DEFAULT NULL COMMENT '结束时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edu_course_course_no` (`course_no`),
  KEY `idx_edu_course_teacher_id` (`teacher_id`),
  KEY `idx_edu_course_publish_status` (`publish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验课程表；保存实验课程、培训课程和课程发布信息。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_course`
--

LOCK TABLES `edu_course` WRITE;
/*!40000 ALTER TABLE `edu_course` DISABLE KEYS */;
/*!40000 ALTER TABLE `edu_course` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_course_resource`
--

DROP TABLE IF EXISTS `edu_course_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_course_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `course_id` bigint NOT NULL COMMENT '课程 ID',
  `resource_name` varchar(150) NOT NULL COMMENT '资源名称',
  `resource_type` varchar(50) DEFAULT NULL COMMENT '资源类型',
  `file_id` bigint DEFAULT NULL COMMENT '统一文件资源 ID',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件地址兼容字段',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `file_format` varchar(50) DEFAULT NULL COMMENT '文件格式',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传人 ID',
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `download_count` int DEFAULT '0' COMMENT '下载次数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_edu_course_resource_course_id` (`course_id`),
  KEY `idx_edu_course_resource_type` (`resource_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教学课程资源表；保存课件、文档、视频、实验指导书等课程资源。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_course_resource`
--

LOCK TABLES `edu_course_resource` WRITE;
/*!40000 ALTER TABLE `edu_course_resource` DISABLE KEYS */;
/*!40000 ALTER TABLE `edu_course_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_experiment_record`
--

DROP TABLE IF EXISTS `edu_experiment_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_experiment_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `record_no` varchar(64) NOT NULL COMMENT '实验记录编号',
  `course_id` bigint DEFAULT NULL COMMENT '课程 ID',
  `project_id` bigint DEFAULT NULL COMMENT '课题 ID',
  `experiment_title` varchar(200) NOT NULL COMMENT '实验标题',
  `experiment_process` text COMMENT '实验过程',
  `experiment_result` text COMMENT '实验结果',
  `recorder_id` bigint DEFAULT NULL COMMENT '记录人 ID',
  `recorded_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '记录时间',
  `archive_status` varchar(50) DEFAULT 'draft' COMMENT '归档状态',
  `archived_at` datetime DEFAULT NULL COMMENT '归档时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edu_experiment_record_record_no` (`record_no`),
  KEY `idx_edu_experiment_record_course_id` (`course_id`),
  KEY `idx_edu_experiment_record_project_id` (`project_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验记录表；记录课程或课题下的实验过程、结果、附件和归档信息。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_experiment_record`
--

LOCK TABLES `edu_experiment_record` WRITE;
/*!40000 ALTER TABLE `edu_experiment_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `edu_experiment_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_experiment_step`
--

DROP TABLE IF EXISTS `edu_experiment_step`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_experiment_step` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `course_id` bigint NOT NULL COMMENT '课程 ID',
  `step_no` varchar(50) NOT NULL COMMENT '步骤编号',
  `step_title` varchar(200) NOT NULL COMMENT '步骤标题',
  `step_content` text COMMENT '步骤内容',
  `expected_result` text COMMENT '预期结果',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edu_experiment_step_course_step_no` (`course_id`,`step_no`),
  KEY `idx_edu_experiment_step_course_id` (`course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实验步骤表；保存课程实验步骤、步骤内容和预期结果。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_experiment_step`
--

LOCK TABLES `edu_experiment_step` WRITE;
/*!40000 ALTER TABLE `edu_experiment_step` DISABLE KEYS */;
/*!40000 ALTER TABLE `edu_experiment_step` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_training_feedback`
--

DROP TABLE IF EXISTS `edu_training_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_training_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `training_record_id` bigint NOT NULL COMMENT '培训记录 ID',
  `user_id` bigint NOT NULL COMMENT '反馈用户 ID',
  `rating` decimal(4,2) DEFAULT NULL COMMENT '评分',
  `feedback_content` text COMMENT '反馈内容',
  `submitted_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_edu_training_feedback_record_id` (`training_record_id`),
  KEY `idx_edu_training_feedback_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训反馈表；保存培训参与人员的反馈评价。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_training_feedback`
--

LOCK TABLES `edu_training_feedback` WRITE;
/*!40000 ALTER TABLE `edu_training_feedback` DISABLE KEYS */;
/*!40000 ALTER TABLE `edu_training_feedback` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_training_plan`
--

DROP TABLE IF EXISTS `edu_training_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_training_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `plan_no` varchar(64) NOT NULL COMMENT '培训计划编号',
  `plan_name` varchar(200) NOT NULL COMMENT '培训计划名称',
  `plan_type` varchar(50) DEFAULT NULL COMMENT '培训类型',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人 ID',
  `description` text COMMENT '培训说明',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `ended_at` datetime DEFAULT NULL COMMENT '结束时间',
  `publish_status` varchar(50) DEFAULT 'draft' COMMENT '发布状态',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_edu_training_plan_plan_no` (`plan_no`),
  KEY `idx_edu_training_plan_owner_id` (`owner_id`),
  KEY `idx_edu_training_plan_publish_status` (`publish_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训计划表；保存培训计划、时间、负责人和培训目标。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_training_plan`
--

LOCK TABLES `edu_training_plan` WRITE;
/*!40000 ALTER TABLE `edu_training_plan` DISABLE KEYS */;
/*!40000 ALTER TABLE `edu_training_plan` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `edu_training_record`
--

DROP TABLE IF EXISTS `edu_training_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `edu_training_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` bigint NOT NULL COMMENT '参与用户 ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程 ID',
  `plan_id` bigint DEFAULT NULL COMMENT '培训计划 ID',
  `progress` decimal(5,2) DEFAULT '0.00' COMMENT '学习进度',
  `training_status` varchar(50) DEFAULT 'not_started' COMMENT '状态',
  `score` decimal(5,2) DEFAULT NULL COMMENT '成绩',
  `started_at` datetime DEFAULT NULL COMMENT '开始学习时间',
  `completed_at` datetime DEFAULT NULL COMMENT '完成时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_edu_training_record_user_id` (`user_id`),
  KEY `idx_edu_training_record_course_id` (`course_id`),
  KEY `idx_edu_training_record_plan_id` (`plan_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='培训参与记录表；保存用户参与课程或培训计划的进度、成绩和状态。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `edu_training_record`
--

LOCK TABLES `edu_training_record` WRITE;
/*!40000 ALTER TABLE `edu_training_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `edu_training_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eval_application`
--

DROP TABLE IF EXISTS `eval_application`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eval_application` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `application_no` varchar(64) NOT NULL COMMENT '申报编号',
  `application_title` varchar(200) NOT NULL COMMENT '申报标题',
  `application_type` varchar(50) DEFAULT NULL COMMENT '申报类型',
  `applicant_id` bigint NOT NULL COMMENT '申报人 ID',
  `review_status` varchar(50) DEFAULT 'draft' COMMENT '审核状态',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `reviewer_id` bigint DEFAULT NULL COMMENT '当前审核人 ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '最近审核时间',
  `review_comment` varchar(500) DEFAULT NULL COMMENT '最近审核意见',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_application_application_no` (`application_no`),
  KEY `idx_eval_application_applicant_id` (`applicant_id`),
  KEY `idx_eval_application_review_status_submitted_at` (`review_status`,`submitted_at`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价申报表；保存申报项目主信息、申报人、当前审核状态和最近审核结果。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eval_application`
--

LOCK TABLES `eval_application` WRITE;
/*!40000 ALTER TABLE `eval_application` DISABLE KEYS */;
INSERT INTO `eval_application` VALUES (1,'DECL-20260710155318-c1fa04','川芎道地药材非遗申报','intangible_heritage',1,'approved','2026-07-10 15:59:52',2,'2026-07-10 16:00:31','材料完整，审核通过',1,0,'2026-07-10 15:53:19','2026-07-10 15:53:19',1,2,NULL,NULL,'M17后端接口测试',2);
/*!40000 ALTER TABLE `eval_application` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eval_archive`
--

DROP TABLE IF EXISTS `eval_archive`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eval_archive` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `archive_no` varchar(64) NOT NULL COMMENT '档案编号',
  `application_id` bigint DEFAULT NULL COMMENT '关联申报 ID',
  `archive_title` varchar(200) NOT NULL COMMENT '档案标题',
  `owner_id` bigint DEFAULT NULL COMMENT '档案所属人',
  `archive_status` varchar(50) DEFAULT 'draft' COMMENT '档案状态',
  `generated_at` datetime DEFAULT NULL COMMENT '生成时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_archive_archive_no` (`archive_no`),
  KEY `idx_eval_archive_application_id` (`application_id`),
  KEY `idx_eval_archive_owner_id` (`owner_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申报档案袋表；汇总药材、采集、图谱、评价、课程课题和附件形成申报档案袋。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eval_archive`
--

LOCK TABLES `eval_archive` WRITE;
/*!40000 ALTER TABLE `eval_archive` DISABLE KEYS */;
INSERT INTO `eval_archive` VALUES (1,'ARCH-20260710160031-5541ad',1,'川芎道地药材非遗申报档案袋',1,'generated','2026-07-10 16:00:31',1,0,'2026-07-10 16:00:31','2026-07-10 16:00:31',1,2,NULL,NULL,'由申报档案生成',0);
/*!40000 ALTER TABLE `eval_archive` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eval_archive_item`
--

DROP TABLE IF EXISTS `eval_archive_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eval_archive_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `archive_id` bigint NOT NULL COMMENT '档案 ID',
  `source_type` varchar(100) NOT NULL COMMENT '来源类型',
  `source_id` bigint NOT NULL COMMENT '来源记录 ID',
  `item_name` varchar(200) DEFAULT NULL COMMENT '材料名称',
  `item_desc` varchar(500) DEFAULT NULL COMMENT '材料说明',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_eval_archive_item_archive_id` (`archive_id`),
  KEY `idx_eval_archive_item_source` (`source_type`,`source_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申报档案材料项表；保存档案袋中来自药材、采集、图谱、评价、课程、课题、文件等来源的材料项。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eval_archive_item`
--

LOCK TABLES `eval_archive_item` WRITE;
/*!40000 ALTER TABLE `eval_archive_item` DISABLE KEYS */;
INSERT INTO `eval_archive_item` VALUES (1,1,'attachment',1,'川芎药材质量评价报告.pdf','质量评价佐证材料',0,'2026-07-10 16:00:31',1,NULL),(2,1,'attachment',2,'川芎采集照片.zip','补充采集图片材料',0,'2026-07-10 16:06:13',1,NULL),(3,1,'evaluation_result',1,'评价结果汇总','来自M16评价结果确认后的汇总材料',10,'2026-07-10 16:07:16',2,'手动关联评价结果');
/*!40000 ALTER TABLE `eval_archive_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eval_attachment`
--

DROP TABLE IF EXISTS `eval_attachment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eval_attachment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `application_id` bigint NOT NULL COMMENT '申报 ID',
  `file_id` bigint DEFAULT NULL COMMENT '文件资源 ID',
  `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型',
  `file_url` varchar(500) DEFAULT NULL COMMENT '文件地址兼容字段',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传人 ID',
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_eval_attachment_application_id` (`application_id`),
  KEY `idx_eval_attachment_file_id` (`file_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申报附件表；保存申报材料附件清单。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eval_attachment`
--

LOCK TABLES `eval_attachment` WRITE;
/*!40000 ALTER TABLE `eval_attachment` DISABLE KEYS */;
INSERT INTO `eval_attachment` VALUES (1,1,1,'川芎药材质量评价报告.pdf','document','/files/test/chuanxiong-report.pdf',204800,1,'2026-07-10 15:58:45',1,0,'2026-07-10 15:58:44','2026-07-10 15:58:44',1,NULL,NULL,NULL,'质量评价佐证材料',0),(2,1,2,'川芎采集照片.zip','material','/files/test/chuanxiong-images.zip',1024000,1,'2026-07-10 16:06:13',1,0,'2026-07-10 16:06:13','2026-07-10 16:06:13',1,NULL,NULL,NULL,'补充采集图片材料',0);
/*!40000 ALTER TABLE `eval_attachment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eval_indicator`
--

DROP TABLE IF EXISTS `eval_indicator`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eval_indicator` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `indicator_no` varchar(64) NOT NULL COMMENT '指标编号',
  `indicator_name` varchar(150) NOT NULL COMMENT '指标名称',
  `indicator_type` varchar(50) DEFAULT NULL COMMENT '指标类型',
  `parent_id` bigint DEFAULT '0' COMMENT '父级指标 ID',
  `weight` decimal(6,2) DEFAULT '0.00' COMMENT '权重',
  `max_score` decimal(8,2) DEFAULT '100.00' COMMENT '满分',
  `score_desc` text COMMENT '评分说明',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_indicator_indicator_no` (`indicator_no`),
  KEY `idx_eval_indicator_parent_id` (`parent_id`),
  KEY `idx_eval_indicator_type` (`indicator_type`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价指标表；评价指标数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eval_indicator`
--

LOCK TABLES `eval_indicator` WRITE;
/*!40000 ALTER TABLE `eval_indicator` DISABLE KEYS */;
INSERT INTO `eval_indicator` VALUES (1,'EVAL-IND-20260710141925-82cf7b','药材品质评分','quality',0,60.00,100.00,'从外观、气味、成分等方面评分',1,1,0,'2026-07-10 14:19:25','2026-07-10 14:19:25',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `eval_indicator` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eval_result`
--

DROP TABLE IF EXISTS `eval_result`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eval_result` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_id` bigint NOT NULL COMMENT '评价任务 ID',
  `total_score` decimal(8,2) DEFAULT NULL COMMENT '总分',
  `result_level` varchar(50) DEFAULT NULL COMMENT '评价等级',
  `result_desc` text COMMENT '评价结果说明',
  `confirmed_by` bigint DEFAULT NULL COMMENT '确认人',
  `confirmed_at` datetime DEFAULT NULL COMMENT '确认时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_result_task_id` (`task_id`),
  KEY `idx_eval_result_result_level` (`result_level`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价结果表；评价结果数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eval_result`
--

LOCK TABLES `eval_result` WRITE;
/*!40000 ALTER TABLE `eval_result` DISABLE KEYS */;
INSERT INTO `eval_result` VALUES (1,1,52.80,'unqualified','评价通过，可作为申报材料引用',1,'2026-07-10 15:12:45',1,0,'2026-07-10 15:12:45','2026-07-10 15:12:45',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `eval_result` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eval_review_record`
--

DROP TABLE IF EXISTS `eval_review_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eval_review_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `application_id` bigint NOT NULL COMMENT '申报 ID',
  `reviewer_id` bigint NOT NULL COMMENT '审核人 ID',
  `review_action` varchar(50) NOT NULL COMMENT '审核动作',
  `before_status` varchar(50) DEFAULT NULL COMMENT '审核前状态',
  `review_status` varchar(50) NOT NULL COMMENT '审核后状态',
  `review_comment` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `reviewed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_eval_review_record_application_id` (`application_id`),
  KEY `idx_eval_review_record_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='申报审核记录表；保存申报项目每一次审核动作和意见。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eval_review_record`
--

LOCK TABLES `eval_review_record` WRITE;
/*!40000 ALTER TABLE `eval_review_record` DISABLE KEYS */;
INSERT INTO `eval_review_record` VALUES (1,1,1,'submit','draft','submitted','申报提交','2026-07-10 15:59:52','2026-07-10 15:59:52',1,NULL),(2,1,2,'approve','submitted','approved','材料完整，审核通过','2026-07-10 16:00:31','2026-07-10 16:00:31',2,'Apifox审核测试');
/*!40000 ALTER TABLE `eval_review_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eval_score_record`
--

DROP TABLE IF EXISTS `eval_score_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eval_score_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_id` bigint NOT NULL COMMENT '评价任务 ID',
  `indicator_id` bigint NOT NULL COMMENT '评价指标 ID',
  `evaluator_id` bigint DEFAULT NULL COMMENT '评分人 ID',
  `score` decimal(8,2) DEFAULT NULL COMMENT '评分',
  `score_comment` varchar(500) DEFAULT NULL COMMENT '评分说明',
  `scored_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '评分时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_score_record_task_indicator_evaluator` (`task_id`,`indicator_id`,`evaluator_id`),
  KEY `idx_eval_score_record_task_id` (`task_id`),
  KEY `idx_eval_score_record_evaluator_id` (`evaluator_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价评分记录表；评价评分记录数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eval_score_record`
--

LOCK TABLES `eval_score_record` WRITE;
/*!40000 ALTER TABLE `eval_score_record` DISABLE KEYS */;
INSERT INTO `eval_score_record` VALUES (1,1,1,1,88.00,'整体质量较好，性状稳定','2026-07-10 14:37:20',1,0,'2026-07-10 14:37:19','2026-07-10 14:37:19',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `eval_score_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `eval_task`
--

DROP TABLE IF EXISTS `eval_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `eval_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_no` varchar(64) NOT NULL COMMENT '评价任务编号',
  `task_name` varchar(200) NOT NULL COMMENT '任务名称',
  `task_type` varchar(50) DEFAULT NULL COMMENT '任务类型',
  `target_type` varchar(100) NOT NULL COMMENT '评价对象类型',
  `target_id` bigint NOT NULL COMMENT '评价对象 ID',
  `owner_id` bigint DEFAULT NULL COMMENT '负责人 ID',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `ended_at` datetime DEFAULT NULL COMMENT '结束时间',
  `task_status` varchar(50) DEFAULT 'draft' COMMENT '任务状态',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_task_task_no` (`task_no`),
  KEY `idx_eval_task_target` (`target_type`,`target_id`),
  KEY `idx_eval_task_status` (`task_status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='评价任务表；评价任务数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `eval_task`
--

LOCK TABLES `eval_task` WRITE;
/*!40000 ALTER TABLE `eval_task` DISABLE KEYS */;
INSERT INTO `eval_task` VALUES (1,'EVAL-TASK-20260710142608-c6b08c','黄连品质评价任务','herb_quality','herb',1,1,NULL,NULL,'confirmed',1,0,'2026-07-10 14:26:08','2026-07-10 14:26:08',NULL,NULL,NULL,NULL,NULL,2);
/*!40000 ALTER TABLE `eval_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `flyway_schema_history`
--

DROP TABLE IF EXISTS `flyway_schema_history`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `flyway_schema_history` (
  `installed_rank` int NOT NULL,
  `version` varchar(50) DEFAULT NULL,
  `description` varchar(200) NOT NULL,
  `type` varchar(20) NOT NULL,
  `script` varchar(1000) NOT NULL,
  `checksum` int DEFAULT NULL,
  `installed_by` varchar(100) NOT NULL,
  `installed_on` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `execution_time` int NOT NULL,
  `success` tinyint(1) NOT NULL,
  PRIMARY KEY (`installed_rank`),
  KEY `flyway_schema_history_s_idx` (`success`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `flyway_schema_history`
--

LOCK TABLES `flyway_schema_history` WRITE;
/*!40000 ALTER TABLE `flyway_schema_history` DISABLE KEYS */;
INSERT INTO `flyway_schema_history` VALUES (1,'20260708.001','init schema','SQL','V20260708_001__init_schema.sql',114204287,'bdis','2026-07-09 08:04:34',1617,1);
/*!40000 ALTER TABLE `flyway_schema_history` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_ai_model_version`
--

DROP TABLE IF EXISTS `herb_ai_model_version`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_ai_model_version` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `model_code` varchar(100) NOT NULL COMMENT '模型编码',
  `model_name` varchar(100) NOT NULL COMMENT '模型名称',
  `model_type` varchar(50) NOT NULL COMMENT '模型类型',
  `model_version` varchar(100) DEFAULT NULL COMMENT '模型版本号',
  `provider` varchar(100) DEFAULT NULL COMMENT '模型提供方',
  `api_endpoint` varchar(500) DEFAULT NULL COMMENT '模型服务地址',
  `feature_dim` int DEFAULT NULL COMMENT '特征向量维度',
  `is_default` tinyint DEFAULT '0' COMMENT '是否默认模型',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_ai_model_version_model_code` (`model_code`),
  KEY `idx_herb_ai_model_version_model_type` (`model_type`),
  KEY `idx_herb_ai_model_version_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='AI 模型版本表；记录图像识别模型、特征提取模型和图谱比对模型版本。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_ai_model_version`
--

LOCK TABLES `herb_ai_model_version` WRITE;
/*!40000 ALTER TABLE `herb_ai_model_version` DISABLE KEYS */;
/*!40000 ALTER TABLE `herb_ai_model_version` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_atlas`
--

DROP TABLE IF EXISTS `herb_atlas`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_atlas` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `atlas_no` varchar(50) NOT NULL COMMENT '标准图谱编号',
  `species_id` bigint NOT NULL COMMENT '药材品种 ID',
  `herb_name` varchar(100) NOT NULL COMMENT '药材名称冗余展示字段',
  `atlas_title` varchar(255) NOT NULL COMMENT '图谱标题',
  `image_url` varchar(500) NOT NULL COMMENT '标准图谱图片地址',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图',
  `image_type` varchar(50) NOT NULL COMMENT '图片类型',
  `growth_stage` varchar(50) DEFAULT NULL COMMENT '生长阶段',
  `medicinal_part` varchar(50) DEFAULT NULL COMMENT '药用部位',
  `health_status` varchar(50) DEFAULT 'normal' COMMENT '健康状态',
  `form_type` varchar(50) DEFAULT NULL COMMENT '形态',
  `color_feature` varchar(255) DEFAULT NULL COMMENT '颜色特征',
  `texture_feature` varchar(255) DEFAULT NULL COMMENT '纹理特征',
  `shape_feature` varchar(255) DEFAULT NULL COMMENT '形态特征',
  `identification_points` text COMMENT '人工识别要点',
  `region_id` bigint DEFAULT NULL COMMENT '采集区域 ID',
  `collector_id` bigint DEFAULT NULL COMMENT '采集人 ID',
  `collected_at` datetime DEFAULT NULL COMMENT '采集时间',
  `source_type` varchar(50) DEFAULT 'manual' COMMENT '来源类型',
  `source_url` varchar(500) DEFAULT NULL COMMENT '来源 URL',
  `license_desc` varchar(255) DEFAULT NULL COMMENT '授权说明',
  `has_watermark` tinyint DEFAULT '0' COMMENT '是否有水印',
  `image_quality` varchar(50) DEFAULT 'normal' COMMENT '图片质量',
  `usable_for_feature` tinyint DEFAULT '1' COMMENT '是否参与特征比对',
  `feature_vector` longtext COMMENT '图像特征向量 JSON',
  `feature_dim` int DEFAULT NULL COMMENT '向量维度',
  `feature_model_id` bigint DEFAULT NULL COMMENT '特征模型 ID',
  `quality_status` varchar(50) DEFAULT 'available' COMMENT '图谱质量状态',
  `knowledge_entity_id` bigint DEFAULT NULL COMMENT '知识图谱实体 ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_atlas_atlas_no` (`atlas_no`),
  KEY `idx_herb_atlas_species_id` (`species_id`),
  KEY `idx_herb_atlas_image_type_growth_stage` (`image_type`,`growth_stage`),
  KEY `idx_herb_atlas_usable_for_feature` (`usable_for_feature`)
) ENGINE=InnoDB AUTO_INCREMENT=46 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中药材标准图谱表；保存标准图片、结构化特征、识别要点、来源信息和特征向量。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_atlas`
--

LOCK TABLES `herb_atlas` WRITE;
/*!40000 ALTER TABLE `herb_atlas` DISABLE KEYS */;
INSERT INTO `herb_atlas` VALUES (1,'ATLAS_DANGSHEN_001',8,'党参','dangshen_flower_flowering_fresh_01.jpeg','/api/files/uploads/2026-07-12/e8c5d15a-5c65-46d2-bcc2-7b7c2ab09c0c.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(2,'ATLAS_DANGSHEN_002',8,'党参','dangshen_flower_flowering_fresh_02.jpeg','/api/files/uploads/2026-07-12/935db8e7-c5d4-4aad-aacc-de9c11e4958c.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(3,'ATLAS_DANGSHEN_003',8,'党参','dangshen_flower_flowering_fresh_03.png','/api/files/uploads/2026-07-12/6115d751-fb7a-453a-8cfa-a64212103058.png',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(4,'ATLAS_DANGSHEN_004',8,'党参','dangshen_root_dried_01.jpeg','/api/files/uploads/2026-07-12/a8b09049-27e4-4d94-ae3c-d28477b5f414.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(5,'ATLAS_DANGSHEN_005',8,'党参','dangshen_root_dried_02.jpeg','/api/files/uploads/2026-07-12/dbcf752f-442f-4261-9e56-99bbf9b3eb18.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(6,'ATLAS_DANGSHEN_006',8,'党参','dangshen_root_dried_03.png','/api/files/uploads/2026-07-12/f301286b-7c0c-41b7-9511-377977851c36.png',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(7,'ATLAS_DANGSHEN_007',8,'党参','dangshen_whole_growth_fresh_01.jpeg','/api/files/uploads/2026-07-12/3c2ba487-b846-4aac-a12a-1a42a9992c9c.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(8,'ATLAS_DANGSHEN_008',8,'党参','dangshen_whole_growth_fresh_02.jpeg','/api/files/uploads/2026-07-12/7c18fb6e-123c-496b-a410-86f36434d868.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(9,'ATLAS_DANGSHEN_009',8,'党参','dangshen_whole_growth_fresh_03.jpeg','/api/files/uploads/2026-07-12/1587916b-4f7e-4cb2-8b5b-fe6c196b48f9.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(10,'ATLAS_GOUQI_001',9,'枸杞','gouqi_dried_fruit_01.jpeg','/api/files/uploads/2026-07-12/9a3265a7-2f8b-4bd4-8f19-5f025d9a82c3.jpeg',NULL,'standard','unknown','果实','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(11,'ATLAS_GOUQI_002',9,'枸杞','gouqi_dried_fruit_02.jpeg','/api/files/uploads/2026-07-12/3444ae1d-dc3d-40b2-a730-ab184b7bddee.jpeg',NULL,'standard','unknown','果实','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(12,'ATLAS_GOUQI_003',9,'枸杞','gouqi_dried_fruit_03.jpeg','/api/files/uploads/2026-07-12/53ffaa90-ccdc-44d4-8771-86ebf7ffa305.jpeg',NULL,'standard','unknown','果实','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(13,'ATLAS_GOUQI_004',9,'枸杞','gouqi_fruit_fresh_01.jpeg','/api/files/uploads/2026-07-12/2724fe92-70e8-49ff-b6a6-13bdcce370ce.jpeg',NULL,'standard','unknown','果实','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(14,'ATLAS_GOUQI_005',9,'枸杞','gouqi_fruit_fresh_02.jpeg','/api/files/uploads/2026-07-12/80cbdca9-82af-4a3d-a7ab-9c4b3cbc79c3.jpeg',NULL,'standard','unknown','果实','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(15,'ATLAS_GOUQI_006',9,'枸杞','gouqi_fruit_fresh_03.png','/api/files/uploads/2026-07-12/5ce466f3-1e27-4aef-a56c-4889e6d60a40.png',NULL,'standard','unknown','果实','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(16,'ATLAS_GOUQI_007',9,'枸杞','gouqi_whole_growth_fresh_01.jpeg','/api/files/uploads/2026-07-12/ac45f845-4acc-49b8-a63e-f7b495b8ef2f.jpeg',NULL,'standard','unknown','果实','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(17,'ATLAS_GOUQI_008',9,'枸杞','gouqi_whole_growth_fresh_02.jpeg','/api/files/uploads/2026-07-12/76c5589c-4a60-46f7-81e0-3705d403ce77.jpeg',NULL,'standard','unknown','果实','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(18,'ATLAS_GOUQI_009',9,'枸杞','gouqi_whole_growth_fresh_03.jpeg','/api/files/uploads/2026-07-12/849a3482-f28d-4d7f-9393-6bb481310ec7.jpeg',NULL,'standard','unknown','果实','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(19,'ATLAS_HUANGLIAN_001',10,'黄连','huanglian_leaf_growth_fresh_01.png','/api/files/uploads/2026-07-12/c69cd1ff-90ed-40f1-b99e-aa7b53226eae.png',NULL,'standard','unknown','根茎','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(20,'ATLAS_HUANGLIAN_002',10,'黄连','huanglian_leaf_growth_fresh_02.jpeg','/api/files/uploads/2026-07-12/094209d1-caa0-4f8e-854b-d88f7743d203.jpeg',NULL,'standard','unknown','根茎','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(21,'ATLAS_HUANGLIAN_003',10,'黄连','huanglian_leaf_growth_fresh_03.jpeg','/api/files/uploads/2026-07-12/400b671d-45c4-4cd5-969d-d117e1fcefaf.jpeg',NULL,'standard','unknown','根茎','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(22,'ATLAS_HUANGLIAN_004',10,'黄连','huanglian_rhizome_dried_01.png','/api/files/uploads/2026-07-12/4e5ad402-82b0-4327-bbfc-f6633a597ed3.png',NULL,'standard','unknown','根茎','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(23,'ATLAS_HUANGLIAN_005',10,'黄连','huanglian_rhizome_dried_02.png','/api/files/uploads/2026-07-12/4783c786-26a2-48cd-8675-514025b4352d.png',NULL,'standard','unknown','根茎','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(24,'ATLAS_HUANGLIAN_006',10,'黄连','huanglian_rhizome_dried_03.jpeg','/api/files/uploads/2026-07-12/663e95bc-2385-42a6-9040-e68ff24118a4.jpeg',NULL,'standard','unknown','根茎','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(25,'ATLAS_HUANGLIAN_007',10,'黄连','huanglian_whole_growth_fresh_01.png','/api/files/uploads/2026-07-12/184b5359-e150-4ebc-a866-76c022e0e293.png',NULL,'standard','unknown','根茎','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(26,'ATLAS_HUANGLIAN_008',10,'黄连','huanglian_whole_growth_fresh_02.jpeg','/api/files/uploads/2026-07-12/2966ba19-ecfd-4974-a1e3-a14d4db3ff34.jpeg',NULL,'standard','unknown','根茎','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(27,'ATLAS_HUANGLIAN_009',10,'黄连','huanglian_whole_growth_fresh_03.jpeg','/api/files/uploads/2026-07-12/59768068-8fe5-49c5-9d4f-93615f2707b1.jpeg',NULL,'standard','unknown','根茎','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(28,'ATLAS_HUANGQI_001',11,'黄芪','huangqi_flower_flowering_fresh_01.jpeg','/api/files/uploads/2026-07-12/c6fc3338-7c01-4568-98a1-6819df36862e.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(29,'ATLAS_HUANGQI_002',11,'黄芪','huangqi_flower_flowering_fresh_02.jpeg','/api/files/uploads/2026-07-12/c552b531-d725-48eb-a462-a592a4e1ecd3.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(30,'ATLAS_HUANGQI_003',11,'黄芪','huangqi_flower_flowering_fresh_03.jpeg','/api/files/uploads/2026-07-12/9b356ea9-bef0-4d66-987f-db7e0190c85e.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(31,'ATLAS_HUANGQI_004',11,'黄芪','huangqi_root_dried_01.png','/api/files/uploads/2026-07-12/ef3b16fa-7754-4970-9791-c5e8be0eb134.png',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(32,'ATLAS_HUANGQI_005',11,'黄芪','huangqi_root_dried_02.jpeg','/api/files/uploads/2026-07-12/a69428f4-012a-4f05-838e-21d7b37b85d5.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(33,'ATLAS_HUANGQI_006',11,'黄芪','huangqi_root_dried_03.png','/api/files/uploads/2026-07-12/db3491c2-b943-4777-baf6-0320810e1c0c.png',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(34,'ATLAS_HUANGQI_007',11,'黄芪','huangqi_whole_growth_fresh_01.png','/api/files/uploads/2026-07-12/4775b8a3-5a13-43fc-ade7-1cb930a8f45d.png',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(35,'ATLAS_HUANGQI_008',11,'黄芪','huangqi_whole_growth_fresh_02.jpeg','/api/files/uploads/2026-07-12/a16e9860-19eb-4298-be90-ed95e8af7480.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(36,'ATLAS_HUANGQI_009',11,'黄芪','huangqi_whole_growth_fresh_03.jpeg','/api/files/uploads/2026-07-12/72931f05-90f3-4e8c-b391-adc82df7382f.jpeg',NULL,'standard','unknown','根','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(37,'ATLAS_JINYINHUA_001',12,'金银花','jinyinhua_dried_flower_01.png','/api/files/uploads/2026-07-12/62f57c12-6206-4e5a-af61-235ffe6d0466.png',NULL,'standard','unknown','花','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(38,'ATLAS_JINYINHUA_002',12,'金银花','jinyinhua_dried_flower_02.jpeg','/api/files/uploads/2026-07-12/7d47e08f-b965-4039-b1b1-355056b18f82.jpeg',NULL,'standard','unknown','花','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(39,'ATLAS_JINYINHUA_003',12,'金银花','jinyinhua_dried_flower_03.png','/api/files/uploads/2026-07-12/58039884-e4cb-4fe5-b1ee-b13935cc22d9.png',NULL,'standard','unknown','花','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(40,'ATLAS_JINYINHUA_004',12,'金银花','jinyinhua_flower_flowering_fresh_01.jpeg','/api/files/uploads/2026-07-12/be0a6ab3-fe52-4848-9146-a56ccb6756b3.jpeg',NULL,'standard','unknown','花','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(41,'ATLAS_JINYINHUA_005',12,'金银花','jinyinhua_flower_flowering_fresh_02.png','/api/files/uploads/2026-07-12/106a28f5-f28d-4b1b-a4b0-4bf6d6220d4f.png',NULL,'standard','unknown','花','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(42,'ATLAS_JINYINHUA_006',12,'金银花','jinyinhua_flower_flowering_fresh_03.jpeg','/api/files/uploads/2026-07-12/cd5353fd-2ef2-4cea-9720-2d4c543b5dfa.jpeg',NULL,'standard','unknown','花','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(43,'ATLAS_JINYINHUA_007',12,'金银花','jinyinhua_whole_growth_fresh_01.jpeg','/api/files/uploads/2026-07-12/eb1030ad-9256-4d9d-8bbf-ebe5bb2a322c.jpeg',NULL,'standard','unknown','花','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(44,'ATLAS_JINYINHUA_008',12,'金银花','jinyinhua_whole_growth_fresh_02.jpeg','/api/files/uploads/2026-07-12/e91f566c-78ba-46bc-a7a4-5e75ea0b2655.jpeg',NULL,'standard','unknown','花','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(45,'ATLAS_JINYINHUA_009',12,'金银花','jinyinhua_whole_growth_fresh_03.png','/api/files/uploads/2026-07-12/5390fbb7-d862-4ebb-a55e-68452e8f8f7c.png',NULL,'standard','unknown','花','normal',NULL,NULL,NULL,NULL,'Batch imported standard atlas image',NULL,NULL,NULL,'batch_import',NULL,NULL,0,'normal',1,NULL,NULL,NULL,'available',NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `herb_atlas` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_atlas_tag`
--

DROP TABLE IF EXISTS `herb_atlas_tag`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_atlas_tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `atlas_id` bigint NOT NULL COMMENT '标准图谱 ID',
  `tag_name` varchar(100) NOT NULL COMMENT '标签名称',
  `tag_type` varchar(50) DEFAULT NULL COMMENT '标签类型',
  `tag_source` varchar(50) DEFAULT 'manual' COMMENT '标签来源',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_herb_atlas_tag_atlas_id` (`atlas_id`),
  KEY `idx_herb_atlas_tag_tag_name` (`tag_name`)
) ENGINE=InnoDB AUTO_INCREMENT=136 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='标准图谱标签表；维护标准图谱扩展标签。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_atlas_tag`
--

LOCK TABLES `herb_atlas_tag` WRITE;
/*!40000 ALTER TABLE `herb_atlas_tag` DISABLE KEYS */;
INSERT INTO `herb_atlas_tag` VALUES (1,1,'standard','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(2,1,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(3,1,'dangshen','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(4,2,'standard','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(5,2,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(6,2,'dangshen','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(7,3,'standard','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(8,3,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(9,3,'dangshen','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(10,4,'standard','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(11,4,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(12,4,'dangshen','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(13,5,'standard','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(14,5,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(15,5,'dangshen','feature','manual',0,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(16,6,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(17,6,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(18,6,'dangshen','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(19,7,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(20,7,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(21,7,'dangshen','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(22,8,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(23,8,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(24,8,'dangshen','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(25,9,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(26,9,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(27,9,'dangshen','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(28,10,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(29,10,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(30,10,'gouqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(31,11,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(32,11,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(33,11,'gouqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(34,12,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(35,12,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(36,12,'gouqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(37,13,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(38,13,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(39,13,'gouqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(40,14,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(41,14,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(42,14,'gouqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(43,15,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(44,15,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(45,15,'gouqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(46,16,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(47,16,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(48,16,'gouqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(49,17,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(50,17,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(51,17,'gouqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(52,18,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(53,18,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(54,18,'gouqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(55,19,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(56,19,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(57,19,'huanglian','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(58,20,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(59,20,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(60,20,'huanglian','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(61,21,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(62,21,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(63,21,'huanglian','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(64,22,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(65,22,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(66,22,'huanglian','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(67,23,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(68,23,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(69,23,'huanglian','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(70,24,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(71,24,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(72,24,'huanglian','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(73,25,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(74,25,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(75,25,'huanglian','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(76,26,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(77,26,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(78,26,'huanglian','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(79,27,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(80,27,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(81,27,'huanglian','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(82,28,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(83,28,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(84,28,'huangqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(85,29,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(86,29,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(87,29,'huangqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(88,30,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(89,30,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(90,30,'huangqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(91,31,'standard','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(92,31,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(93,31,'huangqi','feature','manual',0,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(94,32,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(95,32,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(96,32,'huangqi','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(97,33,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(98,33,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(99,33,'huangqi','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(100,34,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(101,34,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(102,34,'huangqi','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(103,35,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(104,35,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(105,35,'huangqi','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(106,36,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(107,36,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(108,36,'huangqi','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(109,37,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(110,37,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(111,37,'jinyinhua','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(112,38,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(113,38,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(114,38,'jinyinhua','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(115,39,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(116,39,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(117,39,'jinyinhua','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(118,40,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(119,40,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(120,40,'jinyinhua','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(121,41,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(122,41,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(123,41,'jinyinhua','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(124,42,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(125,42,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(126,42,'jinyinhua','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(127,43,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(128,43,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(129,43,'jinyinhua','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(130,44,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(131,44,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(132,44,'jinyinhua','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(133,45,'standard','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(134,45,'batch_import','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0),(135,45,'jinyinhua','feature','manual',0,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `herb_atlas_tag` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_base`
--

DROP TABLE IF EXISTS `herb_base`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_base` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `base_no` varchar(64) NOT NULL COMMENT '基地编号',
  `base_name` varchar(150) NOT NULL COMMENT '基地名称',
  `base_type` varchar(50) DEFAULT NULL COMMENT '基地类型：planting、collection、teaching、research',
  `region_id` bigint DEFAULT NULL COMMENT '区域 ID',
  `address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(30) DEFAULT NULL COMMENT '联系电话',
  `description` text COMMENT '基地说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_base_base_no` (`base_no`),
  KEY `idx_herb_base_region_id` (`region_id`),
  KEY `idx_herb_base_base_type` (`base_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中药材基地表；保存种植基地、采集基地、教学实践基地等基地信息。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_base`
--

LOCK TABLES `herb_base` WRITE;
/*!40000 ALTER TABLE `herb_base` DISABLE KEYS */;
/*!40000 ALTER TABLE `herb_base` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_distribution`
--

DROP TABLE IF EXISTS `herb_distribution`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_distribution` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `species_id` bigint NOT NULL COMMENT '药材品种 ID',
  `base_id` bigint DEFAULT NULL COMMENT '所属基地 ID',
  `region_id` bigint DEFAULT NULL COMMENT '区域 ID',
  `location_name` varchar(255) DEFAULT NULL COMMENT '地点名称',
  `longitude` decimal(10,7) NOT NULL COMMENT '经度',
  `latitude` decimal(10,7) NOT NULL COMMENT '纬度',
  `province` varchar(64) DEFAULT '重庆市' COMMENT '省份',
  `city` varchar(64) DEFAULT '重庆市' COMMENT '城市',
  `district` varchar(64) DEFAULT NULL COMMENT '区县',
  `address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `altitude` decimal(8,2) DEFAULT NULL COMMENT '海拔',
  `distribution_type` varchar(50) DEFAULT NULL COMMENT '分布类型：wild、cultivated、specimen',
  `distribution_level` varchar(50) DEFAULT NULL COMMENT '分布等级',
  `distribution_desc` varchar(500) DEFAULT NULL COMMENT '分布说明',
  `cover_image_url` varchar(500) DEFAULT NULL COMMENT '封面图片 URL',
  `last_collected_at` datetime DEFAULT NULL COMMENT '最近采集时间',
  `source_type` varchar(50) DEFAULT 'pc' COMMENT '来源类型：pc、app、soap',
  `data_source` varchar(100) DEFAULT NULL COMMENT '数据来源说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_herb_distribution_species_id` (`species_id`),
  KEY `idx_herb_distribution_region_id` (`region_id`),
  KEY `idx_herb_distribution_longitude_latitude` (`longitude`,`latitude`),
  KEY `idx_herb_distribution_district` (`district`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中药材地图分布点表；保存药材地图点位、分布区域、经纬度、地址、海拔、来源和封面图。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_distribution`
--

LOCK TABLES `herb_distribution` WRITE;
/*!40000 ALTER TABLE `herb_distribution` DISABLE KEYS */;
INSERT INTO `herb_distribution` VALUES (1,1,NULL,NULL,NULL,106.5515600,29.5630100,'???','???',NULL,NULL,NULL,'cultivated',NULL,NULL,NULL,NULL,'pc','map-demo',1,1,'2026-07-09 16:04:51','2026-07-09 16:05:04',NULL,NULL,NULL,NULL,NULL,0),(2,2,NULL,NULL,NULL,106.4760670,29.2846022,'重庆市','重庆市',NULL,NULL,NULL,'cultivated',NULL,NULL,NULL,NULL,'pc','map-demo',1,0,'2026-07-09 16:06:36','2026-07-09 16:06:36',NULL,NULL,NULL,NULL,NULL,0),(3,3,NULL,NULL,'1',105.6449359,29.4195632,'重庆市','重庆市','1','1',NULL,'cultivated',NULL,NULL,NULL,NULL,'pc','map-demo',1,1,'2026-07-09 16:06:47','2026-07-09 16:44:13',NULL,NULL,NULL,NULL,NULL,0),(4,4,NULL,NULL,'22',107.6669166,28.7610380,'重庆市','重庆市','22',NULL,NULL,'cultivated',NULL,NULL,NULL,NULL,'pc','map-demo',1,1,'2026-07-09 16:20:09','2026-07-09 16:30:49',NULL,NULL,NULL,NULL,NULL,0),(5,1,NULL,NULL,NULL,106.5515600,29.5630100,'???','???',NULL,NULL,NULL,'cultivated',NULL,NULL,NULL,NULL,'pc','map-demo',1,1,'2026-07-09 16:54:23','2026-07-09 16:54:32',NULL,NULL,NULL,NULL,NULL,0),(6,3,NULL,NULL,'1',106.0930153,29.7612646,'重庆市','重庆市','1','1',NULL,'cultivated',NULL,NULL,'http://localhost:8080/api/files/uploads/2026/07/09/FILE202607091655095111E800217.jpg',NULL,'pc','map-demo',1,0,'2026-07-09 16:55:10','2026-07-09 16:55:10',NULL,NULL,NULL,NULL,NULL,0),(7,5,NULL,NULL,'333',105.8594924,29.7874886,'重庆市','重庆市','333','333',NULL,'cultivated',NULL,'33','http://localhost:8080/api/files/uploads/2026/07/09/FILE20260709165718524CE6F7803.png','2026-07-10 11:11:06','pc','map-demo',1,0,'2026-07-09 16:57:21','2026-07-09 16:57:21',NULL,NULL,NULL,NULL,NULL,2),(8,6,NULL,NULL,'黄连种植基地',106.5208148,29.6603355,'重庆市','重庆市','石柱县',NULL,NULL,'wild',NULL,'分布稀疏','http://localhost:8080/api/files/uploads/2026/07/10/FILE202607101114049661293BB5E.png','2026-07-10 10:03:03','pc','map-demo',1,0,'2026-07-10 11:13:23','2026-07-10 11:13:23',NULL,NULL,NULL,NULL,NULL,1);
/*!40000 ALTER TABLE `herb_distribution` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_growth_record`
--

DROP TABLE IF EXISTS `herb_growth_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_growth_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `species_id` bigint NOT NULL COMMENT '药材品种 ID',
  `distribution_id` bigint DEFAULT NULL COMMENT '地图分布点 ID',
  `collector_id` bigint DEFAULT NULL COMMENT '采集人用户 ID',
  `collector_name_snapshot` varchar(64) DEFAULT NULL COMMENT '采集人姓名快照',
  `region_id` bigint DEFAULT NULL COMMENT '区域 ID',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `growth_stage` varchar(50) DEFAULT NULL COMMENT '生长阶段',
  `soil_type` varchar(64) DEFAULT NULL COMMENT '土壤类型',
  `soil_ph` decimal(4,2) DEFAULT NULL COMMENT '土壤 pH',
  `temperature` decimal(5,2) DEFAULT NULL COMMENT '温度',
  `humidity` decimal(5,2) DEFAULT NULL COMMENT '湿度',
  `weather` varchar(50) DEFAULT NULL COMMENT '天气',
  `sample_weight` decimal(10,3) DEFAULT NULL COMMENT '样本重量 g',
  `device_type` varchar(50) DEFAULT NULL COMMENT '设备类型：app、pc、iot',
  `data_source` varchar(50) DEFAULT 'manual' COMMENT '数据来源',
  `review_status` varchar(50) DEFAULT 'draft' COMMENT '审核状态',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `reviewed_at` datetime DEFAULT NULL COMMENT '最近审核时间',
  `archived_at` datetime DEFAULT NULL COMMENT '归档时间',
  `collected_at` datetime DEFAULT NULL COMMENT '采集时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_herb_growth_record_species_id` (`species_id`),
  KEY `idx_herb_growth_record_distribution_id` (`distribution_id`),
  KEY `idx_herb_growth_record_collected_at` (`collected_at`),
  KEY `idx_herb_growth_record_review_status` (`review_status`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中药材生长采集记录表；保存移动网页版、PC 或设备采集的中药材生长环境、形态和位置数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_growth_record`
--

LOCK TABLES `herb_growth_record` WRITE;
/*!40000 ALTER TABLE `herb_growth_record` DISABLE KEYS */;
INSERT INTO `herb_growth_record` VALUES (1,5,7,NULL,'w0',NULL,105.8594924,29.7874886,'结果期',NULL,NULL,NULL,NULL,NULL,15.000,'pc','map','draft',NULL,NULL,NULL,'2026-07-10 11:10:06',1,0,'2026-07-10 11:10:06','2026-07-10 11:10:06',NULL,NULL,NULL,NULL,'生长良好',0),(2,5,7,NULL,'w1',NULL,105.8594924,29.7874886,'开花期',NULL,NULL,NULL,NULL,NULL,30.000,'pc','map','draft',NULL,NULL,NULL,'2026-07-10 11:11:06',1,0,'2026-07-10 11:11:05','2026-07-10 11:11:05',NULL,NULL,NULL,NULL,'生长状况不好',0);
/*!40000 ALTER TABLE `herb_growth_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_growth_review_record`
--

DROP TABLE IF EXISTS `herb_growth_review_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_growth_review_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `growth_record_id` bigint NOT NULL COMMENT '生长采集记录 ID',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人 ID',
  `review_action` varchar(50) NOT NULL COMMENT '动作：submit、approve、reject、archive',
  `before_status` varchar(50) DEFAULT NULL COMMENT '操作前状态',
  `after_status` varchar(50) DEFAULT NULL COMMENT '操作后状态',
  `review_comment` varchar(500) DEFAULT NULL COMMENT '审核意见',
  `reviewed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '审核时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_herb_growth_review_record_record_id` (`growth_record_id`),
  KEY `idx_herb_growth_review_record_reviewer_id` (`reviewer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采集审核记录表；保存采集记录提交、审核通过、退回、归档等流程历史。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_growth_review_record`
--

LOCK TABLES `herb_growth_review_record` WRITE;
/*!40000 ALTER TABLE `herb_growth_review_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `herb_growth_review_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_image`
--

DROP TABLE IF EXISTS `herb_image`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_image` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `image_no` varchar(50) NOT NULL COMMENT '图片业务编号',
  `species_id` bigint DEFAULT NULL COMMENT '药材品种 ID',
  `distribution_id` bigint DEFAULT NULL COMMENT '地图分布点 ID',
  `growth_record_id` bigint DEFAULT NULL COMMENT '生长采集记录 ID',
  `image_url` varchar(500) NOT NULL COMMENT '图片地址',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图',
  `original_filename` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `file_format` varchar(20) DEFAULT NULL COMMENT '文件格式',
  `image_type` varchar(50) DEFAULT NULL COMMENT '图片类型',
  `image_purpose` varchar(50) DEFAULT NULL COMMENT '图片用途',
  `upload_source` varchar(50) DEFAULT 'pc' COMMENT '上传来源',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传人 ID',
  `region_id` bigint DEFAULT NULL COMMENT '采集区域 ID',
  `collected_location` varchar(255) DEFAULT NULL COMMENT '采集地点文字说明',
  `longitude` decimal(10,7) DEFAULT NULL COMMENT '经度',
  `latitude` decimal(10,7) DEFAULT NULL COMMENT '纬度',
  `collected_at` datetime DEFAULT NULL COMMENT '采集时间',
  `growth_stage` varchar(50) DEFAULT NULL COMMENT '生长阶段',
  `health_status` varchar(50) DEFAULT NULL COMMENT '健康状态',
  `form_type` varchar(50) DEFAULT NULL COMMENT '形态',
  `feature_vector` longtext COMMENT '图片特征向量 JSON',
  `feature_dim` int DEFAULT NULL COMMENT '向量维度',
  `feature_model_id` bigint DEFAULT NULL COMMENT '特征模型 ID',
  `feature_model_code` varchar(100) DEFAULT NULL COMMENT '特征模型编码',
  `feature_model_version` varchar(100) DEFAULT NULL COMMENT '特征模型版本',
  `process_status` varchar(50) DEFAULT 'pending' COMMENT '处理状态',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_image_image_no` (`image_no`),
  KEY `idx_herb_image_species_id` (`species_id`),
  KEY `idx_herb_image_distribution_id` (`distribution_id`),
  KEY `idx_herb_image_process_status` (`process_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户上传/采集中药材图片表；保存 PC、移动网页版或 APP 上传的药材图片。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_image`
--

LOCK TABLES `herb_image` WRITE;
/*!40000 ALTER TABLE `herb_image` DISABLE KEYS */;
/*!40000 ALTER TABLE `herb_image` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_image_match`
--

DROP TABLE IF EXISTS `herb_image_match`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_image_match` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `image_id` bigint NOT NULL COMMENT '用户图片 ID',
  `recognition_id` bigint DEFAULT NULL COMMENT 'AI 识别结果 ID',
  `atlas_id` bigint NOT NULL COMMENT '匹配标准图谱 ID',
  `species_id` bigint DEFAULT NULL COMMENT '匹配药材 ID',
  `herb_name` varchar(100) DEFAULT NULL COMMENT '匹配药材名称',
  `match_rank` int NOT NULL COMMENT '匹配排名',
  `image_similarity` decimal(8,6) DEFAULT NULL COMMENT '图像相似度',
  `metadata_score` decimal(8,6) DEFAULT NULL COMMENT '结构化字段匹配分',
  `final_score` decimal(8,4) DEFAULT NULL COMMENT '最终匹配分',
  `match_level` varchar(50) DEFAULT NULL COMMENT '匹配等级',
  `is_image_type_matched` tinyint DEFAULT '0' COMMENT '图片类型是否匹配',
  `is_growth_stage_matched` tinyint DEFAULT '0' COMMENT '生长阶段是否匹配',
  `is_medicinal_part_matched` tinyint DEFAULT '0' COMMENT '药用部位是否匹配',
  `is_health_status_matched` tinyint DEFAULT '0' COMMENT '健康状态是否匹配',
  `is_form_type_matched` tinyint DEFAULT '0' COMMENT '形态是否匹配',
  `match_method` varchar(50) DEFAULT 'hybrid' COMMENT '匹配方式',
  `warning_msg` varchar(500) DEFAULT NULL COMMENT '提示信息',
  `suggestion` text COMMENT '系统建议',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_herb_image_match_image_id` (`image_id`),
  KEY `idx_herb_image_match_atlas_id` (`atlas_id`),
  KEY `idx_herb_image_match_level` (`match_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户图片与标准图谱比对结果表；保存用户图片与标准图谱之间的 TopN 相似度比对结果。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_image_match`
--

LOCK TABLES `herb_image_match` WRITE;
/*!40000 ALTER TABLE `herb_image_match` DISABLE KEYS */;
/*!40000 ALTER TABLE `herb_image_match` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_image_recognition`
--

DROP TABLE IF EXISTS `herb_image_recognition`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_image_recognition` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `image_id` bigint NOT NULL COMMENT '用户图片 ID',
  `model_id` bigint DEFAULT NULL COMMENT '识别模型 ID',
  `model_code` varchar(100) DEFAULT NULL COMMENT '识别模型编码',
  `model_version` varchar(100) DEFAULT NULL COMMENT '模型版本',
  `rank_no` int NOT NULL COMMENT '识别排名',
  `species_id` bigint DEFAULT NULL COMMENT '识别药材 ID',
  `recognized_herb_name` varchar(100) NOT NULL COMMENT '识别药材名称',
  `confidence` decimal(6,4) DEFAULT NULL COMMENT '置信度 0-1',
  `image_type` varchar(50) DEFAULT NULL COMMENT '图片类型',
  `growth_stage` varchar(50) DEFAULT NULL COMMENT '生长阶段',
  `medicinal_part` varchar(50) DEFAULT NULL COMMENT '药用部位',
  `health_status` varchar(50) DEFAULT NULL COMMENT '健康状态',
  `form_type` varchar(50) DEFAULT NULL COMMENT '形态',
  `color_feature` varchar(255) DEFAULT NULL COMMENT '颜色特征',
  `texture_feature` varchar(255) DEFAULT NULL COMMENT '纹理特征',
  `shape_feature` varchar(255) DEFAULT NULL COMMENT '形态特征',
  `reason` text COMMENT '判断依据',
  `suggestion` text COMMENT '模型建议',
  `raw_response` longtext COMMENT '模型原始返回',
  `is_uncertain` tinyint DEFAULT '0' COMMENT '是否不确定',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_herb_image_recognition_image_id` (`image_id`),
  KEY `idx_herb_image_recognition_rank_no` (`rank_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户图片 AI 识别结果表；保存 AI 对用户上传图片的 TopN 候选识别结果。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_image_recognition`
--

LOCK TABLES `herb_image_recognition` WRITE;
/*!40000 ALTER TABLE `herb_image_recognition` DISABLE KEYS */;
/*!40000 ALTER TABLE `herb_image_recognition` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_knowledge_entity`
--

DROP TABLE IF EXISTS `herb_knowledge_entity`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_knowledge_entity` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `entity_no` varchar(50) NOT NULL COMMENT '实体编号',
  `entity_name` varchar(255) NOT NULL COMMENT '实体名称',
  `entity_type` varchar(100) NOT NULL COMMENT '实体类型：herb、region、effect、course、project、atlas、record',
  `ref_table` varchar(100) DEFAULT NULL COMMENT '关联业务表名',
  `ref_id` bigint DEFAULT NULL COMMENT '关联业务记录 ID',
  `description` text COMMENT '实体描述',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_knowledge_entity_entity_no` (`entity_no`),
  KEY `idx_herb_knowledge_entity_type` (`entity_type`),
  KEY `idx_herb_knowledge_entity_ref` (`ref_table`,`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中药材知识图谱实体表；保存知识图谱节点实体。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_knowledge_entity`
--

LOCK TABLES `herb_knowledge_entity` WRITE;
/*!40000 ALTER TABLE `herb_knowledge_entity` DISABLE KEYS */;
/*!40000 ALTER TABLE `herb_knowledge_entity` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_knowledge_relation`
--

DROP TABLE IF EXISTS `herb_knowledge_relation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_knowledge_relation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `relation_code` varchar(100) NOT NULL COMMENT '关系编码',
  `relation_name` varchar(100) NOT NULL COMMENT '关系名称',
  `description` text COMMENT '关系说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_knowledge_relation_relation_code` (`relation_code`),
  UNIQUE KEY `uk_herb_knowledge_relation_relation_name` (`relation_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中药材知识图谱关系类型表；维护知识图谱关系类型。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_knowledge_relation`
--

LOCK TABLES `herb_knowledge_relation` WRITE;
/*!40000 ALTER TABLE `herb_knowledge_relation` DISABLE KEYS */;
/*!40000 ALTER TABLE `herb_knowledge_relation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_knowledge_triple`
--

DROP TABLE IF EXISTS `herb_knowledge_triple`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_knowledge_triple` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `subject_entity_id` bigint NOT NULL COMMENT '主语实体 ID',
  `relation_id` bigint NOT NULL COMMENT '关系 ID',
  `object_entity_id` bigint NOT NULL COMMENT '宾语实体 ID',
  `confidence` decimal(6,4) DEFAULT '1.0000' COMMENT '关系可信度',
  `source_type` varchar(100) DEFAULT NULL COMMENT '来源类型：manual、system、document_extract、ai',
  `source_desc` varchar(255) DEFAULT NULL COMMENT '来源说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_knowledge_triple_sro` (`subject_entity_id`,`relation_id`,`object_entity_id`),
  KEY `idx_herb_knowledge_triple_subject` (`subject_entity_id`),
  KEY `idx_herb_knowledge_triple_object` (`object_entity_id`),
  KEY `idx_herb_knowledge_triple_relation` (`relation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中药材知识图谱三元组表；保存实体-关系-实体三元组数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_knowledge_triple`
--

LOCK TABLES `herb_knowledge_triple` WRITE;
/*!40000 ALTER TABLE `herb_knowledge_triple` DISABLE KEYS */;
/*!40000 ALTER TABLE `herb_knowledge_triple` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `herb_species`
--

DROP TABLE IF EXISTS `herb_species`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `herb_species` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `herb_no` varchar(50) NOT NULL COMMENT '药材编号，业务唯一编码',
  `herb_name` varchar(100) NOT NULL COMMENT '药材名称',
  `alias_name` varchar(255) DEFAULT NULL COMMENT '别名',
  `latin_name` varchar(255) DEFAULT NULL COMMENT '拉丁学名',
  `category_id` bigint DEFAULT NULL COMMENT '药材分类 ID，关联 dict_item.id',
  `category_code` varchar(50) DEFAULT NULL COMMENT '药材分类编码',
  `medicinal_part` varchar(100) DEFAULT NULL COMMENT '主要药用部位',
  `efficacy` text COMMENT '功效主治',
  `growth_environment` text COMMENT '适宜生长环境',
  `origin_area` varchar(255) DEFAULT NULL COMMENT '产地或分布区域说明',
  `growth_cycle` varchar(100) DEFAULT NULL COMMENT '生长周期',
  `description` text COMMENT '品种简介',
  `knowledge_entity_id` bigint DEFAULT NULL COMMENT '对应知识图谱实体 ID',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_herb_species_herb_no` (`herb_no`),
  KEY `idx_herb_species_herb_name` (`herb_name`),
  KEY `idx_herb_species_category_id` (`category_id`),
  KEY `idx_herb_species_knowledge_entity_id` (`knowledge_entity_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='中药材品种基础信息表；全系统唯一的中药材主数据表。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `herb_species`
--

LOCK TABLES `herb_species` WRITE;
/*!40000 ALTER TABLE `herb_species` DISABLE KEYS */;
INSERT INTO `herb_species` VALUES (1,'HERB-20260709160451-0fa22b','??????',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-09 16:04:51','2026-07-09 16:04:51',NULL,NULL,NULL,NULL,NULL,0),(2,'HERB-20260709160636-ec3609','1',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-09 16:06:36','2026-07-09 16:06:36',NULL,NULL,NULL,NULL,NULL,0),(3,'HERB-20260709160647-7b464b','111','11','11',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-09 16:06:47','2026-07-09 16:06:47',NULL,NULL,NULL,NULL,NULL,0),(4,'HERB-20260709162009-f52394','222','222','222',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-09 16:20:09','2026-07-09 16:20:09',NULL,NULL,NULL,NULL,NULL,0),(5,'HERB-20260709165721-9683ed','人参','333',NULL,NULL,NULL,NULL,'33','33',NULL,NULL,NULL,NULL,1,0,'2026-07-09 16:57:21','2026-07-09 16:57:21',NULL,NULL,NULL,NULL,NULL,0),(6,'HERB-20260710111323-7a79d0','黄连',NULL,NULL,NULL,NULL,NULL,'降火','海拔较高',NULL,NULL,NULL,NULL,1,0,'2026-07-10 11:13:23','2026-07-10 11:13:23',NULL,NULL,NULL,NULL,NULL,0),(7,'HERB-20260710121803-a05eac','接口测试临时药材',NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-10 12:18:03','2026-07-10 12:18:03',NULL,NULL,NULL,NULL,NULL,0),(8,'HERB_DANGSHEN','党参','dangshen codonopsis pilosula','Codonopsis pilosula',NULL,'HERB','根',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-12 00:40:34','2026-07-12 00:40:34',NULL,NULL,NULL,NULL,NULL,0),(9,'HERB_GOUQI','枸杞','gouqi lycium barbarum','Lycium barbarum',NULL,'HERB','果实',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(10,'HERB_HUANGLIAN','黄连','huanglian coptis chinensis','Coptis chinensis',NULL,'HERB','根茎',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(11,'HERB_HUANGQI','黄芪','huangqi astragalus mongholicus','Astragalus mongholicus',NULL,'HERB','根',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-12 00:40:35','2026-07-12 00:40:35',NULL,NULL,NULL,NULL,NULL,0),(12,'HERB_JINYINHUA','金银花','jinyinhua lonicera japonica','Lonicera japonica',NULL,'HERB','花',NULL,NULL,NULL,NULL,NULL,NULL,1,0,'2026-07-12 00:40:36','2026-07-12 00:40:36',NULL,NULL,NULL,NULL,NULL,0);
/*!40000 ALTER TABLE `herb_species` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_data_change`
--

DROP TABLE IF EXISTS `log_data_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_data_change` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `table_name` varchar(100) NOT NULL COMMENT '被修改表名',
  `record_id` bigint NOT NULL COMMENT '被修改记录 ID',
  `change_type` varchar(50) NOT NULL COMMENT '变更类型：insert、update、delete',
  `before_data` longtext COMMENT '修改前 JSON',
  `after_data` longtext COMMENT '修改后 JSON',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_data_change_table_record` (`table_name`,`record_id`),
  KEY `idx_log_data_change_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据变更日志表；记录重要业务表数据修改前后内容。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_data_change`
--

LOCK TABLES `log_data_change` WRITE;
/*!40000 ALTER TABLE `log_data_change` DISABLE KEYS */;
/*!40000 ALTER TABLE `log_data_change` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_data_sync`
--

DROP TABLE IF EXISTS `log_data_sync`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_data_sync` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `trace_id` varchar(100) DEFAULT NULL COMMENT '请求追踪 ID',
  `sync_type` varchar(50) NOT NULL COMMENT '同步类型：soap、app_upload、batch_import',
  `source_system` varchar(100) DEFAULT NULL COMMENT '来源系统',
  `request_data` longtext COMMENT '请求数据，JSON 或 XML',
  `response_data` longtext COMMENT '响应数据',
  `sync_status` varchar(50) NOT NULL DEFAULT 'processing' COMMENT '同步状态：processing、success、failed',
  `error_message` text COMMENT '错误信息',
  `target_table` varchar(100) DEFAULT NULL COMMENT '目标表',
  `target_id` bigint DEFAULT NULL COMMENT '目标记录 ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `operation_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_data_sync_sync_type` (`sync_type`),
  KEY `idx_log_data_sync_status` (`sync_status`),
  KEY `idx_log_data_sync_target` (`target_table`,`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='数据同步日志表；保存 APP 上传、批量导入、外部系统同步等通用同步日志。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_data_sync`
--

LOCK TABLES `log_data_sync` WRITE;
/*!40000 ALTER TABLE `log_data_sync` DISABLE KEYS */;
/*!40000 ALTER TABLE `log_data_sync` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_file_access`
--

DROP TABLE IF EXISTS `log_file_access`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_file_access` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `file_id` bigint NOT NULL COMMENT '文件 ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `access_type` varchar(50) NOT NULL COMMENT '访问类型：preview、download、play',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP 地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端信息',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '访问时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_file_access_file_id` (`file_id`),
  KEY `idx_log_file_access_operator_id` (`operator_id`),
  KEY `idx_log_file_access_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件访问日志表；记录文件预览、下载、播放等访问行为。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_file_access`
--

LOCK TABLES `log_file_access` WRITE;
/*!40000 ALTER TABLE `log_file_access` DISABLE KEYS */;
/*!40000 ALTER TABLE `log_file_access` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_login`
--

DROP TABLE IF EXISTS `log_login`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_login` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户 ID',
  `username` varchar(64) DEFAULT NULL COMMENT '登录账号',
  `login_result` varchar(50) NOT NULL COMMENT '登录结果：success、failed',
  `fail_reason` varchar(255) DEFAULT NULL COMMENT '失败原因',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP 地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端信息',
  `logged_in_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_login_user_id` (`user_id`),
  KEY `idx_log_login_logged_in_at` (`logged_in_at`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志表；记录用户登录结果、失败原因、IP 和客户端信息。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_login`
--

LOCK TABLES `log_login` WRITE;
/*!40000 ALTER TABLE `log_login` DISABLE KEYS */;
INSERT INTO `log_login` VALUES (1,NULL,'bdis','failed','账号或密码错误','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0','2026-07-12 21:10:21','2026-07-12 21:10:20'),(2,1,'chong','success','bootstrap-admin','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457','2026-07-12 21:23:44','2026-07-12 21:23:43'),(3,1,'chong','success',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0','2026-07-12 21:24:51','2026-07-12 21:24:50'),(4,1,'chong','success',NULL,'0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36 Edg/150.0.0.0','2026-07-13 00:18:50','2026-07-13 00:18:49'),(5,NULL,'chong','failed','账号或密码错误','0:0:0:0:0:0:0:1','Mozilla/5.0 (Windows NT; Windows NT 10.0; zh-CN) WindowsPowerShell/5.1.26100.8457','2026-07-13 00:48:35','2026-07-13 00:48:34');
/*!40000 ALTER TABLE `log_login` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `log_operation`
--

DROP TABLE IF EXISTS `log_operation`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `log_operation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `trace_id` varchar(100) DEFAULT NULL COMMENT '请求追踪 ID',
  `operator_id` bigint DEFAULT NULL COMMENT '操作人 ID',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人名称快照',
  `operation_module` varchar(100) DEFAULT NULL COMMENT '操作模块',
  `operation_type` varchar(50) DEFAULT NULL COMMENT '操作类型',
  `operation_desc` varchar(500) DEFAULT NULL COMMENT '操作描述',
  `request_method` varchar(20) DEFAULT NULL COMMENT '请求方法',
  `request_url` varchar(500) DEFAULT NULL COMMENT '请求地址',
  `request_param` text COMMENT '请求参数，敏感字段脱敏',
  `result_status` varchar(50) DEFAULT NULL COMMENT '结果状态',
  `error_message` text COMMENT '错误信息',
  `ip_address` varchar(64) DEFAULT NULL COMMENT 'IP 地址',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端信息',
  `operation_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_log_operation_operator_id` (`operator_id`),
  KEY `idx_log_operation_module` (`operation_module`),
  KEY `idx_log_operation_time` (`operation_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='操作日志表；记录后台管理、审核、上传、下载、删除等关键业务操作。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `log_operation`
--

LOCK TABLES `log_operation` WRITE;
/*!40000 ALTER TABLE `log_operation` DISABLE KEYS */;
/*!40000 ALTER TABLE `log_operation` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `perf_identification`
--

DROP TABLE IF EXISTS `perf_identification`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perf_identification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `performance_id` bigint NOT NULL COMMENT '业绩 ID',
  `identifier_id` bigint NOT NULL COMMENT '认定人 ID',
  `identify_action` varchar(50) DEFAULT 'identify' COMMENT '认定动作',
  `identify_result` varchar(50) NOT NULL COMMENT '认定结果',
  `identify_comment` varchar(500) DEFAULT NULL COMMENT '认定意见',
  `identified_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '认定时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_perf_identification_performance_id` (`performance_id`),
  KEY `idx_perf_identification_identifier_id` (`identifier_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业绩认定记录表；保存业绩审核认定结果、意见、认定人和时间。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `perf_identification`
--

LOCK TABLES `perf_identification` WRITE;
/*!40000 ALTER TABLE `perf_identification` DISABLE KEYS */;
INSERT INTO `perf_identification` VALUES (1,1,1,'submit','submitted','业绩提交','2026-07-10 16:34:21','2026-07-10 16:34:20',1,NULL),(2,1,2,'identify','approved','材料完整，认定通过','2026-07-10 16:35:21','2026-07-10 16:35:20',2,'Apifox审核通过测试'),(3,2,1,'submit','submitted','业绩提交','2026-07-10 16:37:58','2026-07-10 16:37:58',1,NULL),(4,2,2,'reject','rejected','材料不完整，请补充证明文件','2026-07-10 16:38:23','2026-07-10 16:38:22',2,'Apifox退回测试'),(5,3,1,'submit','submitted','业绩提交','2026-07-11 09:45:17','2026-07-11 09:45:17',1,NULL),(6,3,2,'approve','approved','材料完整，自动测试认定通过','2026-07-11 09:45:47','2026-07-11 09:45:47',2,'decision-comment-test');
/*!40000 ALTER TABLE `perf_identification` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `perf_record`
--

DROP TABLE IF EXISTS `perf_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perf_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `performance_no` varchar(64) NOT NULL COMMENT '业绩编号',
  `user_id` bigint NOT NULL COMMENT '所属用户 ID',
  `performance_title` varchar(200) NOT NULL COMMENT '业绩标题',
  `performance_type` varchar(50) DEFAULT NULL COMMENT '业绩类型',
  `standard_id` bigint DEFAULT NULL COMMENT '认定标准 ID',
  `source_type` varchar(50) DEFAULT NULL COMMENT '来源类型',
  `source_id` bigint DEFAULT NULL COMMENT '来源对象 ID',
  `identify_status` varchar(50) DEFAULT 'draft' COMMENT '认定状态',
  `submitted_at` datetime DEFAULT NULL COMMENT '提交时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perf_record_performance_no` (`performance_no`),
  KEY `idx_perf_record_user_id` (`user_id`),
  KEY `idx_perf_record_identify_status` (`identify_status`),
  KEY `idx_perf_record_source` (`source_type`,`source_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业绩记录表；保存用户填报或由成果、申报、课程资源转化而来的业绩记录。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `perf_record`
--

LOCK TABLES `perf_record` WRITE;
/*!40000 ALTER TABLE `perf_record` DISABLE KEYS */;
INSERT INTO `perf_record` VALUES (1,'PERF-20260710163147-777c5d',1,'川芎道地药材质量评价与申报支撑成果','research',1,'declaration',1,'approved','2026-07-10 16:34:21',1,0,'2026-07-10 16:31:47','2026-07-10 16:31:47',1,2,NULL,NULL,'修改后的M18业绩测试数据',3),(2,'PERF-20260710163714-3d5a14',1,'测试退回的业绩材料','teaching',NULL,NULL,NULL,'rejected','2026-07-10 16:37:58',1,0,'2026-07-10 16:37:14','2026-07-10 16:37:14',1,2,NULL,NULL,'用于测试退回流程',2),(3,'PERF-20260711094504-5cfb18',1,'M18自动测试业绩','research',2,'declaration',1,'approved','2026-07-11 09:45:17',1,0,'2026-07-11 09:45:04','2026-07-11 09:45:04',1,2,NULL,NULL,'auto-test-performance',2),(4,'PERF-20260711094811-f59d4e',1,'M18旧字段兼容测试业绩','teaching',NULL,NULL,NULL,'draft',NULL,1,0,'2026-07-11 09:48:11','2026-07-11 09:48:11',1,NULL,NULL,NULL,'legacy-audit-test',0);
/*!40000 ALTER TABLE `perf_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `perf_standard`
--

DROP TABLE IF EXISTS `perf_standard`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `perf_standard` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `standard_no` varchar(64) NOT NULL COMMENT '标准编号',
  `standard_name` varchar(150) NOT NULL COMMENT '标准名称',
  `performance_type` varchar(50) NOT NULL COMMENT '业绩类型',
  `standard_desc` text COMMENT '标准说明',
  `score_rule` text COMMENT '评分或认定规则',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perf_standard_standard_no` (`standard_no`),
  KEY `idx_perf_standard_performance_type` (`performance_type`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业绩认定标准表；保存不同业绩类型的认定标准和评分规则。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `perf_standard`
--

LOCK TABLES `perf_standard` WRITE;
/*!40000 ALTER TABLE `perf_standard` DISABLE KEYS */;
INSERT INTO `perf_standard` VALUES (1,'PSTD-20260710163045-d2adcd','科研成果业绩认定标准','research','用于科研论文、课题成果、成果转化等业绩认定','省级以上成果优先认定，材料完整后审核通过',1,1,0,'2026-07-10 16:30:45','2026-07-10 16:30:45',1,NULL,NULL,NULL,'M18测试标准',0),(2,'PSTD-20260711094453-d91798','M18自动测试认定标准','research','用于自动化接口测试','材料完整即可认定',99,1,0,'2026-07-11 09:44:53','2026-07-11 09:44:53',1,NULL,NULL,NULL,'auto-test',0);
/*!40000 ALTER TABLE `perf_standard` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rel_project_member`
--

DROP TABLE IF EXISTS `rel_project_member`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rel_project_member` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `project_id` bigint NOT NULL COMMENT '课题 ID',
  `user_id` bigint NOT NULL COMMENT '成员用户 ID',
  `member_role` varchar(50) DEFAULT NULL COMMENT '成员角色',
  `joined_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rel_project_member_project_id_user_id` (`project_id`,`user_id`),
  KEY `idx_rel_project_member_project_id` (`project_id`),
  KEY `idx_rel_project_member_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课题成员关系表；课题与用户成员多对多关系。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rel_project_member`
--

LOCK TABLES `rel_project_member` WRITE;
/*!40000 ALTER TABLE `rel_project_member` DISABLE KEYS */;
/*!40000 ALTER TABLE `rel_project_member` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rel_role_permission`
--

DROP TABLE IF EXISTS `rel_role_permission`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rel_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `role_id` bigint NOT NULL COMMENT '角色 ID',
  `permission_id` bigint NOT NULL COMMENT '权限 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rel_role_permission_role_id_permission_id` (`role_id`,`permission_id`),
  KEY `idx_rel_role_permission_role_id` (`role_id`),
  KEY `idx_rel_role_permission_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关系表；角色与权限多对多关系。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rel_role_permission`
--

LOCK TABLES `rel_role_permission` WRITE;
/*!40000 ALTER TABLE `rel_role_permission` DISABLE KEYS */;
/*!40000 ALTER TABLE `rel_role_permission` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `rel_user_role`
--

DROP TABLE IF EXISTS `rel_user_role`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `rel_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `role_id` bigint NOT NULL COMMENT '角色 ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_rel_user_role_user_id_role_id` (`user_id`,`role_id`),
  KEY `idx_rel_user_role_user_id` (`user_id`),
  KEY `idx_rel_user_role_role_id` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关系表；用户与角色多对多关系。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `rel_user_role`
--

LOCK TABLES `rel_user_role` WRITE;
/*!40000 ALTER TABLE `rel_user_role` DISABLE KEYS */;
INSERT INTO `rel_user_role` VALUES (1,1,1,'2026-07-12 21:23:43',NULL,NULL);
/*!40000 ALTER TABLE `rel_user_role` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `research_achievement`
--

DROP TABLE IF EXISTS `research_achievement`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `research_achievement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `achievement_no` varchar(64) NOT NULL COMMENT '成果编号',
  `project_id` bigint DEFAULT NULL COMMENT '所属课题 ID',
  `owner_id` bigint DEFAULT NULL COMMENT '成果负责人 ID',
  `achievement_name` varchar(200) NOT NULL COMMENT '成果名称',
  `achievement_type` varchar(50) DEFAULT NULL COMMENT '成果类型',
  `published_at` datetime DEFAULT NULL COMMENT '发表或完成时间',
  `file_id` bigint DEFAULT NULL COMMENT '成果附件文件 ID',
  `file_url` varchar(500) DEFAULT NULL COMMENT '附件地址兼容字段',
  `description` text COMMENT '成果说明',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_research_achievement_achievement_no` (`achievement_no`),
  KEY `idx_research_achievement_project_id` (`project_id`),
  KEY `idx_research_achievement_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科研成果表；保存论文、专利、报告、实验成果等科研成果。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `research_achievement`
--

LOCK TABLES `research_achievement` WRITE;
/*!40000 ALTER TABLE `research_achievement` DISABLE KEYS */;
/*!40000 ALTER TABLE `research_achievement` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `research_project`
--

DROP TABLE IF EXISTS `research_project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `research_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `project_no` varchar(64) NOT NULL COMMENT '课题编号',
  `project_name` varchar(200) NOT NULL COMMENT '课题名称',
  `project_type` varchar(50) DEFAULT NULL COMMENT '课题类型',
  `leader_id` bigint DEFAULT NULL COMMENT '负责人 ID',
  `species_id` bigint DEFAULT NULL COMMENT '关联药材 ID',
  `description` text COMMENT '课题简介',
  `started_at` datetime DEFAULT NULL COMMENT '开始时间',
  `ended_at` datetime DEFAULT NULL COMMENT '结束时间',
  `project_status` varchar(50) DEFAULT 'planning' COMMENT '课题状态',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_research_project_project_no` (`project_no`),
  KEY `idx_research_project_leader_id` (`leader_id`),
  KEY `idx_research_project_species_id` (`species_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科研课题表；保存科研课题、负责人、周期、状态和关联药材信息。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `research_project`
--

LOCK TABLES `research_project` WRITE;
/*!40000 ALTER TABLE `research_project` DISABLE KEYS */;
/*!40000 ALTER TABLE `research_project` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `soap_exchange_record`
--

DROP TABLE IF EXISTS `soap_exchange_record`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `soap_exchange_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_id` bigint DEFAULT NULL COMMENT '同步任务 ID',
  `exchange_no` varchar(64) NOT NULL COMMENT '交换编号',
  `service_name` varchar(150) NOT NULL COMMENT '服务名称',
  `method_name` varchar(150) DEFAULT NULL COMMENT '方法名称',
  `request_xml` longtext COMMENT '请求 XML',
  `response_xml` longtext COMMENT '响应 XML',
  `sync_status` varchar(50) DEFAULT 'pending' COMMENT '同步状态',
  `error_message` text COMMENT '错误信息',
  `called_by` bigint DEFAULT NULL COMMENT '调用人 ID',
  `called_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '调用时间',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_soap_exchange_record_exchange_no` (`exchange_no`),
  KEY `idx_soap_exchange_record_task_id` (`task_id`),
  KEY `idx_soap_exchange_record_called_at` (`called_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SOAP 交换记录表；保存 SOAP 请求 XML、响应 XML、交换状态和错误信息。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `soap_exchange_record`
--

LOCK TABLES `soap_exchange_record` WRITE;
/*!40000 ALTER TABLE `soap_exchange_record` DISABLE KEYS */;
/*!40000 ALTER TABLE `soap_exchange_record` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `soap_sync_task`
--

DROP TABLE IF EXISTS `soap_sync_task`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `soap_sync_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `task_no` varchar(64) NOT NULL COMMENT '任务编号',
  `task_name` varchar(150) NOT NULL COMMENT '任务名称',
  `service_name` varchar(150) NOT NULL COMMENT 'SOAP 服务名称',
  `method_name` varchar(150) DEFAULT NULL COMMENT 'SOAP 方法名称',
  `sync_direction` varchar(50) DEFAULT 'inbound' COMMENT '同步方向',
  `sync_status` varchar(50) DEFAULT 'pending' COMMENT '同步状态',
  `last_sync_at` datetime DEFAULT NULL COMMENT '最近同步时间',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_soap_sync_task_task_no` (`task_no`),
  KEY `idx_soap_sync_task_service_name` (`service_name`),
  KEY `idx_soap_sync_task_status` (`sync_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SOAP 同步任务表；保存 SOAP 同步任务配置、同步方向、状态和最近同步时间。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `soap_sync_task`
--

LOCK TABLES `soap_sync_task` WRITE;
/*!40000 ALTER TABLE `soap_sync_task` DISABLE KEYS */;
/*!40000 ALTER TABLE `soap_sync_task` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `stat_dashboard_snapshot`
--

DROP TABLE IF EXISTS `stat_dashboard_snapshot`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `stat_dashboard_snapshot` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `snapshot_date` date NOT NULL COMMENT '统计日期',
  `herb_count` int DEFAULT '0' COMMENT '药材数量',
  `base_count` int DEFAULT '0' COMMENT '基地数量',
  `distribution_count` int DEFAULT '0' COMMENT '地图点位数量',
  `growth_record_count` int DEFAULT '0' COMMENT '采集记录数量',
  `course_count` int DEFAULT '0' COMMENT '课程数量',
  `pending_review_count` int DEFAULT '0' COMMENT '待审核数量',
  `dashboard_data` longtext COMMENT '首页统计 JSON 快照',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stat_dashboard_snapshot_date` (`snapshot_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='首页看板统计快照表；保存首页看板统计快照，用于缓存药材数量、基地数量、采集数量、待办事项等统计数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `stat_dashboard_snapshot`
--

LOCK TABLES `stat_dashboard_snapshot` WRITE;
/*!40000 ALTER TABLE `stat_dashboard_snapshot` DISABLE KEYS */;
/*!40000 ALTER TABLE `stat_dashboard_snapshot` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_department`
--

DROP TABLE IF EXISTS `sys_department`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_department` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `department_no` varchar(64) NOT NULL COMMENT '部门编号',
  `department_name` varchar(100) NOT NULL COMMENT '部门名称',
  `organization_id` bigint NOT NULL COMMENT '所属机构 ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父级部门 ID',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_department_department_no` (`department_no`),
  KEY `idx_sys_department_organization_id` (`organization_id`),
  KEY `idx_sys_department_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='部门表；保存部门、教研室、实验室、小组等组织结构。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_department`
--

LOCK TABLES `sys_department` WRITE;
/*!40000 ALTER TABLE `sys_department` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_department` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_file_business`
--

DROP TABLE IF EXISTS `sys_file_business`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_file_business` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `file_id` bigint NOT NULL COMMENT '文件 ID',
  `biz_type` varchar(100) NOT NULL COMMENT '业务类型，如 herb_species、edu_course、eval_application',
  `biz_id` bigint NOT NULL COMMENT '业务记录 ID',
  `file_usage` varchar(50) DEFAULT NULL COMMENT '用途：cover、attachment、material、video、atlas',
  `sort_order` int DEFAULT '0' COMMENT '排序号',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_sys_file_business_file_id` (`file_id`),
  KEY `idx_sys_file_business_biz` (`biz_type`,`biz_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件业务关联表；统一维护文件与业务对象的关联。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_file_business`
--

LOCK TABLES `sys_file_business` WRITE;
/*!40000 ALTER TABLE `sys_file_business` DISABLE KEYS */;
INSERT INTO `sys_file_business` VALUES (1,13,'perf_record',3,'material',1,'2026-07-11 09:47:30',1,'auto-test-material');
/*!40000 ALTER TABLE `sys_file_business` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_file_resource`
--

DROP TABLE IF EXISTS `sys_file_resource`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_file_resource` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `file_no` varchar(64) NOT NULL COMMENT '文件编号',
  `file_name` varchar(255) NOT NULL COMMENT '文件名称',
  `original_filename` varchar(255) DEFAULT NULL COMMENT '原始文件名',
  `file_type` varchar(50) DEFAULT NULL COMMENT '文件类型：image、video、document、atlas、material',
  `file_format` varchar(50) DEFAULT NULL COMMENT '文件格式',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `file_url` varchar(500) NOT NULL COMMENT '文件访问地址',
  `thumbnail_url` varchar(500) DEFAULT NULL COMMENT '缩略图地址',
  `storage_type` varchar(50) DEFAULT 'local' COMMENT '存储类型：local、oss、cos',
  `uploader_id` bigint DEFAULT NULL COMMENT '上传人 ID',
  `uploaded_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_file_resource_file_no` (`file_no`),
  KEY `idx_sys_file_resource_file_type` (`file_type`),
  KEY `idx_sys_file_resource_uploader_id` (`uploader_id`),
  KEY `idx_sys_file_resource_uploaded_at` (`uploaded_at`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='文件资源表；统一保存图片、文档、视频、课件、图谱、申报材料、业绩材料等文件元数据。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_file_resource`
--

LOCK TABLES `sys_file_resource` WRITE;
/*!40000 ALTER TABLE `sys_file_resource` DISABLE KEYS */;
INSERT INTO `sys_file_resource` VALUES (2,'FILE20260709164016519974478BA','FILE20260709164016519974478BA.png','屏幕截图 2026-07-09 161627.png','image','png',131006,'/api/files/uploads/2026/07/09/FILE20260709164016519974478BA.png','/api/files/uploads/2026/07/09/FILE20260709164016519974478BA.png','local',NULL,'2026-07-09 16:40:17',1,0,'2026-07-09 16:40:16','2026-07-09 16:40:16',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(3,'FILE20260709164048838022A6694','FILE20260709164048838022A6694.png','arch_diagram.png','image','png',95936,'/api/files/uploads/2026/07/09/FILE20260709164048838022A6694.png','/api/files/uploads/2026/07/09/FILE20260709164048838022A6694.png','local',NULL,'2026-07-09 16:40:49',1,0,'2026-07-09 16:40:48','2026-07-09 16:40:48',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(4,'FILE202607091640577802EAF740D','FILE202607091640577802EAF740D.png','arch_diagram.png','image','png',95936,'/api/files/uploads/2026/07/09/FILE202607091640577802EAF740D.png','/api/files/uploads/2026/07/09/FILE202607091640577802EAF740D.png','local',NULL,'2026-07-09 16:40:58',1,0,'2026-07-09 16:40:57','2026-07-09 16:40:57',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(5,'FILE20260709164355346B067E943','FILE20260709164355346B067E943.png','arch_diagram.png','image','png',95936,'/api/files/uploads/2026/07/09/FILE20260709164355346B067E943.png','/api/files/uploads/2026/07/09/FILE20260709164355346B067E943.png','local',NULL,'2026-07-09 16:43:55',1,0,'2026-07-09 16:43:55','2026-07-09 16:43:55',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(6,'FILE202607091644192710FB5E0F3','FILE202607091644192710FB5E0F3.jpg','dfd_training_center.jpg','image','jpg',135423,'/api/files/uploads/2026/07/09/FILE202607091644192710FB5E0F3.jpg','/api/files/uploads/2026/07/09/FILE202607091644192710FB5E0F3.jpg','local',NULL,'2026-07-09 16:44:19',1,0,'2026-07-09 16:44:19','2026-07-09 16:44:19',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(7,'FILE2026070916465347612F4812A','FILE2026070916465347612F4812A.jpg','dfd_training_center.jpg','image','jpg',135423,'/api/files/uploads/2026/07/09/FILE2026070916465347612F4812A.jpg','/api/files/uploads/2026/07/09/FILE2026070916465347612F4812A.jpg','local',NULL,'2026-07-09 16:46:53',1,0,'2026-07-09 16:46:53','2026-07-09 16:46:53',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(8,'FILE202607091650504815B48478D','FILE202607091650504815B48478D.jpg','dfd_training_center.jpg','image','jpg',135423,'/api/files/uploads/2026/07/09/FILE202607091650504815B48478D.jpg','/api/files/uploads/2026/07/09/FILE202607091650504815B48478D.jpg','local',NULL,'2026-07-09 16:50:50',1,0,'2026-07-09 16:50:50','2026-07-09 16:50:50',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(9,'FILE202607091655095111E800217','FILE202607091655095111E800217.jpg','dfd_training_center.jpg','image','jpg',135423,'/api/files/uploads/2026/07/09/FILE202607091655095111E800217.jpg','/api/files/uploads/2026/07/09/FILE202607091655095111E800217.jpg','local',NULL,'2026-07-09 16:55:10',1,0,'2026-07-09 16:55:09','2026-07-09 16:55:09',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(10,'FILE20260709165718524CE6F7803','FILE20260709165718524CE6F7803.png','屏幕截图 2025-09-08 112332.png','image','png',934126,'/api/files/uploads/2026/07/09/FILE20260709165718524CE6F7803.png','/api/files/uploads/2026/07/09/FILE20260709165718524CE6F7803.png','local',NULL,'2026-07-09 16:57:19',1,0,'2026-07-09 16:57:18','2026-07-09 16:57:18',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(11,'FILE202607100936298614A826265','FILE202607100936298614A826265.png','屏幕截图 2026-07-09 161627.png','image','png',131006,'/api/files/uploads/2026/07/10/FILE202607100936298614A826265.png','/api/files/uploads/2026/07/10/FILE202607100936298614A826265.png','local',NULL,'2026-07-10 09:36:30',1,0,'2026-07-10 09:36:29','2026-07-10 09:36:29',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(12,'FILE202607101114049661293BB5E','FILE202607101114049661293BB5E.png','屏幕截图 2026-03-20 215431.png','image','png',4129844,'/api/files/uploads/2026/07/10/FILE202607101114049661293BB5E.png','/api/files/uploads/2026/07/10/FILE202607101114049661293BB5E.png','local',NULL,'2026-07-10 11:14:05',1,0,'2026-07-10 11:14:05','2026-07-10 11:14:05',NULL,NULL,NULL,NULL,'bizType=map_point, fileUsage=cover',0),(13,'FILE2026071109471821127D6A132','FILE2026071109471821127D6A132.xml','pom.xml','file','xml',6136,'/api/files/uploads/2026/07/11/FILE2026071109471821127D6A132.xml','/api/files/uploads/2026/07/11/FILE2026071109471821127D6A132.xml','local',NULL,'2026-07-11 09:47:18',1,0,'2026-07-11 09:47:18','2026-07-11 09:47:18',NULL,NULL,NULL,NULL,'bizType=perf_record, fileUsage=material',0);
/*!40000 ALTER TABLE `sys_file_resource` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_organization`
--

DROP TABLE IF EXISTS `sys_organization`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_organization` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `organization_no` varchar(64) NOT NULL COMMENT '机构编号',
  `organization_name` varchar(100) NOT NULL COMMENT '机构名称',
  `organization_type` varchar(50) DEFAULT NULL COMMENT '机构类型：school、college、lab、base',
  `contact_name` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(30) DEFAULT NULL COMMENT '联系电话',
  `address` varchar(255) DEFAULT NULL COMMENT '地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_organization_organization_no` (`organization_no`),
  KEY `idx_sys_organization_name` (`organization_name`),
  KEY `idx_sys_organization_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='机构表；保存学校、学院、实验室、合作基地等机构信息。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_organization`
--

LOCK TABLES `sys_organization` WRITE;
/*!40000 ALTER TABLE `sys_organization` DISABLE KEYS */;
/*!40000 ALTER TABLE `sys_organization` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `user_no` varchar(64) NOT NULL COMMENT '用户编号',
  `username` varchar(64) NOT NULL COMMENT '登录账号',
  `password_hash` varchar(255) NOT NULL COMMENT '密码哈希',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `phone_number` varchar(30) DEFAULT NULL COMMENT '手机号',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `organization_id` bigint DEFAULT NULL COMMENT '所属机构 ID',
  `department_id` bigint DEFAULT NULL COMMENT '所属部门 ID',
  `user_type` varchar(50) DEFAULT NULL COMMENT '用户类型：admin、teacher、student、collector、reviewer',
  `avatar_url` varchar(500) DEFAULT NULL COMMENT '头像地址',
  `last_login_at` datetime DEFAULT NULL COMMENT '最后登录时间',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1 启用，0 禁用',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 未删除，1 已删除',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `created_by` bigint DEFAULT NULL COMMENT '创建人 ID',
  `updated_by` bigint DEFAULT NULL COMMENT '更新人 ID',
  `deleted_at` datetime DEFAULT NULL COMMENT '删除时间',
  `deleted_by` bigint DEFAULT NULL COMMENT '删除人 ID',
  `remark` varchar(500) DEFAULT NULL COMMENT '备注',
  `version` int NOT NULL DEFAULT '0' COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_sys_user_user_no` (`user_no`),
  UNIQUE KEY `uk_sys_user_username` (`username`),
  KEY `idx_sys_user_department_id` (`department_id`),
  KEY `idx_sys_user_organization_id` (`organization_id`),
  KEY `idx_sys_user_status` (`status`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表；保存系统用户，用于登录、采集、审核、上传、申报和审计。';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'Udfa8d33aa4554e3d9f','chong','$2a$10$KE.FdD0wsQLNcDDU9jgC8OCnrRQYzUCqprAk0ff0EhxfBnN44tKXy','系统管理员',NULL,NULL,NULL,NULL,NULL,NULL,'2026-07-13 00:18:50',1,0,'2026-07-12 21:23:43','2026-07-12 21:23:43',NULL,NULL,NULL,NULL,NULL,2);
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Dumping routines for database 'biomed_dev'
--
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-13  0:59:56
