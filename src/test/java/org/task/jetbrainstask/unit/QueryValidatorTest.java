package org.task.jetbrainstask.unit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryStatus;
import org.task.jetbrainstask.service.implementations.QueryValidatorImpl;
import org.task.jetbrainstask.service.interfaces.QueryValidator;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("QueryValidator Tests")
class QueryValidatorTest {

    private QueryValidator validator;

    @BeforeEach
    void setUp() {
        validator = new QueryValidatorImpl();
    }

    @Test
    @DisplayName("Should accept simple SELECT query")
    void shouldAcceptSimpleSelectQuery() {
        String input = "SELECT * FROM passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
        assertEquals("SELECT * FROM passengers", result.get(0).getQuery());
        assertEquals(QueryStatus.READY, result.get(0).getStatus());
    }

    @Test
    @DisplayName("Should accept multiple SELECT queries separated by semicolon")
    void shouldAcceptMultipleQueries() {
        String input = "SELECT * FROM passengers; SELECT Name FROM passengers WHERE Age > 30";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(2, result.size());
        assertEquals("SELECT * FROM passengers", result.get(0).getQuery());
        assertEquals("SELECT Name FROM passengers WHERE Age > 30", result.get(1).getQuery());

        assertTrue(result.stream().allMatch(q -> q.getStatus() == QueryStatus.READY));
    }

