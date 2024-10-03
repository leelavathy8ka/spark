package com.example.springbootrestapi.controller;


import com.example.springbootrestapi.model.DagRun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.springbootrestapi.model.*;

import java.util.List;

@RestController
@RequestMapping("/api/airflow")
public class AirflowController {


    private final AirflowService airflowService;

    @Autowired
    public AirflowController(AirflowService airflowService) {
        this.airflowService = airflowService;
    }

    @GetMapping("/dags")
    public List<Dag> getAllDags() {
        return airflowService.getAllDags();
    }

    @PostMapping("/dags/{dagId}/trigger")
    public DagRun triggerDag(@PathVariable String dagId) {
        return airflowService.triggerDag(dagId);
    }
}
