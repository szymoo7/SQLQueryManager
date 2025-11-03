package org.task.jetbrainstask.service.implementations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.models.QueryStatus;
import org.task.jetbrainstask.service.interfaces.QueryValidator;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


@Component
public class QueryValidatorImpl implements QueryValidator {

    public List<QueryEntry> parseAndValidate(String requestBody) {
        if (requestBody == null || requestBody.isBlank()) {
            return List.of();
        }

        List<String> queries = Arrays.stream(requestBody.split(";"))
                .map(String::trim)
                .filter(q -> !q.isEmpty())
                .toList();

        List<QueryEntry> validated = new ArrayList<>();

        for (String query : queries) {
            if (isValidSelectQuery(query)) {
                QueryEntry entry = new QueryEntry();
                entry.setQuery(query);
                entry.setStatus(QueryStatus.READY);
                validated.add(entry);
            }
        }

        return validated;
    }

    private boolean isValidSelectQuery(String query) {
        String normalized = query.trim().toUpperCase();
        return normalized.startsWith("SELECT")
                && !normalized.contains("DROP")
                && !normalized.contains("DELETE")
                && !normalized.contains("UPDATE")
                && !normalized.contains("INSERT")
                && !normalized.contains("--");
    }
}
