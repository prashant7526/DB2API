package com.db2api.persistent.log;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/**
 * Entity representing a persisted HTTP request log entry.
 * Used for monitoring and analytics in the admin dashboard.
 */
@Entity
@Table(name = "request_log")
@Getter
@Setter
public class RequestLog {

    /**
     * Primary key ID for the request log entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * HTTP method of the request (GET, POST, PUT, DELETE).
     */
    @Column(name = "http_method", nullable = false, length = 10)
    private String httpMethod;

    /**
     * The URI path of the request.
     */
    @Column(name = "request_uri", nullable = false)
    private String requestUri;

    /**
     * The HTTP response status code.
     */
    @Column(name = "status_code")
    private Integer statusCode;

    /**
     * The duration of the request processing in milliseconds.
     */
    @Column(name = "duration_ms")
    private Long durationMs;

    /**
     * The client IP address.
     */
    @Column(name = "client_ip", length = 45)
    private String clientIp;

    /**
     * Timestamp when the request was received.
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;
}
