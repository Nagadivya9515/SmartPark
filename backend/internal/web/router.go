package web

import (
	"net/http"
)

// SetupRouter configures endpoint pathways using Go's native ServeMux engine
func SetupRouter(handlers *Handlers) http.Handler {
	// 1. Initialize a new isolated native standard routing multiplexer channel
	mux := http.NewServeMux()

	// 2. Map explicit network URL endpoint entry routes to their designated controller handlers
	// Note: In Go 1.22+, you can append the HTTP method directly to the route string (e.g., "POST /api/sessions/reserve")
	mux.HandleFunc("POST /api/sessions/reserve", handlers.HandleReserveSlot)

	// 3. Chain request tracking filters directly over the multiplexer instance pipeline
	var rootRoutingPipeline http.Handler = mux
	
	// Apply structural execution logs wrapper
	rootRoutingPipeline = LoggingMiddleware(rootRoutingPipeline)

	return rootRoutingPipeline
}
