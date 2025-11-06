package org.task.jetbrainstask.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.implementations.QueryServiceImpl;
import org.task.jetbrainstask.service.interfaces.QueryManager;
import org.task.jetbrainstask.service.interfaces.QueryService;
import org.task.jetbrainstask.service.interfaces.QueryValidator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("QueryService Tests")
class QueryServiceTest {

    private QueryService service;
    private QueryManager queryManager;
    private QueryValidator queryValidator;

    @BeforeEach
    void setUp() {
        queryManager = mock(QueryManager.class);
        queryValidator = mock(QueryValidator.class);
        service = new QueryServiceImpl(queryManager, queryValidator);
    }

    @Test
    @DisplayName("Should add valid queries and return ids")
    void shouldAddValidQueries() {
        QueryEntry q1 = new QueryEntry();
        when(queryValidator.parseAndValidate(anyString())).thenReturn(List.of(q1));
        when(queryManager.addQueries(anyList())).thenReturn(List.of(1L, 2L));

        List<Map<String, Long>> result = service.addQueries("SELECT * FROM test");

        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).get("id"));
    }

    @Test
    @DisplayName("Should return error when no valid queries found")
    void shouldReturnErrorWhenNoValidQueries() {
        when(queryValidator.parseAndValidate(anyString())).thenReturn(List.of());
        List<Map<String, Long>> result = service.addQueries("INVALID");
        assertEquals(1, result.size());
        assertEquals(-1L, result.get(0).get("error"));
    }

    @Test
    @DisplayName("Should return list of queries")
    void shouldReturnListOfQueries() {
        QueryEntry q = new QueryEntry();
        when(queryManager.getQueries()).thenReturn(List.of(q));
        List<QueryEntry> result = service.getQueries();
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should execute query and return result")
    void shouldExecuteQueryAndReturnResult() {
        QueryResult qr = new QueryResult();
        when(queryManager.executeQueryById(1L)).thenReturn(CompletableFuture.completedFuture(qr));
        QueryResult result = service.executeQueryById(1L);
        assertEquals(qr, result);
    }

    @Test
    @DisplayName("Should handle CompletionException and return error result")
    void shouldHandleCompletionException() {
        when(queryManager.executeQueryById(2L))
                .thenThrow(new CompletionException("failed", new RuntimeException()));
        QueryResult result = service.executeQueryById(2L);
        assertEquals("error", result.getHeaders().get(0));
    }

    @Test
    @DisplayName("Should handle generic exception and return error result")
    void shouldHandleGenericException() {
        when(queryManager.executeQueryById(3L)).thenThrow(new RuntimeException("boom"));
        QueryResult result = service.executeQueryById(3L);
        assertEquals("error", result.getHeaders().get(0));
    }

    @Test
    @DisplayName("Should get query execution by id")
    void shouldGetQueryExecution() {
        QueryResult qr = new QueryResult();
        when(queryManager.getQueryExecution(5L)).thenReturn(qr);
        QueryResult result = service.getQueryExecution(5L);
        assertEquals(qr, result);
    }
}
