package org.task.jetbrainstask.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.task.jetbrainstask.models.QueryEntry;
import org.task.jetbrainstask.service.interfaces.QueryService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/queries")
public class QueriesController {

    private QueryService queriesService;

    @Autowired
    public QueriesController(QueryService queriesService) {
        this.queriesService = queriesService;
    }

    @PostMapping
    public List<Map<String, Long>> addQueries(@RequestBody String requestBody) {
        return null;
    }

    @GetMapping
    public List<QueryEntry> getQueries() {
        return null;
    }

}
