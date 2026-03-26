-- ============================================================
-- V1: HMHT 初始化表结构
-- ============================================================

-- 1. 用户表
CREATE TABLE users
(
    id BIGINT
    AUTO_INCREMENT PRIMARY KEY,
    phone         VARCHAR
    (20)  NOT NULL UNIQUE,
    password_hash VARCHAR
    (255) NOT NULL,
    user_name     VARCHAR
    (100),
    gender        VARCHAR
    (10),
    birth_date    DATE,
    avatar_src    VARCHAR
    (500),
    role          VARCHAR
    (20)  NOT NULL DEFAULT 'user',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON
    UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    -- 2. 节点注册表
    CREATE TABLE nodes
    (
        id BIGINT
        AUTO_INCREMENT PRIMARY KEY,
    node_id    VARCHAR
        (50)  NOT NULL UNIQUE,
    node_name  VARCHAR
        (100),
    user_id    BIGINT       NOT NULL,
    online     BOOLEAN      NOT NULL DEFAULT FALSE,
    smart_mode BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON
        UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_node_user
        FOREIGN KEY
        (user_id) REFERENCES users
        (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE INDEX idx_node_user ON nodes(user_id);

        -- 3. 传感器类型 (全局)
        CREATE TABLE sensor_type
        (
            id BIGINT
            AUTO_INCREMENT PRIMARY KEY,
    sensor_key  VARCHAR
            (50)  NOT NULL UNIQUE,
    name        VARCHAR
            (100),
    unit        VARCHAR
            (20),
    icon        VARCHAR
            (50),
    badge_color VARCHAR
            (20),
    bg_color    VARCHAR
            (20)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

            -- 4. 节点-传感器关联 (每节点阈值)
            CREATE TABLE node_sensor
            (
                id BIGINT
                AUTO_INCREMENT PRIMARY KEY,
    node_id     VARCHAR
                (50) NOT NULL,
    sensor_key  VARCHAR
                (50) NOT NULL,
    warning_min DOUBLE,
    warning_max DOUBLE,
    UNIQUE KEY uk_node_sensor
                (node_id, sensor_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                -- 5. 设备类型 (全局)
                CREATE TABLE equipment_type
                (
                    id BIGINT
                    AUTO_INCREMENT PRIMARY KEY,
    equipment_key VARCHAR
                    (50)  NOT NULL UNIQUE,
    name          VARCHAR
                    (100),
    icon          VARCHAR
                    (50),
    controllable  BOOLEAN NOT NULL DEFAULT TRUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                    -- 6. 节点-设备关联
                    CREATE TABLE node_equipment
                    (
                        id BIGINT
                        AUTO_INCREMENT PRIMARY KEY,
    node_id       VARCHAR
                        (50) NOT NULL,
    equipment_key VARCHAR
                        (50) NOT NULL,
    UNIQUE KEY uk_node_equipment
                        (node_id, equipment_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                        -- 7. 告警记录
                        CREATE TABLE alerts
                        (
                            id BIGINT
                            AUTO_INCREMENT PRIMARY KEY,
    alert_id    VARCHAR
                            (50)  NOT NULL UNIQUE,
    node_id     VARCHAR
                            (50)  NOT NULL,
    type        VARCHAR
                            (50)  NOT NULL,
    type_name   VARCHAR
                            (100),
    level       VARCHAR
                            (20)  NOT NULL,
    message     TEXT,
    source      VARCHAR
                            (50),
    source_name VARCHAR
                            (100),
    is_handled  BOOLEAN      NOT NULL DEFAULT FALSE,
    handle_time DATETIME,
    alert_time  DATETIME     NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                            CREATE INDEX idx_alert_node ON alerts(node_id);
                            CREATE INDEX idx_alert_handled ON alerts(is_handled, created_at);

                            -- 8. 指令下发日志
                            CREATE TABLE command_logs
                            (
                                id BIGINT
                                AUTO_INCREMENT PRIMARY KEY,
    node_id       VARCHAR
                                (50)  NOT NULL,
    user_id       BIGINT,
    target_device VARCHAR
                                (50)  NOT NULL,
    command       VARCHAR
                                (50)  NOT NULL,
    source        VARCHAR
                                (20)  NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                                CREATE INDEX idx_cmd_node ON command_logs(node_id, created_at);

                                -- 9. 农业问答聊天记录
                                CREATE TABLE chat_messages
                                (
                                    id BIGINT
                                    AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    role       VARCHAR
                                    (20)  NOT NULL,
    content    TEXT         NOT NULL,
    image_uri  VARCHAR
                                    (500),
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_user FOREIGN KEY
                                    (user_id) REFERENCES users
                                    (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                                    CREATE INDEX idx_chat_user ON chat_messages(user_id, created_at);

                                    -- 10. 验证码
                                    CREATE TABLE verification_codes
                                    (
                                        id BIGINT
                                        AUTO_INCREMENT PRIMARY KEY,
    phone      VARCHAR
                                        (20)  NOT NULL,
    code       VARCHAR
                                        (10)  NOT NULL,
    expires_at DATETIME     NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                                        CREATE INDEX idx_vcode_phone ON verification_codes(phone, expires_at);

                                        -- 11. 定时任务
                                        CREATE TABLE scheduled_tasks
                                        (
                                            id BIGINT
                                            AUTO_INCREMENT PRIMARY KEY,
    node_id       VARCHAR
                                            (50)  NOT NULL,
    user_id       BIGINT       NOT NULL,
    equipment_key VARCHAR
                                            (50)  NOT NULL,
    action        VARCHAR
                                            (20)  NOT NULL,
    task_name     VARCHAR
                                            (100),
    hour          INT          NOT NULL,
    minute        INT          NOT NULL,
    repeat_days   VARCHAR
                                            (20),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON
                                            UPDATE CURRENT_TIMESTAMP
                                            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                                            CREATE INDEX idx_task_user ON scheduled_tasks(user_id);
                                            CREATE INDEX idx_task_enabled ON scheduled_tasks(enabled);

                                            -- 12. 操作日志
                                            CREATE TABLE operation_logs
                                            (
                                                id BIGINT
                                                AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT,
    user_name      VARCHAR
                                                (100),
    operation_type VARCHAR
                                                (50)  NOT NULL,
    target_type    VARCHAR
                                                (50),
    target_id      VARCHAR
                                                (100),
    detail         TEXT,
    ip_address     VARCHAR
                                                (50),
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                                                CREATE INDEX idx_oplog_type ON operation_logs(operation_type, created_at);
                                                CREATE INDEX idx_oplog_user ON operation_logs(user_id, created_at);

                                                -- 13. 用户-节点访问授权
                                                CREATE TABLE user_node_access
                                                (
                                                    id BIGINT
                                                    AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT      NOT NULL,
    node_id    VARCHAR
                                                    (50) NOT NULL,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_node
                                                    (user_id, node_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                                                    CREATE INDEX idx_una_user ON user_node_access(user_id);
                                                    CREATE INDEX idx_una_node ON user_node_access(node_id);
