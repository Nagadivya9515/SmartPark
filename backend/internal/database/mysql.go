package database

import (
	"database/sql"
	"fmt"
	"log"
	"time"

	// Registers the MySQL driver implementation globally inside the standard library
	_ "://github.com"
)

// DBWrapper encapsulates the primary SQL database connection pool handle
type DBWrapper struct {
	Pool *sql.DB
}

// NewMySQLConnection initializes an isolated long-lived SQL pool manager
func NewMySQLConnection(dsn string) (*DBWrapper, error) {
	// 1. Instantiates the client configuration blueprint (Does not establish networks yet)
	db, err := sql.Open("mysql", dsn)
	if err != nil {
		return nil, fmt.Errorf("malformed connection configuration dsn: %w", err)
	}

	// 2. Configure standard scaling resource limits matching our architecture requirements
	db.SetMaxOpenConns(25)                 // Sets a ceiling on concurrent active connections allowed
	db.SetMaxIdleConns(25)                 // Retains warm connection threads ready in memory
	db.SetConnMaxLifetime(5 * time.Minute) // Periodically cycles threads out to clear stale connections

	// 3. Fire an explicit ping instruction across the network to verify credentials
	if err := db.Ping(); err != nil {
		db.Close() // Clear resources if initialization failed
		return nil, fmt.Errorf("failed to complete network handshake with MySQL: %w", err)
	}

	log.Println("📊 Persistent MySQL database connection pool established.")
	return &DBWrapper{Pool: db}, nil
}

// Close terminates all active idle data threads within the connection pool safely
func (d *DBWrapper) Close() error {
	if d.Pool != nil {
		return d.Pool.Close()
	}
	return nil
}
