package com.example.springbootrestapi.controller;

import com.example.springbootrestapi.model.Dag;
import com.example.springbootrestapi.service.DagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dags")
public class DagController {

    @Autowired
    private DagService dagService;

    @GetMapping
    public List<Dag> getAllDags() {
        return dagService.getAllDags();
    }

    @GetMapping("/{id}")
    public Dag getDagById(@PathVariable String id) {
        return dagService.getDagById(id);
    }
}
