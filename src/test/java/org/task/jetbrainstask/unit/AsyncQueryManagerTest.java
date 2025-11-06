package org.task.jetbrainstask.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.implementations.AsyncQueryManagerImpl;
import org.task.jetbrainstask.service.interfaces.QueryExecutor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Unit Tests for AsyncQueryManagerImpl")
class AsyncQueryManagerTest {

    private AsyncQueryManagerImpl asyncManager;
    private QueryExecutor queryExecutor;
    private Executor executor;

    @BeforeEach
    void setUp() {
        queryExecutor = mock(QueryExecutor.class);
        executor = Runnable::run;
        asyncManager = new AsyncQueryManagerImpl(queryExecutor, executor);
    }

    @Test
    @DisplayName("executeAsync should return result and leave QueryEntry status unchanged")
    void executeAsync_shouldReturnResult() throws Exception {
        QueryEntry entry = new QueryEntry();
        entry.setId(1L);
        entry.setQuery("SELECT 1");

        QueryResult expectedResult = new QueryResult();
        expectedResult.setId(1L);
        when(queryExecutor.executeQuery("SELECT 1")).thenReturn(expectedResult);

        CompletableFuture<QueryResult> future = asyncManager.executeAsync(entry);
        QueryResult result = future.get();

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("SELECT 1", entry.getQuery());
    }

    @Test
    @DisplayName("executeAsync should return error result when QueryExecutor throws exception")
    void executeAsync_shouldReturnErrorResultOnException() throws Exception {
        QueryEntry entry = new QueryEntry();
        entry.setId(2L);
        entry.setQuery("SELECT FAIL");

        when(queryExecutor.executeQuery("SELECT FAIL")).thenThrow(new RuntimeException("DB error"));

        CompletableFuture<QueryResult> future = asyncManager.executeAsync(entry);
        QueryResult result = future.get();

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertNotNull(result.getData());
        assertTrue(result.getData().get(0).get(0).toString().contains("Async query failed"));
    }
}
