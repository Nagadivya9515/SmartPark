package main

import (
	"context"
	"log"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"smartpark-backend/internal/cache"
	"smartpark-backend/internal/config"
	"smartpark-backend/internal/database"
	"smartpark-backend/internal/web"
)

func main() {
	log.Println("🚀 Initializing SmartPark High-Concurrency Backend Engine...")

	// 1. Parse environmental configurations parameters
	appConfig := config.LoadConfig()

	// 2. Instantiate persistent relational storage connection pool (MySQL)
	dbConnection, err := database.NewMySQLConnection(appConfig.MySQLDSN)
	if err != nil {
		log.Fatalf("❌ Critical infrastructure failure: MySQL initialization failed: %v", err)
	}
	defer func() {
		if err := dbConnection.Close(); err != nil {
			log.Printf("[⚠️ WARN] Error closing MySQL database connections pool: %v", err)
		}
	}()

	// 3. Initialize high-speed distributed cache network clusters manager (Redis)
	cacheClient, err := cache.NewRedisClient(appConfig.RedisAddr)
	if err != nil {
		log.Fatalf("❌ Critical infrastructure failure: Redis connection failed: %v", err)
	}

	// 4. Instantiate our structural data repository access layout layer
	dataRepository := database.NewRepository(dbConnection.Pool)

	// 5. Wire up structural dependencies to our active web routing layers
	webControllers := web.NewHandlers(dataRepository, cacheClient)
	appRouter := web.SetupRouter(webControllers)

	// 6. Instantiate the native HTTP Server configuration profile
	httpServer := &http.Server{
		Addr:         appConfig.ServerPort,
		Handler:      appRouter,
		ReadTimeout:  10 * time.Second,
		WriteTimeout: 10 * time.Second,
		IdleTimeout:  120 * time.Second,
	}

	// 7. Setup clean shutdown listeners to intercept terminal close interruptions
	shutdownSignalChannel := make(chan os.Signal, 1)
	signal.Notify(shutdownSignalChannel, os.Interrupt, syscall.SIGTERM)

	// Run the network server thread in an independent execution block (Goroutine)
	go func() {
		log.Printf("📡 Standard HTTP Web Server listening natively on port %s...", appConfig.ServerPort)
		if err := httpServer.ListenAndServe(); err != nil && err != http.ErrServerClosed {
			log.Fatalf("❌ Catastrophic system breakdown: Port binding failed: %v", err)
		}
	}()

	// Block main execution thread until a shutdown termination signal is caught
	<-shutdownSignalChannel
	log.Println("🛑 Termination signal received. Initializing clean platform shutdown...")

	// Create a hard 5-second context timeout to flush remaining network tasks safely
	shutdownContext, cancelShutdown := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancelShutdown()

	if err := httpServer.Shutdown(shutdownContext); err != nil {
		log.Fatalf("❌ Forced server shutdown triggered due to resource leaks: %v", err)
	}

	log.Println("✨ SmartPark Backend Engine successfully halted. All resources safely released.")
}
