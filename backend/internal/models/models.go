package models

import "time"

// Slot defines the structural data model for an individual parking spot
type Slot struct {
	ID         string    `json:"id"`
	FloorID    string    `json:"floor_id"`
	SlotNumber string    `json:"slot_number"`
	SlotType   string    `json:"slot_type"` // e.g., "car", "bike", "valet"
	Status     string    `json:"status"`    // e.g., "free", "occupied", "blocked"
	UpdatedAt  time.Time `json:"updated_at"`
}

// ReservationRequest maps the incoming JSON payload when a gate operator ticks a car in
type ReservationRequest struct {
	SlotID       string `json:"slot_id"`
	PlateNumber  string `json:"plate_number"`
	VehicleType  string `json:"vehicle_type"`
}

// ParkingSession records the transactional lifecycle logs of a vehicle stay
type ParkingSession struct {
	ID           string     `json:"id"`
	TicketNumber string     `json:"ticket_number"`
	BarcodeValue string     `json:"barcode_value"`
	SlotID       string     `json:"slot_id"`
	PlateNumber  string     `json:"plate_number"`
	VehicleType  string     `json:"vehicle_type"`
	EntryTime    time.Time  `json:"entry_time"`
	ExitTime     *time.Time `json:"exit_time,omitempty"` // Pointer allows null values until checkout
	CalculatedFee float64    `json:"calculated_fee"`
	Status       string     `json:"status"`              // e.g., "active", "completed", "lost_ticket"
}
