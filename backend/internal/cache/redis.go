package cache

import (
	"context"
	"errors"
	"fmt"
	"time"

	"://github.com"
)

// Predefined errors specific to cache operations
var (
	ErrLockAcquisitionFailed = errors.New("could not acquire distributed lock, resource busy")
)

// RedisClient wraps the underlying Redis driver instance
type RedisClient struct {
	Client *redis.Client
}

// NewRedisClient initializes a new Redis driver connection pool
func NewRedisClient(addr string) (*RedisClient, error) {
	client := redis.NewClient(&redis.Options{
		Addr: addr,
	})

	// Instantly ping the database to verify the connection is alive
	ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
	defer cancel()

	if err := client.Ping(ctx).Err(); err != nil {
		return nil, fmt.Errorf("failed to reach Redis host: %w", err)
	}

	return &RedisClient{Client: client}, nil
}

// AcquireSlotLock handles the SetNX distributed mutex logic to isolate a parking spot mutation
func (r *RedisClient) AcquireSlotLock(ctx context.Context, slotID string, token string, expiration time.Duration) (bool, error) {
	lockKey := fmt.Sprintf("lock:slot:%s", slotID)
	
	// SetNX (Set if Not Exists) acts as an atomic checker and lock placer
	acquired, err := r.Client.SetNX(ctx, lockKey, token, expiration).Result()
	if err != nil {
		return false, fmt.Errorf("redis operation failed during lock acquisition: %w", err)
	}
	
	return acquired, nil
}

// ReleaseSlotLock evaluates lock ownership via a Lua script and releases it atomically
func (r *RedisClient) ReleaseSlotLock(ctx context.Context, slotID string, token string) error {
	lockKey := fmt.Sprintf("lock:slot:%s", slotID)

	// Lua script ensures safety: only delete the lock if the value matches the current token
	var luaReleaseScript = `
		if redis.call("get", KEYS[1]) == ARGV[1] then
			return redis.call("del", KEYS[1])
		else
			return 0
		end`

	_, err := r.Client.Eval(ctx, luaReleaseScript, []string{lockKey}, token).Result()
	if err != nil {
		return fmt.Errorf("failed executing atomic unlock script: %w", err)
	}

	return nil
}

// UpdateLiveOccupancyCounters handles state changes atomically inside a Redis transaction pipeline
func (r *RedisClient) UpdateLiveOccupancyCounters(ctx context.Context, slotID string, status string, vehicleType string) error {
	statusKey := fmt.Sprintf("status:slot:%s", slotID)
	counterKey := fmt.Sprintf("counter:free:%s", vehicleType)

	pipe := r.Client.TxPipeline()

	// Update the slot state string tracking record
	pipe.Set(ctx, statusKey, status, 0)

	// Adjust total category counts depending on the state transition direction
	if status == "occupied" {
		pipe.Decr(ctx, counterKey)
	} else if status == "free" {
		pipe.Incr(ctx, counterKey)
	}

	_, err := pipe.Exec(ctx)
	if err != nil {
		return fmt.Errorf("failed executing cache status pipeline transaction: %w", err)
	}

	return nil
}
