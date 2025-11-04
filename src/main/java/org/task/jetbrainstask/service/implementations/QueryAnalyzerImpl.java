package org.task.jetbrainstask.service.implementations;

import org.springframework.stereotype.Component;
import org.task.jetbrainstask.service.interfaces.QueryAnalyzer;

@Component
public class QueryAnalyzerImpl implements QueryAnalyzer {
    @Override
    public boolean shouldRunAsync(String sql) {
        return false;
    }

    @Override
    public void recordExecution(String sql, long durationMs) {

    }
}
