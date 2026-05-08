package com.brenogomes.pm.service;

import com.brenogomes.pm.client.MemberClient;
import com.brenogomes.pm.mapper.ProjectMapper;
import com.brenogomes.pm.model.ProjectRequest;
import com.brenogomes.pm.model.ProjectResponse;
import com.brenogomes.pm.model.ProjectRisk;
import com.brenogomes.pm.model.dto.MemberExternalResponse;
import com.brenogomes.pm.model.entity.Member;
import com.brenogomes.pm.model.entity.Project;
import com.brenogomes.pm.model.entity.ProjectStatus;
import com.brenogomes.pm.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final MemberClient memberClient;
    private final ProjectMapper projectMapper;

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll() {
        return projectMapper.toResponseList(projectRepository.findAll());
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        var result = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + id));
        var response = projectMapper.toResponse(result);
        response.setRisk(calculateRisk(result));
        return response;
    }

    @Transactional
    public ProjectResponse create(ProjectRequest projectRequest) {
        List<Member> members = resolveMembers(projectRequest.getMemberIds());
        Project project = getBuildProject(projectRequest, members);
        var projectSaved = projectRepository.save(project);
        var response = projectMapper.toResponse(projectSaved);
        response.setRisk(calculateRisk(project));
        return response;
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest projectRequest) {
        Project existing = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + id));
        List<Member> members = resolveMembers(projectRequest.getMemberIds());

        existing.setName(projectRequest.getName());
        existing.setStartDate(projectRequest.getStartDate());
        existing.setEstimatedEndDate(projectRequest.getEstimatedEndDate());
        existing.setActualEndDate(projectRequest.getActualEndDate());
        existing.setTotalBudget(BigDecimal.valueOf(projectRequest.getTotalBudget()));
        existing.setDescription(projectRequest.getDescription());
        existing.setStatus(ProjectStatus.valueOf(projectRequest.getStatus().getValue()));
        existing.setMembers(members);

        return projectMapper.toResponse(projectRepository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Projeto não encontrado com id: " + id));
        switch (project.getStatus()) {
            case INICIADO, EM_ANDAMENTO, ENCERRADO -> {
                throw new IllegalArgumentException("Não é permitido deletar projeto já inicializado ou encerrado");
            }
        }
        projectRepository.delete(project);
    }

    private Project getBuildProject(ProjectRequest projectRequest, List<Member> members) {
        return Project.builder()
                .name(projectRequest.getName())
                .startDate(projectRequest.getStartDate())
                .estimatedEndDate(projectRequest.getEstimatedEndDate())
                .actualEndDate(projectRequest.getActualEndDate())
                .totalBudget(BigDecimal.valueOf(projectRequest.getTotalBudget()))
                .description(projectRequest.getDescription())
                .status(ProjectStatus.valueOf(projectRequest.getStatus().getValue()))
                .members(members)
                .build();
    }

    private ProjectRisk calculateRisk(Project project) {
        BigDecimal budget = project.getTotalBudget();
        long months = ChronoUnit.MONTHS.between(
                project.getStartDate(),
                project.getEstimatedEndDate()
        );

        if (budget.compareTo(new BigDecimal("500000.00")) > 0 || months > 6) {
            return ProjectRisk.ALTO_RISCO;
        }

        // Médio risco: orçamento entre R$ 100.001 e R$ 500.000 OU prazo entre 3 e 6 meses
        if (budget.compareTo(new BigDecimal("100000.00")) > 0 || (months > 3 && months <= 6)) {
            return ProjectRisk.MEDIO_RISCO;
        }

        // Baixo risco: orçamento até R$ 100.000 E prazo <= 3 meses
        return ProjectRisk.BAIXO_RISCO;

    }

    private List<Member> resolveMembers(List<Long> memberIds) {
        if (memberIds == null || memberIds.isEmpty()) {
            throw new IllegalArgumentException("O projeto deve ter pelo menos 1 membro associado");
        }
        if (memberIds.size() > 10) {
            throw new IllegalArgumentException("O projeto pode ter no máximo 10 membros associados");
        }

        List<MemberExternalResponse> externalMembers = memberClient.findAllByIds(memberIds);

        if (externalMembers.size() != memberIds.size()) {
            List<Long> foundIds = externalMembers.stream().map(MemberExternalResponse::getId).toList();
            List<Long> notFoundIds = memberIds.stream().filter(mid -> !foundIds.contains(mid)).toList();
            throw new EntityNotFoundException("Membros não encontrados na API externa com ids: " + notFoundIds);
        }

        // Sincroniza localmente apenas os dados necessários para o relacionamento
        return externalMembers.stream()
                .map(ext -> Member.builder()
                        .id(ext.getId())
                        .name(ext.getName())
                        .email(ext.getRole())
                        .build())
                .toList();
    }
}