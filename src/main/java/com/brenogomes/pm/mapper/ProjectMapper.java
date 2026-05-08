package com.brenogomes.pm.mapper;

import com.brenogomes.pm.model.MemberSummary;
import com.brenogomes.pm.model.ProjectResponse;
import com.brenogomes.pm.model.ProjectStatus;
import com.brenogomes.pm.model.entity.Member;
import com.brenogomes.pm.model.entity.Project;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        ProjectResponse response = new ProjectResponse();

        response.setId(project.getId());
        response.setName(project.getName());
        response.setStartDate(project.getStartDate());
        response.setEstimatedEndDate(project.getEstimatedEndDate());
        response.setActualEndDate(project.getActualEndDate());
        response.setTotalBudget(project.getTotalBudget() != null
                ? project.getTotalBudget().doubleValue()
                : null);
        response.setDescription(project.getDescription());
        response.setStatus(ProjectStatus.fromValue(project.getStatus().name()));
        response.setMembers(toMemberSummaryList(project.getMembers()));

        return response;
    }

    public List<ProjectResponse> toResponseList(List<Project> projects) {
        return projects.stream()
                .map(this::toResponse)
                .toList();
    }

    private List<MemberSummary> toMemberSummaryList(List<Member> members) {
        if (members == null) return List.of();
        return members.stream()
                .map(this::toMemberSummary)
                .toList();
    }

    private MemberSummary toMemberSummary(Member member) {
        MemberSummary summary = new MemberSummary();
        summary.setId(member.getId());
        summary.setName(member.getName());
        summary.setEmail(member.getEmail());
        return summary;
    }
}
