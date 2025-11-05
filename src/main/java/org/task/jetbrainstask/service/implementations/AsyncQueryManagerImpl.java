package org.task.jetbrainstask.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.interfaces.AsyncQueryManager;
import org.task.jetbrainstask.service.interfaces.QueryExecutor;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Component
public class AsyncQueryManagerImpl implements AsyncQueryManager {

    private final QueryExecutor queryExecutor;
    private final Executor executor;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public AsyncQueryManagerImpl(QueryExecutor queryExecutor, Executor asyncExecutor) {
        this.queryExecutor = queryExecutor;
        this.executor = asyncExecutor;
    }

    @Override
    public CompletableFuture<QueryResult> executeAsync(QueryEntry entry) {
        log.info("Starting async execution for query ID={} -> {}", entry.getId(), entry.getQuery());

        return CompletableFuture.supplyAsync(() -> {
            long start = System.currentTimeMillis();
            try {
                QueryResult result = queryExecutor.executeQuery(entry.getQuery());
                result.setId(entry.getId());
                long time = System.currentTimeMillis() - start;
                log.info("Async query ID={} completed in {} ms", entry.getId(), time);
                return result;
            } catch (Exception e) {
                log.error("Error during async execution for ID={}: {}", entry.getId(), e.getMessage());
                QueryResult errorResult = new QueryResult();
                errorResult.setId(entry.getId());
                errorResult.setHeaders(List.of("error"));
                errorResult.setData(List.of(List.of("Async query failed: " + e.getMessage())));
                return errorResult;
            }
        }, executor);
    }
}
