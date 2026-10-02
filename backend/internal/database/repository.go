package database

import (
	"context"
	"database/sql"
	"errors"
	"fmt"
	"time"

	"smartpark-backend/internal/models"
)

// Predefined database operations errors
var (
	ErrRecordNotFound = errors.New("requested entity record not found in storage")
	ErrSlotNotVacant  = errors.New("target database slot state confirms it is not vacant")
)

// Repository encapsulates all persistent data driver functions
type Repository struct {
	db *sql.DB
}

// NewRepository creates a data manipulation handler using the shared database pool
func NewRepository(pool *sql.DB) *Repository {
	return &Repository{db: pool}
}

// GetSlotByID pulls the current model metrics for an isolated parking slot
func (r *Repository) GetSlotByID(ctx context.Context, slotID string) (*models.Slot, error) {
	query := `SELECT id, floor_id, slot_number, slot_type, status, updated_at 
	          FROM slots WHERE id = ? LIMIT 1`

	slot := &models.Slot{}
	err := r.db.QueryRowContext(ctx, query, slotID).Scan(
		&slot.ID,
		&slot.FloorID,
		&slot.SlotNumber,
		&slot.SlotType,
		&slot.Status,
		&slot.UpdatedAt,
	)

	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return nil, ErrRecordNotFound
		}
		return nil, fmt.Errorf("failed fetching slot query: %w", err)
	}

	return slot, nil
}

// CreateParkingSession executes an ACID-compliant transaction to register a vehicle entry
func (r *Repository) CreateParkingSession(ctx context.Context, session *models.ParkingSession) error {
	// 1. Initialize the transaction block execution thread
	tx, err := r.db.BeginTx(ctx, &sql.TxOptions{Isolation: sql.LevelReadCommitted})
	if err != nil {
		return fmt.Errorf("failed to initialize session transaction scope: %w", err)
	}

	// Defer statement guarantees safety: rolls back actions automatically if an error causes an early return
	defer tx.Rollback()

	// 2. Secondary guardrail: verify the database slot state is still free under pessimistic locking strategy
	var currentStatus string
	checkQuery := `SELECT status FROM slots WHERE id = ? FOR UPDATE`
	err = tx.QueryRowContext(ctx, checkQuery, session.SlotID).Scan(&currentStatus)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return ErrRecordNotFound
		}
		return fmt.Errorf("failed executing slot safety status verify check: %w", err)
	}
	if currentStatus != "free" {
		return ErrSlotNotVacant
	}

	// 3. Mutate the target slot tracking status record to occupied
	updateSlotQuery := `UPDATE slots SET status = 'occupied', updated_at = ? WHERE id = ?`
	_, err = tx.ExecContext(ctx, updateSlotQuery, time.Now(), session.SlotID)
	if err != nil {
		return fmt.Errorf("failed updating slot target table row: %w", err)
	}

	// 4. Inject the new vehicle track ledger log inside the append-only sessions schema
	insertSessionQuery := `INSERT INTO parking_sessions 
		(id, ticket_number, barcode_value, slot_id, plate_number, vehicle_type, entry_time, status) 
		VALUES (?, ?, ?, ?, ?, ?, ?, ?)`
	
	_, err = tx.ExecContext(ctx, insertSessionQuery,
		session.ID,
		session.TicketNumber,
		session.BarcodeValue,
		session.SlotID,
		session.PlateNumber,
		session.VehicleType,
		session.EntryTime,
		session.Status,
	)
	if err != nil {
		return fmt.Errorf("failed to commit append-only session creation log: %w", err)
	}

	// 5. Commit the full transaction pipeline to persistent storage
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed to write data transaction modifications: %w", err)
	}

	return nil
}

// CompleteParkingSession updates the append-only session history during a checkout operation
func (r *Repository) CompleteParkingSession(ctx context.Context, sessionID string, fee float64, exitTime time.Time) error {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("failed to initialize checkout transaction: %w", err)
	}
	defer tx.Rollback()

	// 1. Resolve target slot relation from active session
	var slotID string
	findSlotQuery := `SELECT slot_id FROM parking_sessions WHERE id = ? FOR UPDATE`
	err = tx.QueryRowContext(ctx, findSlotQuery, sessionID).Scan(&slotID)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return ErrRecordNotFound
		}
		return fmt.Errorf("failed locating historical session map match: %w", err)
	}

	// 2. Finalize the active session ledger block record
	updateSessionQuery := `UPDATE parking_sessions 
		SET exit_time = ?, calculated_fee = ?, status = 'completed' 
		WHERE id = ?`
	_, err = tx.ExecContext(ctx, updateSessionQuery, exitTime, fee, sessionID)
	if err != nil {
		return fmt.Errorf("failed modifying transactional session record: %w", err)
	}

	// 3. Free up the slot state back to free for future allocations
	freeSlotQuery := `UPDATE slots SET status = 'free', updated_at = ? WHERE id = ?`
	_, err = tx.ExecContext(ctx, freeSlotQuery, time.Now(), slotID)
	if err != nil {
		return fmt.Errorf("failed updating target slot allocation visibility row: %w", err)
	}

	// 4. Safely close out the pipeline updates
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("failed processing closing data transaction updates: %w", err)
	}

	return nil
}
