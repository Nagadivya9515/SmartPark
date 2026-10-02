package web

import (
	"log"
	"net/http"
	"time"
)

// responseWriterInterceptor acts as a structural wrapper to track outbound status codes
type responseWriterInterceptor struct {
	http.ResponseWriter
	statusCode int
}

func (rw *responseWriterInterceptor) WriteHeader(code int) {
	rw.statusCode = code
	rw.ResponseWriter.WriteHeader(code)
}

// LoggingMiddleware logs detailed runtime performance tracking details to the console dashboard
func LoggingMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		startTime := time.Now()

		// Intercept the default response writer to catch status modifications
		interceptor := &responseWriterInterceptor{
			ResponseWriter: w,
			statusCode:     http.StatusOK, // Default status code if not modified
		}

		// Pass execution flow down the pipeline to your active controllers
		next.ServeHTTP(interceptor, r)

		// Calculate total computational runtime duration tracking values
		latencyDuration := time.Since(startTime)

		log.Printf(
			"📡 [NET-API] %s │ %s │ %d │ %s",
			r.Method,
			r.URL.Path,
			interceptor.statusCode,
			latencyDuration,
		)
	})
}
