package org.task.jetbrainstask.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.service.implementations.QueryServiceImpl;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/queries")
public class QueriesController {

    private static final Logger log = LoggerFactory.getLogger(QueriesController.class);
    private final QueryServiceImpl queriesService;

    @Autowired
    public QueriesController(QueryServiceImpl queriesService) {
        this.queriesService = queriesService;
    }

    @PostMapping
    public List<Map<String, Long>> addQueries(@RequestBody String requestBody) {
        log.info("Received POST /queries with body length={} chars",
                requestBody != null ? requestBody.length() : 0);

        List<Map<String, Long>> response = queriesService.addQueries(requestBody);

        log.info("Added {} queries", response.size());
        return response;
    }

    @GetMapping
    public List<QueryEntry> getQueries() {
        log.debug("GET /queries called");
        List<QueryEntry> queries = queriesService.getQueries();
        log.info("Returning {} queries from queue", queries.size());
        return queries;
    }

}
