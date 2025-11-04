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
    private final QueryCacheManager queryCacheManagerImpl;
    private final AsyncQueryManager asyncManager;

    @Autowired
    public QueryManagerImpl(QueryAnalyzerImpl analyzer, QueryExecutorImpl executor, QueryCacheManagerImpl queryCacheManagerImpl, AsyncQueryManagerImpl asyncManager) {
        this.analyzer = analyzer;
        this.executor = executor;
        this.queryCacheManagerImpl = queryCacheManagerImpl;
        this.asyncManager = asyncManager;
    }

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
        return null;
    }

    @Override
    public QueryResult getQueryExecution(long id) {
        return null;
    }
}
