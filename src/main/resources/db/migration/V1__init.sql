-- ============================================================
-- V1: HMHT 初始化表结构 (device→node 重构版)
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

    -- 2. 节点注册表 (原 devices)
    CREATE TABLE nodes
    (
        id BIGINT
        AUTO_INCREMENT PRIMARY KEY,
    node_id   VARCHAR
        (50)  NOT NULL UNIQUE,
    node_name VARCHAR
        (100),
    user_id   BIGINT       NOT NULL,
    online    BOOLEAN      NOT NULL DEFAULT FALSE,
    smart_mode BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON
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
