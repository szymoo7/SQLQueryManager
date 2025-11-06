package org.task.jetbrainstask.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.task.jetbrainstask.models.QueryResult;
import org.task.jetbrainstask.service.implementations.QueryServiceImpl;
import org.task.jetbrainstask.service.interfaces.QueryService;

@RestController
@RequestMapping("/execute")
public class ExecuteController {

    private QueryService queryService;

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    public ExecuteController(QueryServiceImpl queryService) {
        this.queryService = queryService;
    }

    @GetMapping()
    public QueryResult executeQueryById(@RequestParam("query") long queryId) {
        log.info("Executing query with ID={}", queryId);

        QueryResult result = queryService.executeQueryById(queryId);

        if (result == null) {
            log.warn("Query execution returned null result for ID={}", queryId);
            return QueryResult.error("Query execution returned null result ");
        }

        if (result.getData() == null || result.getData().isEmpty()) {
            log.debug("Query ID={} executed successfully but returned no data", queryId);
        } else {
            log.info("Query ID={} executed successfully, rows={}",
                    queryId, result.getData().size());
        }

        return result;
    }

    @GetMapping("/{id}")
    public QueryResult getQueryExecution(@PathVariable long id) {
        return queryService.getQueryExecution(id);
    }

}
