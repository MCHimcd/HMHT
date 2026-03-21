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
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON
    UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

    -- 2. 设备注册表
    CREATE TABLE devices
    (
        id BIGINT
        AUTO_INCREMENT PRIMARY KEY,
    device_id   VARCHAR
        (50)  NOT NULL UNIQUE,
    device_name VARCHAR
        (100),
    user_id     BIGINT       NOT NULL,
    online      BOOLEAN      NOT NULL DEFAULT FALSE,
    smart_mode  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON
        UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_device_user
        FOREIGN KEY
        (user_id) REFERENCES users
        (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

        CREATE INDEX idx_device_user ON devices(user_id);

        -- 3. 传感器元数据
        CREATE TABLE sensor_meta
        (
            id BIGINT
            AUTO_INCREMENT PRIMARY KEY,
    device_id   VARCHAR
            (50) NOT NULL,
    sensor_key  VARCHAR
            (50) NOT NULL,
    name        VARCHAR
            (100),
    unit        VARCHAR
            (20),
    icon        VARCHAR
            (50),
    badge_color VARCHAR
            (20),
    bg_color    VARCHAR
            (20),
    warning_min DOUBLE,
    warning_max DOUBLE,
    UNIQUE KEY uk_device_sensor
            (device_id, sensor_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

            -- 4. 执行器元数据
            CREATE TABLE equipment_meta
            (
                id BIGINT
                AUTO_INCREMENT PRIMARY KEY,
    device_id      VARCHAR
                (50) NOT NULL,
    equipment_key  VARCHAR
                (50) NOT NULL,
    name           VARCHAR
                (100),
    icon           VARCHAR
                (50),
    controllable   BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE KEY uk_device_equipment
                (device_id, equipment_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

                -- 5. 告警记录
                CREATE TABLE alerts
                (
                    id BIGINT
                    AUTO_INCREMENT PRIMARY KEY,
    alert_id    VARCHAR
                    (50)  NOT NULL UNIQUE,
    device_id   VARCHAR
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

                    CREATE INDEX idx_alert_device ON alerts(device_id);
                    CREATE INDEX idx_alert_handled ON alerts(is_handled, created_at);

                    -- 6. 指令下发日志
                    CREATE TABLE command_logs
                    (
                        id BIGINT
                        AUTO_INCREMENT PRIMARY KEY,
    device_id     VARCHAR
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

                        CREATE INDEX idx_cmd_device ON command_logs(device_id, created_at);

                        -- 7. 农业问答聊天记录
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

                            -- 8. 验证码
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
