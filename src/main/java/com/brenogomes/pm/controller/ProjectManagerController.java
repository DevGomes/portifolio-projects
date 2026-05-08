package com.brenogomes.pm.controller;


import com.brenogomes.pm.model.ProjectRequest;
import com.brenogomes.pm.model.ProjectResponse;
import com.brenogomes.pm.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(path = "/api/v1")
@RequiredArgsConstructor
public class ProjectManagerController implements ProjectApi {

    private final ProjectService projectService;

    @Override
    public ResponseEntity<ProjectResponse> createProject(ProjectRequest projectRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(projectRequest));
    }

    @Override
    public ResponseEntity<Void> deleteProject(Long id) {
        projectService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<ProjectResponse> getProjectById(Long id) {
        return ResponseEntity.status(HttpStatus.OK).body(projectService.findById(id));
    }

    @Override
    public ResponseEntity<List<ProjectResponse>> listProjects() {
        return ResponseEntity.status(HttpStatus.OK).body(projectService.findAll());
    }

    @Override
    public ResponseEntity<ProjectResponse> updateProject(Long id, ProjectRequest projectRequest) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(projectService.update(id, projectRequest));
    }
}
