package org.task.jetbrainstask.service.interfaces;

import org.task.jetbrainstask.models.QueryResult;

public interface QueryExecutor {
    QueryResult executeQuery(String sql);
}
