package config

import (
	"os"
)

// Config holds all environmental connection credentials for the system
type Config struct {
	ServerPort  string
	MySQLDSN    string
	RedisAddr   string
}

// LoadConfig reads values from the environment or assigns default fallbacks
func LoadConfig() *Config {
	return &Config{
		// Default to local port 8080 if not set by environment
		ServerPort: getEnv("SERVER_PORT", ":8080"),

		// Standard DSN string format for the go-sql-driver/mysql library
		// Pattern: username:password@tcp(host:port)/dbname?parseTime=true
		MySQLDSN: getEnv("MYSQL_DSN", "admin:secret_password@tcp(127.0.0.1:5432)/parking_lot_management?parseTime=true"),

		// Standard location interface address for the Redis driver link
		RedisAddr: getEnv("REDIS_ADDR", "127.0.0.1:6379"),
	}
}

// getEnv checks if a key exists in the environment, otherwise returns the fallback value
func getEnv(key, fallback string) string {
	if value, exists := os.LookupEnv(key); exists {
		return value
	}
	return fallback
}
