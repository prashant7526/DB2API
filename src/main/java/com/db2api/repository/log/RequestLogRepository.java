package com.db2api.repository.log;

import com.db2api.persistent.log.RequestLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository interface for {@link RequestLog} entities.
 */
@Repository
public interface RequestLogRepository extends JpaRepository<RequestLog, Long> {

    /**
     * Finds all request logs after the given timestamp.
     *
     * @param since the earliest timestamp to include
     * @return list of request logs after the given time
     */
    List<RequestLog> findByTimestampAfter(Instant since);

    /**
     * Counts request logs for a given URI prefix since a given time.
     *
     * @param uriPrefix the URI prefix to match
     * @param since     the earliest timestamp
     * @return the count of matching logs
     */
    long countByRequestUriStartingWithAndTimestampAfter(String uriPrefix, Instant since);
}
