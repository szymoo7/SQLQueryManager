package org.task.jetbrainstask.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.interfaces.QueryExecutor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class QueryExecutorImpl implements QueryExecutor {
    private final JdbcTemplate jdbcTemplate;
    private static final Logger log = LoggerFactory.getLogger(QueryExecutorImpl.class);

    @Autowired
    public QueryExecutorImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    @Override
    public QueryResult executeQuery(String sql) {
        long start = System.currentTimeMillis();
        log.info("Executing SQL query: {}", sql);

        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            log.debug("Query returned {} rows", rows.size());

            List<String> headers = rows.isEmpty()
                    ? List.of()
                    : new ArrayList<>(rows.get(0).keySet());

            List<List<Object>> data = rows.stream()
                    .map(row -> headers.stream()
                            .map(row::get)
                            .toList())
                    .toList();

            long executionTime = System.currentTimeMillis() - start;

            QueryResult queryResult = new QueryResult();
            queryResult.setHeaders(headers);
            queryResult.setData(data);
            queryResult.setExecutionTimeMs(executionTime);

            log.info("Query executed successfully in {} ms with {} columns", executionTime, headers.size());
            return queryResult;

        } catch (BadSqlGrammarException e) {
            long executionTime = System.currentTimeMillis() - start;
            String sqlMessage = e.getSQLException().getMessage();
            log.warn("SQL syntax or table error in {} ms: {}", executionTime, sqlMessage);

            String message;
            if (sqlMessage != null && sqlMessage.toLowerCase().contains("not found")) {
                message = "TABLE_NOT_FOUND";
            } else {
                message = "SQL_ERROR: " + sqlMessage;
            }

            QueryResult errorResult = new QueryResult();
            errorResult.setExecutionTimeMs(executionTime);
            errorResult.setHeaders(List.of("error"));
            errorResult.setData(List.of(List.of(message)));
            return errorResult;

        } catch (DataAccessException e) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("Database access error in {} ms: {}", executionTime, e.getMessage());

            QueryResult errorResult = new QueryResult();
            errorResult.setExecutionTimeMs(executionTime);
            errorResult.setHeaders(List.of("error"));
            errorResult.setData(List.of(List.of("DATA_ACCESS_ERROR: " + e.getMessage())));
            return errorResult;

        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - start;
            log.error("Unexpected error executing query in {} ms: {}", executionTime, sql, e);

            QueryResult errorResult = new QueryResult();
            errorResult.setExecutionTimeMs(executionTime);
            errorResult.setHeaders(List.of("error"));
            errorResult.setData(List.of(List.of("UNEXPECTED_ERROR: " + e.getMessage())));
            return errorResult;
        }
    }
}
