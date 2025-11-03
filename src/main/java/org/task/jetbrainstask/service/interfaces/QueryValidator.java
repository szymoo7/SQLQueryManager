package org.task.jetbrainstask.service.interfaces;



import org.task.jetbrainstask.models.QueryEntry;

import java.util.List;

public interface QueryValidator {
    List<QueryEntry> parseAndValidate(String requestBody);
}
