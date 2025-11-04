package org.task.jetbrainstask.service.interfaces;

import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;

import java.util.concurrent.CompletableFuture;

public interface AsyncQueryManager {
    CompletableFuture<QueryResult> executeAsync(QueryEntry entry);
}
