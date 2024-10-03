package com.example.springbootrestapi.service;

import com.example.springbootrestapi.model.Dag;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DagService {
    private final List<Dag> dags = new ArrayList<>();

    public DagService() {
        // Sample data
        dags.add(new Dag("1", "Example DAG 1"));
        dags.add(new Dag("2", "Example DAG 2"));
    }

    public List<Dag> getAllDags() {
        return dags;
    }

    public Dag getDagById(String id) {
        return dags.stream().filter(dag -> dag.getId().equals(id)).findFirst().orElse(null);
    }
}
