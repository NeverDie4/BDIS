CREATE TABLE IF NOT EXISTS sys_file_resource (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_no VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    original_filename VARCHAR(255) NULL,
    file_type VARCHAR(50) NULL,
    file_format VARCHAR(50) NULL,
    file_size BIGINT NULL,
    file_url VARCHAR(500) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    storage_type VARCHAR(50) NULL,
    content_type VARCHAR(150) NULL,
    uploader_id BIGINT NULL,
    uploader_name VARCHAR(100) NULL,
    uploaded_at DATETIME NULL,
    status TINYINT DEFAULT 1,
    is_deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at DATETIME NULL,
    deleted_by BIGINT NULL,
    remark VARCHAR(500) NULL,
    version INT DEFAULT 0,
    UNIQUE KEY uk_sys_file_resource_file_no (file_no),
    KEY idx_sys_file_resource_file_type (file_type),
    KEY idx_sys_file_resource_uploader_id (uploader_id),
    KEY idx_sys_file_resource_uploaded_at (uploaded_at)
);

CREATE TABLE IF NOT EXISTS sys_file_business (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id BIGINT NOT NULL,
    biz_type VARCHAR(100) NOT NULL,
    biz_id BIGINT NOT NULL,
    file_usage VARCHAR(50) NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    remark VARCHAR(500) NULL,
    KEY idx_sys_file_business_file_id (file_id),
    KEY idx_sys_file_business_biz (biz_type, biz_id)
);

CREATE TABLE IF NOT EXISTS log_login (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NULL,
    username VARCHAR(100) NULL,
    login_result VARCHAR(50) NOT NULL,
    failure_reason VARCHAR(500) NULL,
    ip_address VARCHAR(100) NULL,
    user_agent VARCHAR(500) NULL,
    logged_in_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_log_login_user_id (user_id),
    KEY idx_log_login_logged_in_at (logged_in_at)
);

CREATE TABLE IF NOT EXISTS log_operation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    operator_id BIGINT NULL,
    operator_name VARCHAR(100) NULL,
    operation_module VARCHAR(100) NULL,
    operation_type VARCHAR(100) NULL,
    biz_type VARCHAR(100) NULL,
    biz_id BIGINT NULL,
    operation_result VARCHAR(50) NULL,
    request_method VARCHAR(20) NULL,
    request_uri VARCHAR(500) NULL,
    ip_address VARCHAR(100) NULL,
    user_agent VARCHAR(500) NULL,
    error_message VARCHAR(1000) NULL,
    operation_time DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_log_operation_operator_id (operator_id),
    KEY idx_log_operation_module (operation_module),
    KEY idx_log_operation_time (operation_time)
);

CREATE TABLE IF NOT EXISTS log_data_change (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    table_name VARCHAR(100) NOT NULL,
    record_id BIGINT NOT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(100) NULL,
    before_data LONGTEXT NULL,
    after_data LONGTEXT NULL,
    change_reason VARCHAR(500) NULL,
    operation_time DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_log_data_change_table_record (table_name, record_id),
    KEY idx_log_data_change_time (operation_time)
);

CREATE TABLE IF NOT EXISTS log_file_access (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    file_id BIGINT NOT NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(100) NULL,
    access_type VARCHAR(50) NOT NULL,
    access_result VARCHAR(50) NULL,
    ip_address VARCHAR(100) NULL,
    user_agent VARCHAR(500) NULL,
    operation_time DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_log_file_access_file_id (file_id),
    KEY idx_log_file_access_operator_id (operator_id),
    KEY idx_log_file_access_time (operation_time)
);

CREATE TABLE IF NOT EXISTS log_data_sync (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    sync_type VARCHAR(50) NOT NULL,
    source_type VARCHAR(50) NULL,
    target_type VARCHAR(100) NULL,
    sync_status VARCHAR(50) NOT NULL,
    success_count INT DEFAULT 0,
    failure_count INT DEFAULT 0,
    failure_reason VARCHAR(1000) NULL,
    operator_id BIGINT NULL,
    operator_name VARCHAR(100) NULL,
    operation_time DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_log_data_sync_type (sync_type),
    KEY idx_log_data_sync_status (sync_status),
    KEY idx_log_data_sync_time (operation_time)
);

CREATE TABLE IF NOT EXISTS soap_sync_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_no VARCHAR(64) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    service_name VARCHAR(150) NULL,
    method_name VARCHAR(150) NULL,
    sync_direction VARCHAR(50) NULL,
    sync_status VARCHAR(50) NOT NULL,
    mock TINYINT DEFAULT 1,
    retry_count INT DEFAULT 0,
    last_sync_at DATETIME NULL,
    status TINYINT DEFAULT 1,
    is_deleted TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BIGINT NULL,
    updated_by BIGINT NULL,
    deleted_at DATETIME NULL,
    deleted_by BIGINT NULL,
    remark VARCHAR(500) NULL,
    version INT DEFAULT 0,
    UNIQUE KEY uk_soap_sync_task_task_no (task_no),
    KEY idx_soap_sync_task_service_name (service_name),
    KEY idx_soap_sync_task_status (sync_status)
);

CREATE TABLE IF NOT EXISTS soap_exchange_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    exchange_no VARCHAR(64) NOT NULL,
    task_id BIGINT NOT NULL,
    service_name VARCHAR(150) NULL,
    method_name VARCHAR(150) NULL,
    request_xml LONGTEXT NULL,
    response_xml LONGTEXT NULL,
    exchange_status VARCHAR(50) NOT NULL,
    error_message VARCHAR(1000) NULL,
    parsed_payload LONGTEXT NULL,
    called_by BIGINT NULL,
    called_by_name VARCHAR(100) NULL,
    called_at DATETIME NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    remark VARCHAR(500) NULL,
    UNIQUE KEY uk_soap_exchange_record_exchange_no (exchange_no),
    KEY idx_soap_exchange_record_task_id (task_id),
    KEY idx_soap_exchange_record_called_at (called_at)
);

CREATE TABLE IF NOT EXISTS stat_dashboard_snapshot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    snapshot_date DATE NOT NULL,
    herb_count INT DEFAULT 0,
    base_count INT DEFAULT 0,
    growth_record_count INT DEFAULT 0,
    pending_task_count INT DEFAULT 0,
    dashboard_data LONGTEXT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    remark VARCHAR(500) NULL,
    UNIQUE KEY uk_stat_dashboard_snapshot_date (snapshot_date)
);
