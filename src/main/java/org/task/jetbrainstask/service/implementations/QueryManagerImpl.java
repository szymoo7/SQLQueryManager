package org.task.jetbrainstask.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.models.QueryStatus;
import org.task.jetbrainstask.service.interfaces.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class QueryManagerImpl implements QueryManager {

    private final Map<Long, QueryEntry> queue = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<QueryResult>> executions = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    private final Logger log = LoggerFactory.getLogger(QueryManagerImpl.class);

    private final QueryAnalyzer analyzer;
    private final QueryExecutor executor;
    private final QueryCacheManager queryCacheManager;
    private final AsyncQueryManager asyncManager;

    @Autowired
    public QueryManagerImpl(QueryAnalyzer analyzer, QueryExecutor executor, QueryCacheManager queryCacheManager, AsyncQueryManager asyncManager) {
        this.analyzer = analyzer;
        this.executor = executor;
        this.queryCacheManager = queryCacheManager;
        this.asyncManager = asyncManager;
    }

    @Override
    public List<Long> addQueries(List<QueryEntry> queries) {
        List<Long> ids = new ArrayList<>();
        queries.forEach(q -> {
            Long id = idGenerator.getAndIncrement();
            queue.put(id, q);
            ids.add(id);
            q.setId(id);
            q.setStatus(QueryStatus.READY);
            log.info("Added query ID={} to queue: {}", id, q.getQuery());
        });

        log.info("Total {} queries added to queue", ids.size());
        return ids;
    }

    @Override
    public List<QueryEntry> getQueries() {
        List<QueryEntry> allQueries = new ArrayList<>(queue.values());
        log.debug("Retrieved {} queries from queue", allQueries.size());
        return allQueries;
    }

    @Override
    public CompletableFuture<QueryResult> executeQueryById(long id) {
        QueryEntry queryEntry = queue.get(id);
        if (queryEntry == null) {
            log.warn("Attempt to access non-existent query id={}", id);
            return CompletableFuture.completedFuture(
                    QueryResult.error("Query not found for id=" + id)
            );
        }

        String sql = queryEntry.getQuery();
        log.info("Executing query id={} (async check pending)", id);

        try {
            Optional<QueryResult> cached = queryCacheManager.getCachedResult(sql);
            if (cached.isPresent()) {
                log.debug("Cache hit for query id={} sql={}", id, sql);
                queryEntry.setStatus(QueryStatus.COMPLETED);
                return CompletableFuture.completedFuture(cached.get());
            }

            boolean async = analyzer.shouldRunAsync(sql);
            log.info("Query id={} determined to run {}", id, async ? "asynchronously" : "synchronously");

            queryEntry.setStatus(QueryStatus.RUNNING);
            if (async) {
                log.info("Submitting async query id={}", id);

                CompletableFuture<QueryResult> future = asyncManager.executeAsync(queryEntry)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                queryEntry.setStatus(QueryStatus.FAILED);
                                queryEntry.setErrorMessage(ex.getMessage());
                                log.error("Async query id={} failed: {}", id, ex.getMessage(), ex);
                            } else {
                                queryEntry.setStatus(QueryStatus.TO_BE_SEEN);
                                log.info("Async query id={} completed successfully. Result will be available at /execute/{}", id, id);
                            }
                        });

                executions.put(id, future);
                return CompletableFuture.completedFuture(createRunningPlaceholder(id));
            }

            QueryResult result = executor.executeQuery(sql);
            result.setId(queryEntry.getId());
            queryCacheManager.putResultInCache(sql, result);
            queryEntry.setStatus(QueryStatus.COMPLETED);
            analyzer.recordExecution(sql, result.getExecutionTimeMs());
            log.info("Synchronous query id={} completed successfully", id);

            return CompletableFuture.completedFuture(result);

        } catch (Exception ex) {
            log.error("Error executing query id={} sql={} : {}", id, sql, ex.getMessage(), ex);
            queryEntry.setStatus(QueryStatus.FAILED);
            queryEntry.setErrorMessage(ex.getMessage());

            QueryResult errorResult = QueryResult.error(
                    "Error executing query id=" + id + ": " + ex.getMessage()
            );
            errorResult.setId(id);
            return CompletableFuture.completedFuture(errorResult);
        }
    }

    @Override
    public QueryResult getQueryExecution(long id) {
        CompletableFuture<QueryResult> future = executions.get(id);
        if (future == null) {
            log.warn("Async query ID={} not found", id);
            return QueryResult.error("Async query not found for id=" + id);
        }

        if (!future.isDone()) {
            log.info("Async query ID={} is still running", id);
            return getQueryResult(id);
        }

        try {
            QueryResult result = future.get();
            QueryEntry entry = queue.get(id);
            if (entry != null && entry.getStatus() == QueryStatus.TO_BE_SEEN) {
                entry.setStatus(QueryStatus.COMPLETED);
                result.setStatus(QueryStatus.COMPLETED);
                log.info("Async query ID={} status changed TO_BE_SEEN -> COMPLETED", id);
            }
            log.info("Async query ID={} result retrieved", id);
            return result;
        } catch (Exception e) {
            log.error("Error retrieving result for async query ID={}: {}", id, e.getMessage(), e);
            QueryResult errorResult = QueryResult.error("Error retrieving async result for id=" + id + ": " + e.getMessage());
            errorResult.setId(id);
            errorResult.setStatus(QueryStatus.FAILED);
            return errorResult;
        }
    }

    private QueryResult createRunningPlaceholder(long id) {
        return getQueryResult(id);
    }

    private QueryResult getQueryResult(long id) {
        QueryResult placeholder = new QueryResult();
        placeholder.setId(id);
        placeholder.setHeaders(List.of("status", "message"));
        placeholder.setData(List.of(List.of(
                "RUNNING",
                "Query is running asynchronously. Result will be available at /execute/" + id
        )));
        placeholder.setStatus(QueryStatus.RUNNING);
        return placeholder;
    }
}
