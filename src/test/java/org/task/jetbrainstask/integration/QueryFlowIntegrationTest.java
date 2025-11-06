package org.task.jetbrainstask.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.models.QueryStatus;
import org.task.jetbrainstask.service.interfaces.QueryService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Integration Tests for Full Query Flow")
class QueryFlowIntegrationTest {

    @Autowired
    private QueryService queryService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_users");
        jdbcTemplate.execute("CREATE TABLE test_users (id INT PRIMARY KEY, name VARCHAR(255), age INT)");
        jdbcTemplate.execute("INSERT INTO test_users VALUES (1, 'Alice', 30)");
        jdbcTemplate.execute("INSERT INTO test_users VALUES (2, 'Bob', 25)");
        jdbcTemplate.execute("INSERT INTO test_users VALUES (3, 'Charlie', 35)");
    }

    @Test
    @DisplayName("should add single query and return valid ID")
    void shouldAddSingleQueryAndReturnValidId() {
        String requestBody = "SELECT * FROM test_users WHERE id = 1";

        List<Map<String, Long>> response = queryService.addQueries(requestBody);

        assertThat(response).hasSize(1);
        assertThat(response.get(0)).containsKey("id");
        assertThat(response.get(0).get("id")).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("should add multiple queries separated by semicolon")
    void shouldAddMultipleQueriesSeparatedBySemicolon() {
        String requestBody = "SELECT * FROM test_users WHERE id = 1; SELECT name FROM test_users WHERE age > 25";

        List<Map<String, Long>> response = queryService.addQueries(requestBody);

        assertThat(response).hasSize(2);
        assertThat(response.get(0)).containsKey("id");
        assertThat(response.get(1)).containsKey("id");
    }

    @Test
    @DisplayName("should reject invalid queries containing DROP")
    void shouldRejectInvalidQueriesContainingDrop() {
        String requestBody = "DROP TABLE test_users; SELECT * FROM test_users";

        List<Map<String, Long>> response = queryService.addQueries(requestBody);

        assertThat(response).hasSize(1);
        assertThat(response.get(0)).containsKey("id");
    }

    @Test
    @DisplayName("should reject queries with DELETE, UPDATE, INSERT")
    void shouldRejectQueriesWithDmlOperations() {
        String requestBody = "DELETE FROM test_users; UPDATE test_users SET name = 'Test'; INSERT INTO test_users VALUES (4, 'Dave', 40)";

        List<Map<String, Long>> response = queryService.addQueries(requestBody);

        assertThat(response).hasSize(1);
        assertThat(response.get(0)).containsKey("error");
    }

    @Test
    @DisplayName("should list all added queries")
    void shouldListAllAddedQueries() {
        String requestBody = "SELECT * FROM test_users; SELECT name FROM test_users";

        queryService.addQueries(requestBody);
        List<QueryEntry> queries = queryService.getQueries();

        assertThat(queries).hasSizeGreaterThanOrEqualTo(2);
        assertThat(queries).allMatch(q -> q.getStatus() == QueryStatus.READY || q.getStatus() == QueryStatus.COMPLETED);
    }

    @Test
    @DisplayName("should execute simple query synchronously and return results")
    void shouldExecuteSimpleQuerySynchronouslyAndReturnResults() {
        String requestBody = "SELECT * FROM test_users WHERE id = 1";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        assertThat(result.getHeaders()).containsExactlyInAnyOrder("ID", "NAME", "AGE");
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0)).containsExactly(1, "Alice", 30);
    }

    @Test
    @DisplayName("should execute query with all rows and return complete dataset")
    void shouldExecuteQueryWithAllRowsAndReturnCompleteDataset() {
        String requestBody = "SELECT * FROM test_users";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(3);
        assertThat(result.getExecutionTimeMs()).isGreaterThanOrEqualTo(0L);
    }

    @Test
    @DisplayName("should execute query with WHERE clause filtering")
    void shouldExecuteQueryWithWhereClauseFiltering() {
        String requestBody = "SELECT name, age FROM test_users WHERE age > 25";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        assertThat(result.getHeaders()).containsExactly("NAME", "AGE");
        assertThat(result.getData()).hasSize(2);
    }

    @Test
    @DisplayName("should return error for non-existent query ID")
    void shouldReturnErrorForNonExistentQueryId() {
        QueryResult result = queryService.executeQueryById(99999L);

        assertThat(result).isNotNull();
        assertThat(result.getHeaders()).contains("error");
        assertThat(result.getData().get(0).get(0).toString()).contains("Query not found");
    }

    @Test
    @DisplayName("should return error for invalid SQL syntax")
    void shouldReturnErrorForInvalidSqlSyntax() {
        String requestBody = "SELECT * FROM";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        assertThat(result.getHeaders()).contains("error");
    }

    @Test
    @DisplayName("should return TABLE_NOT_FOUND error for non-existent table")
    void shouldReturnTableNotFoundErrorForNonExistentTable() {
        String requestBody = "SELECT * FROM non_existent_table";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        assertThat(result.getHeaders()).contains("error");
        assertThat(result.getData().get(0).get(0).toString()).isEqualTo("TABLE_NOT_FOUND");
    }

    @Test
    @DisplayName("should cache and reuse results for identical queries")
    void shouldCacheAndReuseResultsForIdenticalQueries() {
        String requestBody = "SELECT * FROM test_users WHERE id = 1";

        List<Map<String, Long>> addResponse1 = queryService.addQueries(requestBody);
        long queryId1 = addResponse1.get(0).get("id");
        QueryResult result1 = queryService.executeQueryById(queryId1);

        List<Map<String, Long>> addResponse2 = queryService.addQueries(requestBody);
        long queryId2 = addResponse2.get(0).get("id");
        QueryResult result2 = queryService.executeQueryById(queryId2);

        assertThat(result1.getData()).isEqualTo(result2.getData());
        assertThat(result1.getHeaders()).isEqualTo(result2.getHeaders());
    }

    @Test
    @DisplayName("should handle query with JOIN asynchronously")
    void shouldHandleQueryWithJoinAsynchronously() throws InterruptedException {
        jdbcTemplate.execute("DROP TABLE IF EXISTS test_orders");
        jdbcTemplate.execute("CREATE TABLE test_orders (order_id INT PRIMARY KEY, user_id INT, amount DECIMAL)");
        jdbcTemplate.execute("INSERT INTO test_orders VALUES (1, 1, 100.50)");
        jdbcTemplate.execute("INSERT INTO test_orders VALUES (2, 2, 250.75)");

        String requestBody = "SELECT u.name, o.amount FROM test_users u JOIN test_orders o ON u.id = o.user_id";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        if (result.getStatus() == QueryStatus.RUNNING) {
            Thread.sleep(100);
            result = queryService.getQueryExecution(queryId);
        }

        assertThat(result.getData()).isNotEmpty();
    }

    @Test
    @DisplayName("should handle multiple SELECT subqueries asynchronously")
    void shouldHandleMultipleSelectSubqueriesAsynchronously() throws InterruptedException {
        String requestBody = "SELECT * FROM test_users WHERE id IN (SELECT id FROM test_users WHERE age > 25)";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        if (result.getStatus() == QueryStatus.RUNNING) {
            Thread.sleep(100);
            result = queryService.getQueryExecution(queryId);
        }

        assertThat(result.getData()).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("should execute full flow add list and execute query")
    void shouldExecuteFullFlowAddListAndExecuteQuery() {
        String requestBody = "SELECT name FROM test_users WHERE age = 30";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        assertThat(addResponse).hasSize(1);
        long queryId = addResponse.get(0).get("id");

        List<QueryEntry> allQueries = queryService.getQueries();
        assertThat(allQueries).isNotEmpty();
        assertThat(allQueries).anyMatch(q -> q.getId().equals(queryId));

        QueryResult result = queryService.executeQueryById(queryId);
        assertThat(result).isNotNull();
        assertThat(result.getHeaders()).contains("NAME");
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).get(0)).isEqualTo("Alice");
    }

    @Test
    @DisplayName("should execute full flow with multiple queries add list and execute all")
    void shouldExecuteFullFlowWithMultipleQueriesAddListAndExecuteAll() {
        String requestBody = "SELECT * FROM test_users WHERE id = 1; SELECT name FROM test_users WHERE age > 25; SELECT age FROM test_users WHERE name = 'Bob'";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        assertThat(addResponse).hasSize(3);

        List<QueryEntry> allQueries = queryService.getQueries();
        assertThat(allQueries.size()).isGreaterThanOrEqualTo(3);

        for (Map<String, Long> queryMap : addResponse) {
            long queryId = queryMap.get("id");
            QueryResult result = queryService.executeQueryById(queryId);
            assertThat(result).isNotNull();
            assertThat(result.getData()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("should handle empty result set gracefully")
    void shouldHandleEmptyResultSetGracefully() {
        String requestBody = "SELECT * FROM test_users WHERE id = 999";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        assertThat(result.getData()).isEmpty();
        assertThat(result.getHeaders()).isEmpty();
    }

    @Test
    @DisplayName("should handle ORDER BY clause correctly")
    void shouldHandleOrderByClauseCorrectly() {
        String requestBody = "SELECT name FROM test_users ORDER BY age DESC";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(3);
        assertThat(result.getData().get(0).get(0)).isEqualTo("Charlie");
        assertThat(result.getData().get(1).get(0)).isEqualTo("Alice");
        assertThat(result.getData().get(2).get(0)).isEqualTo("Bob");
    }

    @Test
    @DisplayName("should handle LIMIT clause correctly")
    void shouldHandleLimitClauseCorrectly() {
        String requestBody = "SELECT * FROM test_users LIMIT 2";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        assertThat(result.getData()).hasSize(2);
    }

    @Test
    @DisplayName("should track execution time for all queries")
    void shouldTrackExecutionTimeForAllQueries() {
        String requestBody = "SELECT * FROM test_users";

        List<Map<String, Long>> addResponse = queryService.addQueries(requestBody);
        long queryId = addResponse.get(0).get("id");

        QueryResult result = queryService.executeQueryById(queryId);

        assertThat(result).isNotNull();
        assertThat(result.getExecutionTimeMs()).isGreaterThanOrEqualTo(0L);
    }
}