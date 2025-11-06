package org.task.jetbrainstask.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.models.QueryStatus;
import org.task.jetbrainstask.service.implementations.QueryManagerImpl;
import org.task.jetbrainstask.service.interfaces.AsyncQueryManager;
import org.task.jetbrainstask.service.interfaces.QueryAnalyzer;
import org.task.jetbrainstask.service.interfaces.QueryCacheManager;
import org.task.jetbrainstask.service.interfaces.QueryExecutor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class QueryManagerTest {

    private QueryAnalyzer analyzer;
    private QueryExecutor executor;
    private QueryCacheManager cacheManager;
    private AsyncQueryManager asyncManager;
    private QueryManagerImpl queryManager;

    @BeforeEach
    void setup() {
        analyzer = mock(QueryAnalyzer.class);
        executor = mock(QueryExecutor.class);
        cacheManager = mock(QueryCacheManager.class);
        asyncManager = mock(AsyncQueryManager.class);

        queryManager = new QueryManagerImpl(analyzer, executor, cacheManager, asyncManager);
    }

    @Test
    void testAddQueries() {
        QueryEntry query = new QueryEntry();
        query.setQuery("SELECT 1");

        List<Long> ids = queryManager.addQueries(List.of(query));
        assertEquals(1, ids.size());
        assertEquals(QueryStatus.READY, query.getStatus());
        assertNotNull(query.getId());
    }

    @Test
    void testExecuteQueryById_Synchronous() throws Exception {
        QueryEntry query = new QueryEntry();
        query.setQuery("SELECT 1");
        List<Long> ids = queryManager.addQueries(List.of(query));

        when(cacheManager.getCachedResult("SELECT 1")).thenReturn(Optional.empty());
        when(analyzer.shouldRunAsync("SELECT 1")).thenReturn(false);

        QueryResult expectedResult = new QueryResult();
        expectedResult.setId(ids.get(0));
        expectedResult.setExecutionTimeMs(5L);
        when(executor.executeQuery("SELECT 1")).thenReturn(expectedResult);

        CompletableFuture<QueryResult> future = queryManager.executeQueryById(ids.get(0));
        QueryResult result = future.get();

        assertEquals(QueryStatus.COMPLETED, query.getStatus());
        assertEquals(ids.get(0), result.getId());
        verify(cacheManager).putResultInCache("SELECT 1", result);
        verify(analyzer).recordExecution("SELECT 1", 5L);
    }

    @Test
    void testExecuteQueryById_Asynchronous() throws Exception {
        QueryEntry query = new QueryEntry();
        query.setQuery("SELECT 2");
        List<Long> ids = queryManager.addQueries(List.of(query));

        when(cacheManager.getCachedResult("SELECT 2")).thenReturn(Optional.empty());
        when(analyzer.shouldRunAsync("SELECT 2")).thenReturn(true);

        QueryResult asyncResult = new QueryResult();
        asyncResult.setId(ids.get(0));
        asyncResult.setStatus(QueryStatus.COMPLETED);
        CompletableFuture<QueryResult> asyncFuture = CompletableFuture.completedFuture(asyncResult);

        when(asyncManager.executeAsync(query)).thenReturn(asyncFuture);

        CompletableFuture<QueryResult> future = queryManager.executeQueryById(ids.get(0));
        QueryResult placeholder = future.get();

        assertEquals(QueryStatus.RUNNING, placeholder.getStatus());

        QueryResult finalResult = queryManager.getQueryExecution(ids.get(0));
        assertEquals(QueryStatus.COMPLETED, finalResult.getStatus());
    }

    @Test
    void testExecuteQueryById_FromCache() throws Exception {
        QueryEntry query = new QueryEntry();
        query.setQuery("SELECT 3");
        List<Long> ids = queryManager.addQueries(List.of(query));

        QueryResult cachedResult = new QueryResult();
        cachedResult.setId(ids.get(0));
        when(cacheManager.getCachedResult("SELECT 3")).thenReturn(Optional.of(cachedResult));

        CompletableFuture<QueryResult> future = queryManager.executeQueryById(ids.get(0));
        QueryResult result = future.get();

        assertEquals(QueryStatus.COMPLETED, query.getStatus());
        assertEquals(cachedResult, result);
        verifyNoInteractions(executor);
    }

    @Test
    void testGetQueryExecution_NotFound() {
        QueryResult result = queryManager.getQueryExecution(999L);
        assertNotNull(result);
        assertEquals(QueryStatus.FAILED, result.getStatus());
        assertEquals("Async query not found for id=999", result.getErrorMessage());
    }

    @Test
    void testCleanupMethods() {
        QueryEntry query = new QueryEntry();
        query.setQuery("SELECT 4");
        List<Long> ids = queryManager.addQueries(List.of(query));

        query.setStatus(QueryStatus.COMPLETED);

        queryManager.cleanCompletedQueries();

        assertTrue(queryManager.getQueries().isEmpty());
    }
}