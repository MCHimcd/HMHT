ALTER TABLE equipment_type ADD COLUMN control_type VARCHAR(20) DEFAULT 'toggle';

UPDATE equipment_type SET control_type = 'slider' WHERE equipment_key = 'led_light';
