package org.task.jetbrainstask.service.interfaces;

import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface QueryManager {
    List<Long> addQueries(List<QueryEntry> queries);
    List<QueryEntry> getQueries();
    CompletableFuture<QueryResult> executeQueryById(long id);
    QueryResult getQueryExecution(long id);
}
