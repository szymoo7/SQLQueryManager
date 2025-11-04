package org.task.jetbrainstask.service.implementations;

import org.springframework.stereotype.Component;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.interfaces.QueryCacheManager;

import java.util.Optional;

@Component
public class QueryCacheManagerImpl implements QueryCacheManager {
    @Override
    public Optional<QueryResult> getCachedResult(String sql) {
        return Optional.empty();
    }

    @Override
    public void putResultInCache(String sql, QueryResult result) {

    }
}
