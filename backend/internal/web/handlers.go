package web

import (
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"time"

	"smartpark-backend/internal/cache"
	"smartpark-backend/internal/database"
	"smartpark-backend/internal/models"

	"://github.com"
)

// Handlers encapsulates the structural dependencies needed for routing web traffic
type Handlers struct {
	Repo  *database.Repository
	Cache *cache.RedisClient
}

// NewHandlers instantiates a web controller mapped to our active persistence layers
func NewHandlers(repo *database.Repository, cache *cache.RedisClient) *Handlers {
	return &Handlers{
		Repo:  repo,
		Cache: cache,
	}
}

// HandleReserveSlot processes incoming POST /api/sessions/reserve operations natively
func (h *Handlers) HandleReserveSlot(w http.ResponseWriter, r *http.Request) {
	// 1. Enforce correct structural network method access
	if r.Method != http.MethodPost {
		writeJSONError(w, "Method Not Allowed. Use POST.", http.StatusMethodNotAllowed)
		return
	}

	// 2. Decode the incoming JSON network payload array
	var req models.ReservationRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		writeJSONError(w, "Malformed JSON request structure payload", http.StatusBadRequest)
		return
	}

	// Basic constraint boundary checking validations
	if req.SlotID == "" || req.PlateNumber == "" || req.VehicleType == "" {
		writeJSONError(w, "Missing mandatory parameters fields: slot_id, plate_number, and vehicle_type are required", http.StatusBadRequest)
		return
	}

	ctx := r.Context()
	uniqueLockToken := uuid.New().String()

	// 3. Acquire high-speed Distributed Mutex Lock inside Redis Cache
	// Short-lived 2-second lock window blocks parallel entry gates processing the same spot simultaneously
	lockAcquired, err := h.Cache.AcquireSlotLock(ctx, req.SlotID, uniqueLockToken, 2*time.Second)
	if err != nil {
		writeJSONError(w, fmt.Sprintf("Internal state cache check error: %v", err), http.StatusInternalServerError)
		return
	}
	if !lockAcquired {
		writeJSONError(w, "The target parking slot is currently processing a transaction, please retry", http.StatusConflict)
		return
	}

	// Ensure our lock execution cleanup release script always triggers on exit scope
	defer h.Cache.ReleaseSlotLock(ctx, req.SlotID, uniqueLockToken)

	// 4. Instantiate the core business transaction mapping schema
	now := time.Now()
	generatedSessionID := uuid.New().String()
	ticketToken := fmt.Sprintf("TKT-%s-%d", req.SlotID, now.Unix())

	sessionRecord := &models.ParkingSession{
		ID:           generatedSessionID,
		TicketNumber: ticketToken,
		BarcodeValue: ticketToken, // In V1, the scan value mirrors the unique human-readable ticket ID string
		SlotID:       req.SlotID,
		PlateNumber:  req.PlateNumber,
		VehicleType:  req.VehicleType,
		EntryTime:    now,
		Status:       "active",
	}

	// 5. Execute persistent storage write mutations inside an ACID transaction block
	err = h.Repo.CreateParkingSession(ctx, sessionRecord)
	if err != nil {
		if errors.Is(err, database.ErrSlotNotVacant) {
			writeJSONError(w, "The requested parking spot is already occupied in the database", http.StatusConflict)
			return
		}
		if errors.Is(err, database.ErrRecordNotFound) {
			writeJSONError(w, "The designated slot ID does not exist in our catalog inventory", http.StatusNotFound)
			return
		}
		writeJSONError(w, fmt.Sprintf("Persistent storage database execution failure: %v", err), http.StatusInternalServerError)
		return
	}

	// 6. Sync state balances back down to update real-time cache tracking systems
	err = h.Cache.UpdateLiveOccupancyCounters(ctx, req.SlotID, "occupied", req.VehicleType)
	if err != nil {
		// Log execution warning but don't fail request since persistent data successfully saved in SQL
		fmt.Printf("[⚠️ CACHE-WARN] Failed tracking counter updates for slot %s: %v\n", req.SlotID, err)
	}

	// 7. Write the clean response payload back across the open network socket channel
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(http.StatusCreated)
	json.NewEncoder(w).Encode(sessionRecord)
}

// writeJSONError acts as a utility helper helper to standardize our REST error payloads
func writeJSONError(w http.ResponseWriter, message string, statusCode int) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(statusCode)
	json.NewEncoder(w).Encode(map[string]string{"error": message})
}
