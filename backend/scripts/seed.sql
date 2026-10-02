-- Seed baseline layout tracking levels
INSERT INTO floors (id, name, display_order) VALUES 
('floor_l1', 'Level 1 - Ground', 1),
('floor_l2', 'Level 2 - Basement', 2);

-- Seed physical mock slot items matching our API reservation capabilities
INSERT INTO slots (id, floor_id, slot_number, slot_type, status) VALUES 
('slot_101', 'floor_l1', 'A-101', 'car', 'free'),
('slot_102', 'floor_l1', 'A-102', 'car', 'free'),
('slot_103', 'floor_l1', 'B-101', 'bike', 'free'),
('slot_201', 'floor_l2', 'C-101', 'car', 'free');
