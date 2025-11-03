package org.task.jetbrainstask.service.implementations;

import org.springframework.stereotype.Service;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.interfaces.QueryService;

import java.util.List;
import java.util.Map;

@Service
public class QueryServiceImpl implements QueryService {
    @Override
    public List<Map<String, Long>> addQueries(String requestBody) {
        return List.of();
    }

    @Override
    public List<QueryEntry> getQueries() {
        return List.of();
    }

    @Override
    public QueryResult executeQueryById(long id) {
        return null;
    }

    @Override
    public QueryResult getQueryExecution(long id) {
        return null;
    }
}
