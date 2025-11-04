package org.task.jetbrainstask.service.interfaces;

import org.task.jetbrainstask.models.QueryResult;

import java.util.Optional;

public interface QueryCacheManager {
    Optional<QueryResult> getCachedResult(String sql);
    void putResultInCache(String sql, QueryResult result);
}
