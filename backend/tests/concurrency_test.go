package tests

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/http/httptest"
	"sync"
	"sync/atomic"
	"testing"
	"time"

	"smartpark-backend/internal/cache"
	"smartpark-backend/internal/database"
	"smartpark-backend/internal/models"
	"smartpark-backend/internal/web"

	"://github.com"
	"://github.com"
)

// TestConcurrentReservationRaceConditions simulates 50 concurrent incoming gate requests 
// hitting a single available parking slot simultaneously to verify thread-safety.
func TestConcurrentReservationRaceConditions(t *testing.T) {
	// 1. Spin up an in-memory Redis mock instance to avoid needing a live running cache server
	mockRedisServer, err := miniredis.Run()
	if err != nil {
		t.Fatalf("Failed to initialize mock Redis engine setup: %v", err)
	}
	defer mockRedisServer.Close()

	cacheClient, err := cache.NewRedisClient(mockRedisServer.Addr())
	if err != nil {
		t.Fatalf("Failed to hook into mock cache client handler: %v", err)
	}

	// 2. Spin up an in-memory SQL mock layer to decouple persistent database constraints
	mockDbPool, sqlMockHandle, err := sqlmock.New()
	if err != nil {
		t.Fatalf("Failed to open virtual SQL connection thread: %v", err)
	}
	defer mockDbPool.Close()

	repoLayer := database.NewRepository(mockDbPool)
	webControllers := web.NewHandlers(repoLayer, cacheClient)

	// 3. Configure mock database expectations for the single request that successfully acquires the lock
	// Pessimistic Lock check row fetch expectation
	sqlMockHandle.ExpectBegin()
	sqlMockHandle.ExpectQuery("SELECT status FROM slots WHERE id = \\? FOR UPDATE").
		WithArgs("slot_101").
		WillReturnRows(sqlmock.NewRows([]string{"status"}).AddRow("free"))
	
	// Slot update row state expectation
	sqlMockHandle.ExpectExec("UPDATE slots SET status = 'occupied'").
		WithArgs(sqlmock.AnyArg(), "slot_101").
		WillReturnResult(sqlmock.NewResult(1, 1))
	
	// Append-only ledger session entry injection expectation
	sqlMockHandle.ExpectExec("INSERT INTO parking_sessions").
		WithArgs(sqlmock.AnyArg(), sqlmock.AnyArg(), sqlmock.AnyArg(), "slot_101", sqlmock.AnyArg(), "car", sqlmock.AnyArg(), "active").
		WillReturnResult(sqlmock.NewResult(1, 1))
	sqlMockHandle.ExpectCommit()

	// 4. Setup structural multi-threaded orchestration barriers
	const concurrentRequestCount = 50
	var synchronizationStartWaitBarrier sync.WaitGroup
	var concurrentExecutionFinishTracker sync.WaitGroup
	
	// Atomic integers to track absolute response categorizations safely across execution routines
	var successfulAllocationsCounter int32
	var rejectedConflictReservationsCounter int32
	var processingErrorsCounter int32

	synchronizationStartWaitBarrier.Add(1) // Hold line gate block variable
	concurrentExecutionFinishTracker.Add(concurrentRequestCount)

	// 5. Fire 50 independent concurrent goroutines targeting the exact same handler method
	for i := 0; i < concurrentRequestCount; i++ {
		go func(workerID int) {
			defer concurrentExecutionFinishTracker.Done()

			// Block routine runtime execution until the synchronization latch drops
			synchronizationStartWaitBarrier.Wait()

			// Construct raw unique payload metrics
			reqPayload := models.ReservationRequest{
				SlotID:      "slot_101",
				PlateNumber: fmt.Sprintf("AP-07-WORKER-%d", workerID),
				VehicleType: "car",
			}
			jsonBytes, _ := json.Marshal(reqPayload)

			// Instantiate standalone virtual HTTP network cycle channels
			httpRequest := httptest.NewRequest(http.MethodPost, "/api/sessions/reserve", bytes.NewBuffer(jsonBytes))
			httpResponseRecorder := httptest.NewRecorder()

			// Invoke target router handler operation sequence directly
			webControllers.HandleReserveSlot(httpResponseRecorder, httpRequest)

			// Classify responses across atomic counters depending on standard status outputs
			switch httpResponseRecorder.Code {
			case http.StatusCreated:
				atomic.AddInt32(&successfulAllocationsCounter, 1)
			case http.StatusConflict:
				atomic.AddInt32(&rejectedConflictReservationsCounter, 1)
			default:
				atomic.AddInt32(&processingErrorsCounter, 1)
			}
		}(i)
	}

	// 6. Release the synchronization latch to launch all 50 execution routines at the same millisecond
	startTimeTrack := time.Now()
	synchronizationStartWaitBarrier.Done()
	
	// Block test completion until all workers successfully finish execution
	concurrentExecutionFinishTracker.Wait()

	t.Logf("🚀 Concurrency execution window completed processing in: %v", time.Since(startTimeTrack))
	t.Logf("📊 Metrics Log -> Success: %d │ Conflict Rejections: %d │ Processing Anomalies: %d", 
		successfulAllocationsCounter, rejectedConflictReservationsCounter, processingErrorsCounter)

	// 7. Core Assertion Guardrails: Verify absolute thread safety state balances
	if successfulAllocationsCounter != 1 {
		t.Errorf("❌ CONCURRENCY CRASH RISK FLAGGED: System permitted exactly %d allocations instead of restricting strictly to 1.", successfulAllocationsCounter)
	}
	if rejectedConflictReservationsCounter != concurrentRequestCount-1 {
		t.Errorf("❌ LOCK BYPASS RISK FLAGGED: Expected %d conflict rejections, recorded %d.", concurrentRequestCount-1, rejectedConflictReservationsCounter)
	}
	if processingErrorsCounter > 0 {
		t.Errorf("❌ UNEXPECTED SYSTEM FAILURES: Encountered %d operational engine crashes under load conditions.", processingErrorsCounter)
	}
}
