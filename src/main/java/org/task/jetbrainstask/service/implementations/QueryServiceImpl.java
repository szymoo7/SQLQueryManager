package org.task.jetbrainstask.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.interfaces.QueryManager;
import org.task.jetbrainstask.service.interfaces.QueryService;
import org.task.jetbrainstask.service.interfaces.QueryValidator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;

@Service
public class QueryServiceImpl implements QueryService {

    private final QueryManager queryManager;
    private final QueryValidator queryValidator;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public QueryServiceImpl(QueryManager queryManager, QueryValidator queryValidator) {
        this.queryManager = queryManager;
        this.queryValidator = queryValidator;
    }

    @Override
    public List<Map<String, Long>> addQueries(String requestBody) {
        log.info("Received new query batch request");

        List<QueryEntry> validatedQueries = queryValidator.parseAndValidate(requestBody);

        if (validatedQueries.isEmpty()) {
            log.warn("No valid queries found in request");
            return List.of(Map.of("error", -1L));
        }

        List<Long> ids = queryManager.addQueries(validatedQueries);
        log.info("Added {} queries to execution queue", ids.size());

        return ids.stream()
                .map(id -> Map.of("id", id))
                .toList();
    }

    @Override
    public List<QueryEntry> getQueries() {
        log.debug("Fetching all queries from QueryManager");

        List<QueryEntry> queries = queryManager.getQueries();

        if (queries.isEmpty()) {
            log.info("No queries currently in the queue");
        } else {
            log.info("Retrieved {} queries from queue", queries.size());
        }

        return queries;
    }

    @Override
    public QueryResult executeQueryById(long id) {
        log.info("Executing query with ID={}", id);

        try {
            QueryResult result = queryManager.executeQueryById(id).join();

            if (result == null) {
                log.warn("Query result is null for ID={}", id);
            } else {
                log.debug("Query ID={} executed successfully", id);
            }

            return result;

        } catch (CompletionException ex) {
            log.error("Asynchronous query execution failed for ID={} with message: {}", id, ex.getMessage());
            return buildErrorResult("Query execution failed: " + ex.getMessage());
        } catch (Exception ex) {
            log.error("Unexpected error while executing query ID={}: {}", id, ex.getMessage());
            return buildErrorResult("Unexpected error: " + ex.getMessage());
        }
    }

    @Override
    public QueryResult getQueryExecution(long id) {
        return queryManager.getQueryExecution(id);
    }

    private QueryResult buildErrorResult(String message) {
        return QueryResult.error(message);
    }
}
