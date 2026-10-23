-- 对齐 sla-wecom-notifier/doc/复盘明细.xlsx
-- 实际建表由 MyBatis SlaSchemaMapper 在启动时执行 CREATE TABLE IF NOT EXISTS

CREATE TABLE IF NOT EXISTS zabbix.sla_fault_category (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  fault_category    VARCHAR(64) NOT NULL COMMENT '故障类别',
  fault_subcategory VARCHAR(64) NOT NULL COMMENT '故障子类',
  fault_desc        VARCHAR(512) NULL COMMENT '故障描述（说明）',
  count_sla         VARCHAR(8) NOT NULL COMMENT '是否计算SLA：是/否',
  sort_order        INT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_fault_cat (fault_category, fault_subcategory)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复盘明细-故障分类字典';

CREATE TABLE IF NOT EXISTS zabbix.sla_review_event (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  period_month      CHAR(6) NOT NULL COMMENT 'yyyyMM，对齐复盘明细-yyyyMM',
  system_level      VARCHAR(16) NOT NULL COMMENT '父/子系统',
  system_name       VARCHAR(256) NOT NULL COMMENT '系统名称',
  down_start        DATETIME NOT NULL COMMENT '宕机开始时间',
  down_end          DATETIME NULL COMMENT '宕机结束时间',
  exclude_time      DATETIME NULL COMMENT '排除结束时间',
  owner_name        VARCHAR(64) NULL COMMENT '责任人',
  fault_category    VARCHAR(64) NULL COMMENT '故障类别',
  fault_subcategory VARCHAR(64) NULL COMMENT '故障子类',
  fault_desc        VARCHAR(512) NULL COMMENT '故障描述',
  count_sla         VARCHAR(8) NULL COMMENT '是否计算SLA',
  resolution_process TEXT NULL COMMENT '解决过程',
  root_cause         TEXT NULL COMMENT '根本原因',
  improvement        TEXT NULL COMMENT '改进措施',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_event (period_month, system_level, system_name, down_start, down_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='复盘明细-故障录入';

CREATE TABLE IF NOT EXISTS zabbix.sla_system_owner (
  id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  system_level      VARCHAR(16) NOT NULL COMMENT '子系统/父系统',
  system_name       VARCHAR(256) NOT NULL COMMENT '系统名称',
  owner_name        VARCHAR(64) NOT NULL COMMENT '责任人',
  mobile            VARCHAR(32) NULL COMMENT '手机号码',
  created_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_owner (system_level, system_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统责任人字典';

CREATE TABLE IF NOT EXISTS zabbix.sla_system_owner (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  system_level   VARCHAR(16) NOT NULL COMMENT '子系统/父系统',
  system_name    VARCHAR(256) NOT NULL COMMENT '系统名称',
  owner_name     VARCHAR(64) NOT NULL COMMENT '责任人',
  mobile         VARCHAR(32) NULL COMMENT '手机号码',
  created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_owner (system_level, system_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统责任人字典';
