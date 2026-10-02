-- =====================================================================
-- SMARTPARK STRUCTURAL SCHEMA DEFINITION (MySQL 8.0+)
-- =====================================================================

-- 1. FLOORS: Structural vertical layout levels
CREATE TABLE IF NOT EXISTS floors (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    display_order INT NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. SLOTS: Individual parking spot inventories
CREATE TABLE IF NOT EXISTS slots (
    id VARCHAR(36) PRIMARY KEY,
    floor_id VARCHAR(36) NOT NULL,
    slot_number VARCHAR(20) NOT NULL,
    slot_type ENUM('car', 'bike', 'valet', 'accessible') NOT NULL,
    status ENUM('free', 'occupied', 'blocked', 'flagged') DEFAULT 'free',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (floor_id) REFERENCES floors(id) ON DELETE CASCADE,
    UNIQUE KEY uq_floor_slot (floor_id, slot_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexing optimized for the repository's 'FOR UPDATE' slot isolation lookups
CREATE INDEX idx_slots_status_type ON slots(id, status, slot_type);

-- 3. PARKING_SESSIONS: Main append-only transaction tracker ledger
CREATE TABLE IF NOT EXISTS parking_sessions (
    id VARCHAR(36) PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    barcode_value VARCHAR(100) NOT NULL UNIQUE,
    slot_id VARCHAR(36) NOT NULL,
    plate_number VARCHAR(20) NOT NULL,
    vehicle_type ENUM('car', 'bike', 'taxi', 'valet') NOT NULL,
    entry_time TIMESTAMP NOT NULL,
    exit_time TIMESTAMP NULL DEFAULT NULL,
    calculated_fee DECIMAL(10, 2) DEFAULT 0.00,
    status ENUM('active', 'completed', 'lost_ticket', 'disputed') DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (slot_id) REFERENCES slots(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Query optimization indices for checkout searches and management log pagination
CREATE INDEX idx_sessions_active ON parking_sessions(status, plate_number);
CREATE INDEX idx_sessions_entry_time ON parking_sessions(entry_time);
