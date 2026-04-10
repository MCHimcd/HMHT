-- ============================================================
-- V4: 补充常用设备类型（V3 遗漏 water_pump / led_light / camera）
-- ============================================================

INSERT IGNORE INTO equipment_type (equipment_key, name, icon, controllable) VALUES
    ('water_pump', '水泵',   '💦', TRUE),
    ('led_light',  '补光灯', '💡', TRUE),
    ('camera',     '摄像头', '📷', FALSE);
