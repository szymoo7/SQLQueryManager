package org.task.jetbrainstask.service.interfaces;

public interface QueryAnalyzer {
    boolean shouldRunAsync(String sql);
    void recordExecution(String sql, long durationMs);
}
