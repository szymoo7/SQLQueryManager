package org.task.jetbrainstask.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.task.jetbrainstask.service.implementations.QueryAnalyzerImpl;
import org.task.jetbrainstask.service.interfaces.QueryAnalyzer;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QueryAnalyzer Tests")
class QueryAnalyzerTest {

    private QueryAnalyzer analyzer;

    @BeforeEach
    void setUp() {
        analyzer = new QueryAnalyzerImpl();
    }

    @Test
    @DisplayName("Should run simple SELECT synchronously")
    void shouldRunSimpleSelectSync() {
        String sql = "SELECT * FROM passengers";

        boolean result = analyzer.shouldRunAsync(sql);

        assertFalse(result, "Simple SELECT should run synchronously");
    }

    @Test
    @DisplayName("Should run SELECT with WHERE synchronously")
    void shouldRunSelectWithWhereSync() {
        String sql = "SELECT * FROM passengers WHERE Age > 30";

        boolean result = analyzer.shouldRunAsync(sql);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should run SELECT with LIMIT synchronously")
    void shouldRunSelectWithLimitSync() {
        String sql = "SELECT * FROM passengers LIMIT 10";

        boolean result = analyzer.shouldRunAsync(sql);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should run SELECT with ORDER BY synchronously")
    void shouldRunSelectWithOrderBySync() {
        String sql = "SELECT * FROM passengers ORDER BY Age";

        boolean result = analyzer.shouldRunAsync(sql);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should run SELECT with GROUP BY synchronously")
    void shouldRunSelectWithGroupBySync() {
        String sql = "SELECT Pclass, COUNT(*) FROM passengers GROUP BY Pclass";

        boolean result = analyzer.shouldRunAsync(sql);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should run aggregate function synchronously")
    void shouldRunAggregateSync() {
        String sql = "SELECT AVG(Age), MAX(Fare) FROM passengers";

        boolean result = analyzer.shouldRunAsync(sql);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should run query with JOIN asynchronously")
    void shouldRunJoinAsync() {
        String sql = "SELECT p.*, t.* FROM passengers p JOIN tickets t ON p.id = t.passenger_id";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result, "Query with JOIN should run asynchronously");
    }

    @Test
    @DisplayName("Should run query with multiple JOINs asynchronously")
    void shouldRunMultipleJoinsAsync() {
        String sql = "SELECT * FROM p1 JOIN p2 ON p1.id = p2.id JOIN p3 ON p2.id = p3.id";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should run CROSS JOIN asynchronously")
    void shouldRunCrossJoinAsync() {
        String sql = "SELECT * FROM passengers p1 CROSS JOIN passengers p2";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result, "CROSS JOIN should run asynchronously");
    }

    @Test
    @DisplayName("Should run query with subquery asynchronously")
    void shouldRunSubqueryAsync() {
        String sql = "SELECT * FROM passengers WHERE Age > (SELECT AVG(Age) FROM passengers)";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result, "Query with subquery (multiple SELECT) should run asynchronously");
    }

    @Test
    @DisplayName("Should run query with IN subquery asynchronously")
    void shouldRunInSubqueryAsync() {
        String sql = "SELECT * FROM passengers WHERE PassengerId IN (SELECT PassengerId FROM tickets)";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should run complex nested subquery asynchronously")
    void shouldRunNestedSubqueryAsync() {
        String sql = """
            SELECT * FROM passengers 
            WHERE Age > (
                SELECT AVG(Age) FROM passengers 
                WHERE Pclass IN (SELECT DISTINCT Pclass FROM passengers)
            )
        """;

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result, "Nested subqueries (3 SELECT) should run asynchronously");
    }

    @Test
    @DisplayName("Should learn from slow execution and switch to async")
    void shouldLearnFromSlowExecution() {
        String sql = "SELECT * FROM passengers";

        assertFalse(analyzer.shouldRunAsync(sql));

        analyzer.recordExecution(sql, 6000);

        assertTrue(analyzer.shouldRunAsync(sql),
                "Query should run async after being recorded as slow (>5s)");
    }

    @Test
    @DisplayName("Should stay sync for fast queries in history")
    void shouldStaySyncForFastQueries() {
        String sql = "SELECT * FROM passengers";

        analyzer.recordExecution(sql, 100);

        assertFalse(analyzer.shouldRunAsync(sql));
    }

    @Test
    @DisplayName("Should use threshold of 5000ms for async decision")
    void shouldUseCorrectThreshold() {
        String sql = "SELECT * FROM passengers";

        analyzer.recordExecution(sql, 5000);
        assertFalse(analyzer.shouldRunAsync(sql));

        analyzer.recordExecution(sql, 5001);
        assertTrue(analyzer.shouldRunAsync(sql));
    }

    @Test
    @DisplayName("Should normalize SQL with extra spaces")
    void shouldNormalizeSqlWithExtraSpaces() {
        String sql1 = "SELECT * FROM passengers";
        String sql2 = "SELECT  *  FROM  passengers";

        analyzer.recordExecution(sql1, 6000);

        assertTrue(analyzer.shouldRunAsync(sql2),
                "Normalized SQL should match history regardless of spacing");
    }

    @Test
    @DisplayName("Should normalize SQL case sensitivity")
    void shouldNormalizeSqlCase() {
        String sql1 = "SELECT * FROM passengers";
        String sql2 = "select * from passengers";
        String sql3 = "SeLeCt * FrOm passengers";

        analyzer.recordExecution(sql1, 6000);

        assertTrue(analyzer.shouldRunAsync(sql2));
        assertTrue(analyzer.shouldRunAsync(sql3));
    }

    @Test
    @DisplayName("Should normalize SQL with tabs and newlines")
    void shouldNormalizeSqlWithWhitespace() {
        String sql1 = "SELECT * FROM passengers";
        String sql2 = "SELECT\t*\nFROM\tpassengers";

        analyzer.recordExecution(sql1, 6000);

        assertTrue(analyzer.shouldRunAsync(sql2));
    }

    @Test
    @DisplayName("Should return false for null SQL")
    void shouldReturnFalseForNull() {
        boolean result = analyzer.shouldRunAsync(null);

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for empty SQL")
    void shouldReturnFalseForEmpty() {
        boolean result = analyzer.shouldRunAsync("");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should return false for blank SQL")
    void shouldReturnFalseForBlank() {
        boolean result = analyzer.shouldRunAsync("   \t  \n  ");

        assertFalse(result);
    }

    @Test
    @DisplayName("Should not record execution for null SQL")
    void shouldNotRecordExecutionForNull() {
        assertDoesNotThrow(() -> analyzer.recordExecution(null, 1000));
    }

    @Test
    @DisplayName("Should not record execution for empty SQL")
    void shouldNotRecordExecutionForEmpty() {
        assertDoesNotThrow(() -> analyzer.recordExecution("", 1000));
    }

    @Test
    @DisplayName("Should handle zero execution time")
    void shouldHandleZeroExecutionTime() {
        String sql = "SELECT 1";

        analyzer.recordExecution(sql, 0);

        assertFalse(analyzer.shouldRunAsync(sql));
    }

    @Test
    @DisplayName("Should handle negative execution time")
    void shouldHandleNegativeExecutionTime() {
        String sql = "SELECT 1";

        analyzer.recordExecution(sql, -100);

        assertFalse(analyzer.shouldRunAsync(sql));
    }

    @Test
    @DisplayName("Should not detect 'join' in column name as JOIN")
    void shouldNotDetectJoinInColumnName() {
        String sql = "SELECT joined_at FROM passengers";

        boolean result = analyzer.shouldRunAsync(sql);

        assertFalse(result, "Should not detect 'join' substring in column name");
    }

    @Test
    @DisplayName("Should detect JOIN with different casing")
    void shouldDetectJoinWithDifferentCasing() {
        String sql = "select * from p1 join p2 on p1.id = p2.id";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should detect CROSS JOIN with different casing")
    void shouldDetectCrossJoinWithDifferentCasing() {
        String sql = "select * from p1 cross join p2";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should not detect SELECT in string literal")
    void shouldNotDetectSelectInStringLiteral() {
        String sql = "SELECT * FROM passengers WHERE Name = 'SELECT something'";

        boolean result = analyzer.shouldRunAsync(sql);

        assertFalse(result, "Analyzer should ignore SELECT in string literal");
    }



    @Test
    @DisplayName("Should overwrite execution time on subsequent recordings")
    void shouldOverwriteExecutionTime() {
        String sql = "SELECT * FROM passengers";

        analyzer.recordExecution(sql, 6000);
        assertTrue(analyzer.shouldRunAsync(sql));

        analyzer.recordExecution(sql, 100);
        assertFalse(analyzer.shouldRunAsync(sql),
                "Should use latest execution time");
    }

    @Test
    @DisplayName("Should handle multiple different queries")
    void shouldHandleMultipleDifferentQueries() {
        String sql1 = "SELECT * FROM passengers";
        String sql2 = "SELECT * FROM tickets";
        String sql3 = "SELECT * FROM bookings";

        analyzer.recordExecution(sql1, 6000);
        analyzer.recordExecution(sql2, 100);
        analyzer.recordExecution(sql3, 7000);

        assertTrue(analyzer.shouldRunAsync(sql1));
        assertFalse(analyzer.shouldRunAsync(sql2));
        assertTrue(analyzer.shouldRunAsync(sql3));
    }

    @Test
    @DisplayName("Should detect async for query with JOIN and WHERE")
    void shouldDetectAsyncForJoinWithWhere() {
        String sql = "SELECT * FROM p1 JOIN p2 ON p1.id = p2.id WHERE p1.age > 30";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should detect async for LEFT JOIN")
    void shouldDetectAsyncForLeftJoin() {
        String sql = "SELECT * FROM passengers p LEFT JOIN tickets t ON p.id = t.passenger_id";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should detect async for RIGHT JOIN")
    void shouldDetectAsyncForRightJoin() {
        String sql = "SELECT * FROM passengers p RIGHT JOIN tickets t ON p.id = t.passenger_id";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should detect async for INNER JOIN")
    void shouldDetectAsyncForInnerJoin() {
        String sql = "SELECT * FROM passengers p INNER JOIN tickets t ON p.id = t.passenger_id";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should detect async for OUTER JOIN")
    void shouldDetectAsyncForOuterJoin() {
        String sql = "SELECT * FROM passengers p FULL OUTER JOIN tickets t ON p.id = t.passenger_id";

        boolean result = analyzer.shouldRunAsync(sql);

        assertTrue(result);
    }

    @Test
    @DisplayName("Should analyze query quickly")
    void shouldAnalyzeQueryQuickly() {
        String sql = "SELECT * FROM passengers WHERE Age > 30";

        long start = System.currentTimeMillis();
        analyzer.shouldRunAsync(sql);
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 10, "Analysis should take less than 10ms, took: " + duration + "ms");
    }

    @Test
    @DisplayName("Should handle large query string efficiently")
    void shouldHandleLargeQueryString() {
        StringBuilder sql = new StringBuilder("SELECT ");
        for (int i = 0; i < 1000; i++) {
            sql.append("col").append(i).append(", ");
        }
        sql.append("id FROM passengers");

        long start = System.currentTimeMillis();
        boolean result = analyzer.shouldRunAsync(sql.toString());
        long duration = System.currentTimeMillis() - start;

        assertFalse(result);
        assertTrue(duration < 100, "Should analyze large query in <100ms");
    }

    @Test
    @DisplayName("Should handle many recorded queries efficiently")
    void shouldHandleManyRecordedQueries() {
        for (int i = 0; i < 1000; i++) {
            analyzer.recordExecution("SELECT * FROM table" + i, 100);
        }

        long start = System.currentTimeMillis();
        analyzer.shouldRunAsync("SELECT * FROM passengers");
        long duration = System.currentTimeMillis() - start;

        assertTrue(duration < 10);
    }

    @Test
    @DisplayName("Should handle query at exact threshold")
    void shouldHandleQueryAtExactThreshold() {
        String sql = "SELECT * FROM passengers";

        analyzer.recordExecution(sql, 5000);

        assertFalse(analyzer.shouldRunAsync(sql),
                "5000ms is not greater than 5000ms threshold");
    }

    @Test
    @DisplayName("Should handle query just above threshold")
    void shouldHandleQueryJustAboveThreshold() {
        String sql = "SELECT * FROM passengers";

        analyzer.recordExecution(sql, 5001);

        assertTrue(analyzer.shouldRunAsync(sql));
    }

    @Test
    @DisplayName("Should handle very long execution time")
    void shouldHandleVeryLongExecutionTime() {
        String sql = "SELECT * FROM passengers";

        analyzer.recordExecution(sql, Long.MAX_VALUE);

        assertTrue(analyzer.shouldRunAsync(sql));
    }
}