package org.task.jetbrainstask.models;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryResult {

    private Long id;
    private List<String> headers;
    private List<List<Object>> data = new ArrayList<>();;
    private String errorMessage;
    private QueryStatus status;
    private Long executionTimeMs = null;

    public QueryResult() {
    }

    public QueryResult(Long id, List<String> headers, List<List<Object>> rows) {
        this.id = id;
        this.headers = headers;
        this.data = rows;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<List<Object>> getData() {
        return data;
    }

    public void setData(List<List<Object>> data) {
        this.data = data;
    }

    public static QueryResult error(String message) {
        QueryResult qr = new QueryResult();
        qr.setHeaders(List.of("error"));
        qr.setData(List.of(List.of(message)));
        qr.setStatus(QueryStatus.FAILED);
        qr.setErrorMessage(message);
        return qr;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public QueryStatus getStatus() {
        return status;
    }

    public void setStatus(QueryStatus status) {
        this.status = status;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
