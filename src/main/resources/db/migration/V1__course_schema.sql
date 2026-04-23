-- ============================================================
-- V1: HMHT course edition schema
-- ============================================================

CREATE TABLE users
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone         VARCHAR(20)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    user_name     VARCHAR(100),
    gender        VARCHAR(10),
    birth_date    DATE,
    avatar_src    VARCHAR(500),
    role          VARCHAR(20)  NOT NULL DEFAULT 'user',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE nodes
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id    VARCHAR(50) NOT NULL UNIQUE,
    node_name  VARCHAR(100),
    user_id    BIGINT,
    online     BOOLEAN     NOT NULL DEFAULT FALSE,
    smart_mode BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_node_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_node_user ON nodes (user_id);

CREATE TABLE sensor_type
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_key  VARCHAR(50) NOT NULL UNIQUE,
    name        VARCHAR(100),
    unit        VARCHAR(20),
    icon        VARCHAR(50),
    badge_color VARCHAR(20),
    bg_color    VARCHAR(20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE node_sensor
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id     VARCHAR(50) NOT NULL,
    sensor_key  VARCHAR(50) NOT NULL,
    warning_min DOUBLE,
    warning_max DOUBLE,
    UNIQUE KEY uk_node_sensor (node_id, sensor_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE equipment_type
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_key VARCHAR(50)  NOT NULL UNIQUE,
    name          VARCHAR(100),
    icon          VARCHAR(50),
    controllable  BOOLEAN      NOT NULL DEFAULT TRUE,
    control_type  VARCHAR(20)  NOT NULL DEFAULT 'toggle'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE node_equipment
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id       VARCHAR(50) NOT NULL,
    equipment_key VARCHAR(50) NOT NULL,
    UNIQUE KEY uk_node_equipment (node_id, equipment_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE alerts
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    alert_id    VARCHAR(50) NOT NULL UNIQUE,
    node_id     VARCHAR(50) NOT NULL,
    type        VARCHAR(50) NOT NULL,
    type_name   VARCHAR(100),
    level       VARCHAR(20) NOT NULL,
    message     TEXT,
    source      VARCHAR(50),
    source_name VARCHAR(100),
    is_handled  BOOLEAN     NOT NULL DEFAULT FALSE,
    handle_time DATETIME,
    alert_time  DATETIME    NOT NULL,
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_alert_node ON alerts (node_id);
CREATE INDEX idx_alert_handled ON alerts (is_handled, created_at);

CREATE TABLE verification_codes
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    phone      VARCHAR(20) NOT NULL,
    code       VARCHAR(10) NOT NULL,
    expires_at DATETIME    NOT NULL,
    used       BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_vcode_phone ON verification_codes (phone, expires_at);

CREATE TABLE scheduled_tasks
(
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    node_id       VARCHAR(50) NOT NULL,
    user_id       BIGINT      NOT NULL,
    equipment_key VARCHAR(50) NOT NULL,
    action        VARCHAR(20) NOT NULL,
    task_name     VARCHAR(100),
    hour          INT         NOT NULL,
    minute        INT         NOT NULL,
    repeat_days   VARCHAR(20),
    enabled       BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_task_user ON scheduled_tasks (user_id);
CREATE INDEX idx_task_enabled ON scheduled_tasks (enabled);

CREATE TABLE operation_logs
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT,
    user_name      VARCHAR(100),
    operation_type VARCHAR(50) NOT NULL,
    target_type    VARCHAR(50),
    target_id      VARCHAR(100),
    detail         TEXT,
    ip_address     VARCHAR(50),
    created_at     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_oplog_type ON operation_logs (operation_type, created_at);
CREATE INDEX idx_oplog_user ON operation_logs (user_id, created_at);

CREATE TABLE user_node_access
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    node_id    VARCHAR(50) NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_node (user_id, node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_una_user ON user_node_access (user_id);
CREATE INDEX idx_una_node ON user_node_access (node_id);
