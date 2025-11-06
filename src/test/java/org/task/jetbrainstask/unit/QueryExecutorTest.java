package org.task.jetbrainstask.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.implementations.QueryExecutorImpl;
import org.task.jetbrainstask.service.interfaces.QueryExecutor;


import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Import(QueryExecutorImpl.class)
@DisplayName("QueryExecutorImpl Tests")
class QueryExecutorTest {

    @Autowired
    private QueryExecutor queryExecutor;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("""
            CREATE TABLE IF NOT EXISTS passengers (
                PassengerId INT PRIMARY KEY,
                Survived INT,
                Pclass INT,
                Name VARCHAR(255),
                Sex VARCHAR(10),
                Age DECIMAL(5,2),
                SibSp INT,
                Parch INT,
                Ticket VARCHAR(50),
                Fare DECIMAL(10,4),
                Cabin VARCHAR(50),
                Embarked VARCHAR(1)
            )
        """);

        jdbcTemplate.execute("""
            INSERT INTO passengers 
            (PassengerId, Survived, Pclass, Name, Sex, Age, SibSp, Parch, Ticket, Fare, Cabin, Embarked) 
            VALUES 
            (1, 0, 3, 'Braund, Mr. Owen Harris', 'male', 22, 1, 0, 'A/5 21171', 7.2500, null, 'S'),
            (2, 1, 1, 'Cumings, Mrs. John Bradley', 'female', 38, 1, 0, 'PC 17599', 71.2833, 'C85', 'C'),
            (3, 1, 3, 'Heikkinen, Miss. Laina', 'female', 26, 0, 0, 'STON/O2. 3101282', 7.9250, null, 'S'),
            (4, 1, 1, 'Futrelle, Mrs. Jacques Heath', 'female', 35, 1, 0, '113803', 53.1000, 'C123', 'S'),
            (5, 0, 3, 'Allen, Mr. William Henry', 'male', 35, 0, 0, '373450', 8.0500, null, 'S')
        """);
    }

    @Test
    @DisplayName("Should execute simple SELECT * query")
    void shouldExecuteSimpleSelectAll() {
        String sql = "SELECT * FROM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertNotNull(result.getHeaders());
        assertNotNull(result.getData());
        assertEquals(12, result.getHeaders().size());
        assertEquals(5, result.getData().size());
        assertNotNull(result.getExecutionTimeMs());
        assertTrue(result.getExecutionTimeMs() >= 0);
    }

    @Test
    @DisplayName("Should execute SELECT with WHERE clause")
    void shouldExecuteSelectWithWhere() {
        String sql = "SELECT * FROM passengers WHERE Survived = 1";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(3, result.getData().size());
        assertTrue(result.getHeaders().contains("NAME"));
    }

    @Test
    @DisplayName("Should execute SELECT specific columns")
    void shouldExecuteSelectSpecificColumns() {
        String sql = "SELECT PassengerId, Name, Age FROM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(3, result.getHeaders().size());
        assertTrue(result.getHeaders().contains("PASSENGERID"));
        assertTrue(result.getHeaders().contains("NAME"));
        assertTrue(result.getHeaders().contains("AGE"));
        assertEquals(5, result.getData().size());
    }

    @Test
    @DisplayName("Should execute SELECT with LIMIT")
    void shouldExecuteSelectWithLimit() {
        String sql = "SELECT * FROM passengers LIMIT 2";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("Should execute SELECT with ORDER BY")
    void shouldExecuteSelectWithOrderBy() {
        String sql = "SELECT PassengerId, Name FROM passengers ORDER BY PassengerId DESC";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(5, result.getData().size());

        Object firstId = result.getData().get(0).get(0);
        assertEquals(5, firstId);
    }

    @Test
    @DisplayName("Should execute COUNT query")
    void shouldExecuteCountQuery() {
        String sql = "SELECT COUNT(*) as total FROM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(1, result.getHeaders().size());
        assertEquals("TOTAL", result.getHeaders().get(0));
        assertEquals(1, result.getData().size());
        assertEquals(5L, result.getData().get(0).get(0));
    }

    @Test
    @DisplayName("Should execute GROUP BY query")
    void shouldExecuteGroupByQuery() {
        String sql = "SELECT Pclass, COUNT(*) as count FROM passengers GROUP BY Pclass ORDER BY Pclass";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(2, result.getHeaders().size());
        assertTrue(result.getHeaders().contains("PCLASS"));
        assertTrue(result.getHeaders().contains("COUNT"));
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("Should execute AVG aggregation")
    void shouldExecuteAvgQuery() {
        String sql = "SELECT AVG(Age) as avg_age FROM passengers WHERE Age IS NOT NULL";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(1, result.getHeaders().size());
        assertEquals("AVG_AGE", result.getHeaders().get(0));
        assertNotNull(result.getData().get(0).get(0));
    }

    @Test
    @DisplayName("Should handle NULL values correctly")
    void shouldHandleNullValues() {
        String sql = "SELECT PassengerId, Cabin FROM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(5, result.getData().size());

        assertNull(result.getData().get(0).get(1));

        assertEquals("C85", result.getData().get(1).get(1));
    }

    @Test
    @DisplayName("Should execute query returning empty result set")
    void shouldExecuteQueryReturningEmptyResult() {
        String sql = "SELECT * FROM passengers WHERE PassengerId = 999";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(0, result.getHeaders().size());
        assertEquals(0, result.getData().size());
    }

    @Test
    @DisplayName("Should execute JOIN query")
    void shouldExecuteJoinQuery() {
        String sql = """
            SELECT p1.PassengerId, p1.Name, p2.Name as RelativeName
            FROM passengers p1
            JOIN passengers p2 ON p1.SibSp = p2.SibSp AND p1.PassengerId != p2.PassengerId
            LIMIT 5
        """;

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertTrue(result.getData().size() <= 5);
    }

    @Test
    @DisplayName("Should handle invalid SQL syntax")
    void shouldHandleInvalidSqlSyntax() {
        String sql = "SELEKT * FORM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(1, result.getHeaders().size());
        assertEquals("error", result.getHeaders().get(0));
        assertEquals(1, result.getData().size());
        assertNotNull(result.getData().get(0).get(0));
        assertTrue(result.getData().get(0).get(0).toString().length() > 0);
    }

    @Test
    @DisplayName("Should handle non-existent table")
    void shouldHandleNonExistentTable() {
        String sql = "SELECT * FROM non_existent_table";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals("error", result.getHeaders().get(0));
        assertTrue(result.getData().get(0).get(0).toString().contains("NOT_FOUND") ||
                result.getData().get(0).get(0).toString().contains("NOT_FOUND"));
    }

    @Test
    @DisplayName("Should handle non-existent column")
    void shouldHandleNonExistentColumn() {
        String sql = "SELECT NonExistentColumn FROM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals("error", result.getHeaders().get(0));
    }

    @Test
    @DisplayName("Should handle division by zero")
    void shouldHandleDivisionByZero() {
        String sql = "SELECT 1/0 as result";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle query with special characters in strings")
    void shouldHandleSpecialCharacters() {
        String sql = "SELECT * FROM passengers WHERE Name LIKE '%Mr.%'";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertTrue(result.getData().size() > 0);
    }

    @Test
    @DisplayName("Should handle query with subquery")
    void shouldHandleSubquery() {
        String sql = """
            SELECT * FROM passengers 
            WHERE Age > (SELECT AVG(Age) FROM passengers WHERE Age IS NOT NULL)
        """;

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle DISTINCT query")
    void shouldHandleDistinctQuery() {
        String sql = "SELECT DISTINCT Pclass FROM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(2, result.getData().size());
    }

    @Test
    @DisplayName("Should handle CASE WHEN expression")
    void shouldHandleCaseWhen() {
        String sql = """
            SELECT PassengerId, 
                   CASE WHEN Survived = 1 THEN 'Yes' ELSE 'No' END as SurvivedText
            FROM passengers
        """;

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(2, result.getHeaders().size());
        assertEquals(5, result.getData().size());
    }

    @Test
    @DisplayName("Should handle UNION query")
    void shouldHandleUnion() {
        String sql = """
            SELECT PassengerId, Name FROM passengers WHERE Pclass = 1
            UNION
            SELECT PassengerId, Name FROM passengers WHERE Pclass = 3
        """;

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(5, result.getData().size());
    }


    @Test
    @DisplayName("Should execute query within reasonable time")
    void shouldExecuteQueryWithinReasonableTime() {
        String sql = "SELECT * FROM passengers";

        long start = System.currentTimeMillis();
        QueryResult result = queryExecutor.executeQuery(sql);
        long duration = System.currentTimeMillis() - start;

        assertNotNull(result);
        assertTrue(duration < 1000, "Query should execute in less than 1 second, took: " + duration + "ms");
        assertTrue(result.getExecutionTimeMs() < 1000);
    }

    @Test
    @DisplayName("Should track execution time correctly")
    void shouldTrackExecutionTime() {
        String sql = "SELECT * FROM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result.getExecutionTimeMs());
        assertTrue(result.getExecutionTimeMs() >= 0);
        assertTrue(result.getExecutionTimeMs() < 1000);
    }

    @Test
    @DisplayName("Should return correct data types")
    void shouldReturnCorrectDataTypes() {
        String sql = "SELECT PassengerId, Name, Age, Fare FROM passengers WHERE PassengerId = 1";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(1, result.getData().size());

        List<Object> row = result.getData().get(0);
        assertTrue(row.get(0) instanceof Integer);
        assertTrue(row.get(1) instanceof String);
    }

    @Test
    @DisplayName("Should maintain column order")
    void shouldMaintainColumnOrder() {
        String sql = "SELECT Name, PassengerId, Age FROM passengers LIMIT 1";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(3, result.getHeaders().size());
        assertEquals("NAME", result.getHeaders().get(0));
        assertEquals("PASSENGERID", result.getHeaders().get(1));
        assertEquals("AGE", result.getHeaders().get(2));
    }

    @Test
    @DisplayName("Should handle large result set")
    void shouldHandleLargeResultSet() {
        for (int i = 6; i <= 100; i++) {
            jdbcTemplate.execute(String.format(
                    "INSERT INTO passengers (PassengerId, Name, Pclass) VALUES (%d, 'Passenger %d', 3)",
                    i, i
            ));
        }

        String sql = "SELECT * FROM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(100, result.getData().size());
    }

    @Test
    @DisplayName("Should execute in read-only transaction")
    void shouldExecuteInReadOnlyTransaction() {
        String sql = "SELECT COUNT(*) as total FROM passengers";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertEquals(5L, result.getData().get(0).get(0));
    }

    @Test
    @DisplayName("Result should have correct structure")
    void resultShouldHaveCorrectStructure() {
        String sql = "SELECT PassengerId, Name FROM passengers LIMIT 1";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertNotNull(result.getHeaders());
        assertNotNull(result.getData());
        assertNotNull(result.getExecutionTimeMs());

        assertEquals(result.getHeaders().size(), result.getData().get(0).size());
    }

    @Test
    @DisplayName("Empty result should have empty headers")
    void emptyResultShouldHaveEmptyHeaders() {
        String sql = "SELECT * FROM passengers WHERE 1 = 0";

        QueryResult result = queryExecutor.executeQuery(sql);

        assertNotNull(result);
        assertTrue(result.getHeaders().isEmpty());
        assertTrue(result.getData().isEmpty());
    }
}