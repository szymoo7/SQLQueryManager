package org.task.jetbrainstask.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.implementations.QueryCacheManagerImpl;
import org.task.jetbrainstask.service.interfaces.QueryCacheManager;


import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("QueryCacheManager Tests")
class QueryCacheManagerTest {

    private QueryCacheManager cacheManager;
    private CacheManager springCacheManager;
    private Cache cache;

    @BeforeEach
    void setUp() {
        springCacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        when(springCacheManager.getCache("queryResults")).thenReturn(cache);
        cacheManager = new QueryCacheManagerImpl(springCacheManager);
    }

    @Test
    @DisplayName("Should return empty when cache not found")
    void shouldReturnEmptyWhenCacheMissing() {
        when(springCacheManager.getCache("queryResults")).thenReturn(null);
        Optional<QueryResult> result = cacheManager.getCachedResult("SELECT * FROM test");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return empty when cache miss")
    void shouldReturnEmptyOnCacheMiss() {
        when(cache.get(anyString())).thenReturn(null);
        Optional<QueryResult> result = cacheManager.getCachedResult("SELECT * FROM passengers");
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Should return cached result when cache hit")
    void shouldReturnCachedResultOnHit() {
        QueryResult expected = new QueryResult();
        Cache.ValueWrapper wrapper = mock(Cache.ValueWrapper.class);

        when(wrapper.get()).thenReturn(expected);
        when(cache.get(anyString())).thenReturn(wrapper);

        Optional<QueryResult> result = cacheManager.getCachedResult("SELECT * FROM users");
        assertTrue(result.isPresent());
        assertEquals(expected, result.get());
    }

    @Test
    @DisplayName("Should store result in cache when available")
    void shouldPutResultInCache() {
        QueryResult result = new QueryResult();
        cacheManager.putResultInCache("SELECT * FROM data", result);
        verify(cache, times(1)).put(anyString(), eq(result));
    }

    @Test
    @DisplayName("Should log warning when cache not found during put")
    void shouldWarnWhenCacheNotFoundOnPut() {
        when(springCacheManager.getCache("queryResults")).thenReturn(null);
        QueryResult result = new QueryResult();
        cacheManager.putResultInCache("SELECT * FROM data", result);
        verify(cache, never()).put(anyString(), any());
    }
}