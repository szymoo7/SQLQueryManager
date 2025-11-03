package org.task.jetbrainstask.service.interfaces;

import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;

import java.util.List;
import java.util.Map;

public interface QueryService {
    List<Map<String, Long>> addQueries(String requestBody);
    List<QueryEntry> getQueries();
    QueryResult executeQueryById(long id);
    QueryResult getQueryExecution(long id);
}
