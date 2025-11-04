package org.task.jetbrainstask.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.interfaces.QueryCacheManager;

import java.util.Optional;

@Component
public class QueryCacheManagerImpl implements QueryCacheManager {

    private final CacheManager springCacheManager;
    private static final Logger log = LoggerFactory.getLogger(QueryCacheManagerImpl.class);

    @Autowired
    public QueryCacheManagerImpl(CacheManager springCacheManager) {
        this.springCacheManager = springCacheManager;
    }

    public Optional<QueryResult> getCachedResult(String sql) {
        String key = generateHash(sql);
        Cache cache = springCacheManager.getCache("queryResults");

        if (cache == null) {
            log.warn("Cache 'queryResults' not found.");
            return Optional.empty();
        }

        Cache.ValueWrapper wrapper = cache.get(key);
        if (wrapper != null) {
            log.debug("Cache hit for SQL: {}", sql);
            return Optional.ofNullable((QueryResult) wrapper.get());
        } else {
            log.debug("Cache miss for SQL: {}", sql);
        }

        return Optional.empty();
    }

    public void putResultInCache(String sql, QueryResult result) {
        String key = generateHash(sql);
        Cache cache = springCacheManager.getCache("queryResults");

        if (cache != null) {
            cache.put(key, result);
            log.debug("Stored result in cache for SQL: {}", sql);
        } else {
            log.warn("Failed to store result — cache 'queryResults' not found.");
        }
    }

    private String generateHash(String sql) {
        String normalized = normalizeSQL(sql);
        return String.valueOf(normalized.hashCode());
    }

    private String normalizeSQL(String sql) {
        return sql.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase();
    }
}
