package org.task.jetbrainstask.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.task.jetbrainstask.service.interfaces.QueryAnalyzer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class QueryAnalyzerImpl implements QueryAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(QueryAnalyzerImpl.class);
    private static final long ASYNC_THRESHOLD_MS = 5000;

    private final Map<String, Long> executionHistory = new ConcurrentHashMap<>();

    private static final Pattern SELECT_PATTERN = Pattern.compile("\\bSELECT\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern JOIN_PATTERN = Pattern.compile("\\bJOIN\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern CROSS_JOIN_PATTERN = Pattern.compile("\\bCROSS\\s+JOIN\\b", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean shouldRunAsync(String sql) {
        if (sql == null || sql.isBlank()) {
            log.warn("Received empty or null SQL query — cannot analyze, running synchronously.");
            return false;
        }

        String withoutStrings = removeStringLiterals(sql);

        String normalized = normalizeSQL(withoutStrings);
        log.debug("Analyzing query for async execution: {}", normalized);

        Long lastTime = executionHistory.get(normalized);
        if (lastTime != null && lastTime > ASYNC_THRESHOLD_MS) {
            log.info("Query previously took {} ms (> {} ms) → running asynchronously.", lastTime, ASYNC_THRESHOLD_MS);
            return true;
        }

        int joinCount = countPatternOccurrences(normalized, JOIN_PATTERN);
        int selectCount = countPatternOccurrences(normalized, SELECT_PATTERN);

        if (joinCount >= 1) {
            log.info("Query has {} JOINs → running asynchronously.", joinCount);
            return true;
        }

        if (CROSS_JOIN_PATTERN.matcher(normalized).find()) {
            log.info("Query contains CROSS JOIN → running asynchronously.");
            return true;
        }

        if (selectCount > 1) {
            log.info("Query has {} SELECT statements → running asynchronously.", selectCount);
            return true;
        }

        log.debug("Query is simple enough — will run synchronously.");
        return false;
    }

    @Override
    public void recordExecution(String sql, long durationMs) {
        if (sql == null || sql.isBlank()) {
            log.warn("Cannot record execution time for empty or null SQL query.");
            return;
        }

        String normalized = normalizeSQL(removeStringLiterals(sql));
        executionHistory.put(normalized, durationMs);
        log.debug("Recorded execution time for query [{}]: {} ms", normalized, durationMs);
    }

    private String removeStringLiterals(String sql) {
        if (sql == null) return null;
        return sql.replaceAll("(?s)'(?:''|[^'])*'", " ");
    }

    private String normalizeSQL(String sql) {
        if (sql == null) return "";
        String withSpaces = sql
                .replaceAll("([()\\[\\];,])", " $1 ")
                .replaceAll("\\s+", " ")
                .trim()
                .toUpperCase();

        return withSpaces;
    }

    private int countPatternOccurrences(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        int count = 0;
        while (m.find()) {
            count++;
        }
        return count;
    }
}
