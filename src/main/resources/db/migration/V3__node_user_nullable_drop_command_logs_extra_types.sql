-- ============================================================
-- V3: nodes.user_id 可空 + 删除冗余 command_logs + 新增传感器/设备类型
-- ============================================================

-- 1) nodes.user_id 改为可空，支持硬件自动注册（无归属用户）
ALTER TABLE nodes DROP FOREIGN KEY fk_node_user;
ALTER TABLE nodes MODIFY COLUMN user_id BIGINT NULL;
ALTER TABLE nodes ADD CONSTRAINT fk_node_user FOREIGN KEY (user_id) REFERENCES users(id);

-- 2) 删除冗余的 V1 command_logs 表（功能已被 V2 command_request 完全替代）
DROP TABLE IF EXISTS command_logs;

-- 3) 新增常见农业传感器类型
INSERT IGNORE INTO sensor_type (sensor_key, name, unit, icon, badge_color, bg_color) VALUES
    ('soilPh',    '土壤pH值',   '',    '🧪', '#AB47BC', '#F3E5F5'),
    ('soilN',     '土壤氮含量', 'mg/kg', '🟢', '#4CAF50', '#E8F5E9'),
    ('soilP',     '土壤磷含量', 'mg/kg', '🔵', '#2196F3', '#E3F2FD'),
    ('soilK',     '土壤钾含量', 'mg/kg', '🟠', '#FF9800', '#FFF3E0'),
    ('windSpeed', '风速',       'm/s',   '💨', '#26C6DA', '#E0F7FA'),
    ('rainfall',  '降雨量',     'mm',    '🌧️', '#5C6BC0', '#E8EAF6'),
    ('pressure',  '大气压',     'hPa',   '🌐', '#78909C', '#ECEFF1');

-- 4) 新增常见农业设备类型
INSERT IGNORE INTO equipment_type (equipment_key, name, icon, controllable) VALUES
    ('fan',             '排风扇',   '🌀', TRUE),
    ('heater',          '加热器',   '🔥', TRUE),
    ('shade',           '遮阳帘',   '🪟', TRUE),
    ('ventilation',     '通风窗',   '🪁', TRUE),
    ('fertilizer_pump', '施肥泵',   '🧴', TRUE),
    ('mist_sprayer',    '喷雾器',   '🌫️', TRUE);
