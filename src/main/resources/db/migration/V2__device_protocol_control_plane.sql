-- ============================================================
-- V2: 设备协议控制面（能力模型 + 命令闭环 + 接入审计）
-- ============================================================

-- 1) 设备档案
CREATE TABLE device_profile
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id        VARCHAR(50)  NOT NULL UNIQUE,
    node_id          VARCHAR(50),
    display_name     VARCHAR(100),
    hardware_model   VARCHAR(100),
    firmware_version VARCHAR(50),
    vendor           VARCHAR(100),
    online           BOOLEAN      NOT NULL DEFAULT FALSE,
    last_seen_at     DATETIME,
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_device_profile_node ON device_profile(node_id);

-- 2) 设备能力快照
CREATE TABLE device_capability
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id          VARCHAR(50) NOT NULL,
    capability_version VARCHAR(20) NOT NULL,
    checksum           VARCHAR(80),
    reported_at        DATETIME,
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_device_capability_device ON device_capability(device_id, created_at);

-- 3) 传感器能力明细
CREATE TABLE capability_sensor
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id          VARCHAR(50)  NOT NULL,
    capability_version VARCHAR(20)  NOT NULL,
    sensor_key         VARCHAR(80)  NOT NULL,
    data_type          VARCHAR(30)  NOT NULL,
    unit               VARCHAR(20),
    min_value          DOUBLE,
    max_value          DOUBLE,
    required_field     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at         DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cap_sensor (device_id, capability_version, sensor_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4) 设备能力明细
CREATE TABLE capability_equipment
(
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id          VARCHAR(50) NOT NULL,
    capability_version VARCHAR(20) NOT NULL,
    equipment_key      VARCHAR(80) NOT NULL,
    controllable       BOOLEAN     NOT NULL DEFAULT TRUE,
    actions_json       TEXT,
    created_at         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cap_equipment (device_id, capability_version, equipment_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5) 命令请求主表
CREATE TABLE command_request
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    command_id  VARCHAR(80) NOT NULL UNIQUE,
    device_id   VARCHAR(50) NOT NULL,
    target_key  VARCHAR(80) NOT NULL,
    action      VARCHAR(80) NOT NULL,
    status      VARCHAR(30) NOT NULL,
    trace_id    VARCHAR(80),
    timeout_ms  INT,
    source_topic VARCHAR(200),
    relay_topic  VARCHAR(200),
    raw_payload TEXT,
    retry_count INT         NOT NULL DEFAULT 0,
    sent_at     DATETIME,
    acked_at    DATETIME,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cmd_req_device_created ON command_request(device_id, created_at);
CREATE INDEX idx_cmd_req_status_created ON command_request(status, created_at);

-- 6) 命令 ACK 表
CREATE TABLE command_ack
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    command_id  VARCHAR(80) NOT NULL,
    device_id   VARCHAR(50) NOT NULL,
    status      VARCHAR(30) NOT NULL,
    ack_code    INT,
    ack_message VARCHAR(255),
    trace_id    VARCHAR(80),
    source_topic VARCHAR(200),
    raw_payload TEXT,
    acked_at    DATETIME,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cmd_ack_command_id ON command_ack(command_id);
CREATE INDEX idx_cmd_ack_device_created ON command_ack(device_id, created_at);

-- 7) 命令重试日志
CREATE TABLE command_retry_log
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    command_id   VARCHAR(80) NOT NULL,
    attempt_no   INT         NOT NULL,
    retry_reason VARCHAR(255),
    retry_at     DATETIME,
    created_at   DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_cmd_retry_command ON command_retry_log(command_id, created_at);

-- 8) 设备影子
CREATE TABLE device_shadow
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    device_id  VARCHAR(50) NOT NULL UNIQUE,
    shadow_json TEXT,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 9) 接入审计
CREATE TABLE ingest_audit
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_type         VARCHAR(30)  NOT NULL,
    source_topic     VARCHAR(200) NOT NULL,
    device_id        VARCHAR(50),
    protocol_version VARCHAR(20),
    trace_id         VARCHAR(80),
    status           VARCHAR(20)  NOT NULL,
    error_message    VARCHAR(500),
    created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ingest_audit_device_created ON ingest_audit(device_id, created_at);
CREATE INDEX idx_ingest_audit_type_created ON ingest_audit(msg_type, created_at);

-- 10) 死信队列
CREATE TABLE ingest_deadletter
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_type     VARCHAR(30)  NOT NULL,
    source_topic VARCHAR(200) NOT NULL,
    device_id    VARCHAR(50),
    trace_id     VARCHAR(80),
    reason       VARCHAR(500) NOT NULL,
    raw_payload  TEXT,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_ingest_deadletter_device_created ON ingest_deadletter(device_id, created_at);
CREATE INDEX idx_ingest_deadletter_type_created ON ingest_deadletter(msg_type, created_at);