    @Test
    @DisplayName("Should accept SELECT with WHERE clause")
    void shouldAcceptSelectWithWhere() {
        String input = "SELECT * FROM passengers WHERE Age > 30";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getQuery().contains("WHERE"));
    }

    @Test
    @DisplayName("Should accept SELECT with JOIN")
    void shouldAcceptSelectWithJoin() {
        String input = "SELECT p.*, t.* FROM passengers p JOIN tickets t ON p.id = t.passenger_id";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getQuery().contains("JOIN"));
    }

    @Test
    @DisplayName("Should accept SELECT with GROUP BY and ORDER BY")
    void shouldAcceptSelectWithGroupByAndOrderBy() {
        String input = "SELECT Pclass, COUNT(*) FROM passengers GROUP BY Pclass ORDER BY Pclass";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should accept lowercase select query")
    void shouldAcceptLowercaseSelect() {
        String input = "select * from passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
        assertEquals("select * from passengers", result.get(0).getQuery());
    }

    @Test
    @DisplayName("Should accept mixed case SELECT query")
    void shouldAcceptMixedCaseSelect() {
        String input = "SeLeCt * FrOm passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should accept SELECT with subquery")
    void shouldAcceptSelectWithSubquery() {
        String input = "SELECT * FROM passengers WHERE Age > (SELECT AVG(Age) FROM passengers)";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should accept SELECT with LIMIT")
    void shouldAcceptSelectWithLimit() {
        String input = "SELECT * FROM passengers LIMIT 10";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should reject INSERT query")
    void shouldRejectInsertQuery() {
        String input = "INSERT INTO passengers VALUES (1, 'John', 30)";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should reject UPDATE query")
    void shouldRejectUpdateQuery() {
        String input = "UPDATE passengers SET Age = 30 WHERE PassengerId = 1";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should reject DELETE query")
    void shouldRejectDeleteQuery() {
        String input = "DELETE FROM passengers WHERE PassengerId = 1";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should reject DROP query")
    void shouldRejectDropQuery() {
        String input = "DROP TABLE passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should reject query with SQL comment (--)")
    void shouldRejectQueryWithComment() {
        String input = "SELECT * FROM passengers -- malicious comment";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should reject SELECT with hidden DROP in second statement")
    void shouldRejectSelectWithHiddenDrop() {
        String input = "SELECT * FROM passengers; DROP TABLE passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
        assertEquals("SELECT * FROM passengers", result.get(0).getQuery());
    }

    @Test
    @DisplayName("Should reject query starting with non-SELECT keyword")
    void shouldRejectNonSelectStart() {
        String input = "CREATE TABLE test (id INT)";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should return empty list for null input")
    void shouldReturnEmptyListForNullInput() {
        List<QueryEntry> result = validator.parseAndValidate(null);

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should return empty list for empty string")
    void shouldReturnEmptyListForEmptyString() {
        List<QueryEntry> result = validator.parseAndValidate("");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should return empty list for blank string (spaces/tabs)")
    void shouldReturnEmptyListForBlankString() {
        List<QueryEntry> result = validator.parseAndValidate("   \t  \n  ");

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should handle multiple semicolons")
    void shouldHandleMultipleSemicolons() {
        String input = "SELECT * FROM passengers;;; SELECT Name FROM passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should trim whitespace around queries")
    void shouldTrimWhitespace() {
        String input = "  SELECT * FROM passengers  ;  SELECT Name FROM passengers  ";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(2, result.size());
        assertEquals("SELECT * FROM passengers", result.get(0).getQuery());
        assertEquals("SELECT Name FROM passengers", result.get(1).getQuery());
    }

    @Test
    @DisplayName("Should skip empty queries between semicolons")
    void shouldSkipEmptyQueries() {
        String input = "SELECT * FROM passengers; ; ;SELECT Name FROM passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("Should handle query ending with semicolon")
    void shouldHandleTrailingSemicolon() {
        String input = "SELECT * FROM passengers;";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should handle query with no semicolon")
    void shouldHandleNoSemicolon() {
        String input = "SELECT * FROM passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should filter out invalid queries from mixed input")
    void shouldFilterInvalidFromMixed() {
        String input = "SELECT * FROM passengers; " +
                "UPDATE passengers SET Age = 30; " +
                "SELECT Name FROM passengers; " +
                "DROP TABLE passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(2, result.size());
        assertEquals("SELECT * FROM passengers", result.get(0).getQuery());
        assertEquals("SELECT Name FROM passengers", result.get(1).getQuery());
    }

    @Test
    @DisplayName("Should return empty list if all queries invalid")
    void shouldReturnEmptyIfAllInvalid() {
        String input = "UPDATE passengers SET Age = 30; DELETE FROM passengers; DROP TABLE passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should accept first valid and reject rest invalid")
    void shouldAcceptFirstValidRejectRestInvalid() {
        String input = "SELECT * FROM passengers; INSERT INTO passengers VALUES (1); DELETE FROM passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(1, result.size());
        assertEquals("SELECT * FROM passengers", result.get(0).getQuery());
    }

    @Test
    @DisplayName("Should reject SQL injection with comment")
    void shouldRejectSqlInjectionWithComment() {
        String input = "SELECT * FROM passengers WHERE Name = 'John' -- OR '1'='1'";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should reject query trying to bypass with nested DROP")
    void shouldRejectNestedDrop() {
        String input = "SELECT * FROM passengers WHERE Name = (SELECT 'value' FROM (DROP TABLE x))";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should reject query with DELETE in string literal (protection)")
    void shouldRejectDeleteInQuery() {
        String input = "SELECT * FROM passengers WHERE Name = 'DELETE'";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should reject query with UPDATE keyword anywhere")
    void shouldRejectUpdateKeywordAnywhere() {
        String input = "SELECT updated_at FROM passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("Should handle large number of queries efficiently")
    void shouldHandleLargeNumberOfQueries() {
        StringBuilder input = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            input.append("SELECT * FROM passengers WHERE PassengerId = ").append(i).append(";");
        }

        long startTime = System.currentTimeMillis();
        List<QueryEntry> result = validator.parseAndValidate(input.toString());
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(100, result.size());
        assertTrue(duration < 1000, "Validation should take less than 1 second for 100 queries");
    }

    @Test
    @DisplayName("Should handle very long query string")
    void shouldHandleVeryLongQuery() {
        StringBuilder input = new StringBuilder("SELECT ");
        for (int i = 0; i < 1000; i++) {
            input.append("col").append(i).append(", ");
        }
        input.append("id FROM passengers");

        List<QueryEntry> result = validator.parseAndValidate(input.toString());

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("All validated queries should have READY status")
    void allValidatedQueriesShouldHaveReadyStatus() {
        String input = "SELECT * FROM passengers; SELECT Name FROM passengers; SELECT Age FROM passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertEquals(3, result.size());
        assertTrue(result.stream().allMatch(q -> q.getStatus() == QueryStatus.READY),
                "All queries should have READY status");
    }

    @Test
    @DisplayName("Validated queries should not be null")
    void validatedQueriesShouldNotBeNull() {
        String input = "SELECT * FROM passengers";

        List<QueryEntry> result = validator.parseAndValidate(input);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertNotNull(result.get(0));
        assertNotNull(result.get(0).getQuery());
        assertNotNull(result.get(0).getStatus());
    }
}