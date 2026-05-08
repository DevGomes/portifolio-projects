package com.brenogomes.pm.service;

import com.brenogomes.pm.client.MemberClient;
import com.brenogomes.pm.mapper.ProjectMapper;
import com.brenogomes.pm.model.ProjectRequest;
import com.brenogomes.pm.model.ProjectResponse;
import com.brenogomes.pm.model.ProjectRisk;
import com.brenogomes.pm.model.ProjectStatus;
import com.brenogomes.pm.model.dto.MemberExternalResponse;
import com.brenogomes.pm.model.entity.Member;
import com.brenogomes.pm.model.entity.Project;
import com.brenogomes.pm.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private MemberClient memberClient;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private ProjectService projectService;

    private Project project;
    private ProjectRequest projectRequest;
    private ProjectResponse projectResponse;
    private MemberExternalResponse memberExternal;
    private Member member;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .id(1L)
                .name("João Silva")
                .email("Desenvolvedor")
                .build();

        project = Project.builder()
                .id(1L)
                .name("Sistema Financeiro")
                .startDate(LocalDate.of(2024, 1, 1))
                .estimatedEndDate(LocalDate.of(2024, 3, 1))
                .totalBudget(new BigDecimal("50000.00"))
                .status(com.brenogomes.pm.model.entity.ProjectStatus.EM_ANALISE)
                .members(List.of(member))
                .build();

        memberExternal = new MemberExternalResponse(1L, "João Silva", "Desenvolvedor");

        projectRequest = new ProjectRequest();
        projectRequest.setName("Sistema Financeiro");
        projectRequest.setStartDate(LocalDate.of(2024, 1, 1));
        projectRequest.setEstimatedEndDate(LocalDate.of(2024, 3, 1));
        projectRequest.setTotalBudget(50000.00);
        projectRequest.setStatus(ProjectStatus.EM_ANALISE);
        projectRequest.setMemberIds(List.of(1L));

        projectResponse = new ProjectResponse();
        projectResponse.setId(1L);
        projectResponse.setName("Sistema Financeiro");
    }

    // =========================================================
    // findAll
    // =========================================================

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("Deve retornar lista de projetos mapeados")
        void shouldReturnMappedProjectList() {
            when(projectRepository.findAll()).thenReturn(List.of(project));
            when(projectMapper.toResponseList(List.of(project))).thenReturn(List.of(projectResponse));

            List<ProjectResponse> result = projectService.findAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(projectRepository).findAll();
            verify(projectMapper).toResponseList(List.of(project));
        }

        @Test
        @DisplayName("Deve retornar lista vazia quando não há projetos")
        void shouldReturnEmptyListWhenNoProjects() {
            when(projectRepository.findAll()).thenReturn(List.of());
            when(projectMapper.toResponseList(List.of())).thenReturn(List.of());

            List<ProjectResponse> result = projectService.findAll();

            assertThat(result).isEmpty();
        }
    }

    // =========================================================
    // findById
    // =========================================================

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("Deve retornar projeto com risco calculado quando encontrado")
        void shouldReturnProjectWithRiskWhenFound() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectMapper.toResponse(project)).thenReturn(projectResponse);

            ProjectResponse result = projectService.findById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getRisk()).isEqualTo(ProjectRisk.BAIXO_RISCO);
            verify(projectRepository).findById(1L);
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando projeto não encontrado")
        void shouldThrowEntityNotFoundWhenProjectNotFound() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================
    // create
    // =========================================================

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("Deve criar projeto com sucesso e retornar response com risco")
        void shouldCreateProjectSuccessfully() {
            when(memberClient.findAllByIds(List.of(1L))).thenReturn(List.of(memberExternal));
            when(projectRepository.save(any(Project.class))).thenReturn(project);
            when(projectMapper.toResponse(any(Project.class))).thenReturn(projectResponse);

            ProjectResponse result = projectService.create(projectRequest);

            assertThat(result).isNotNull();
            assertThat(result.getRisk()).isEqualTo(ProjectRisk.BAIXO_RISCO);
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando lista de membros for vazia")
        void shouldThrowWhenMemberIdsIsEmpty() {
            projectRequest.setMemberIds(List.of());

            assertThatThrownBy(() -> projectService.create(projectRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pelo menos 1 membro");
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando lista de membros ultrapassar 10")
        void shouldThrowWhenMemberIdsExceedsTen() {
            projectRequest.setMemberIds(List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L));

            assertThatThrownBy(() -> projectService.create(projectRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("no máximo 10 membros");
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando membro não encontrado na API externa")
        void shouldThrowWhenMemberNotFoundInExternalApi() {
            when(memberClient.findAllByIds(List.of(1L, 2L))).thenReturn(List.of(memberExternal));
            projectRequest.setMemberIds(List.of(1L, 2L));

            assertThatThrownBy(() -> projectService.create(projectRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("2");
        }
    }

    // =========================================================
    // update
    // =========================================================

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("Deve atualizar projeto com sucesso")
        void shouldUpdateProjectSuccessfully() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(memberClient.findAllByIds(List.of(1L))).thenReturn(List.of(memberExternal));
            when(projectRepository.save(any(Project.class))).thenReturn(project);
            when(projectMapper.toResponse(any(Project.class))).thenReturn(projectResponse);

            ProjectResponse result = projectService.update(1L, projectRequest);

            assertThat(result).isNotNull();
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando projeto não encontrado no update")
        void shouldThrowWhenProjectNotFoundOnUpdate() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.update(99L, projectRequest))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================
    // delete
    // =========================================================

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("Deve deletar projeto com sucesso quando status permitido")
        void shouldDeleteProjectSuccessfully() {
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            projectService.delete(1L);

            verify(projectRepository).delete(project);
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException ao tentar deletar projeto INICIADO")
        void shouldThrowWhenDeletingInitiatedProject() {
            project.setStatus(com.brenogomes.pm.model.entity.ProjectStatus.INICIADO);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.delete(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inicializado ou encerrado");
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException ao tentar deletar projeto EM_ANDAMENTO")
        void shouldThrowWhenDeletingInProgressProject() {
            project.setStatus(com.brenogomes.pm.model.entity.ProjectStatus.EM_ANDAMENTO);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.delete(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inicializado ou encerrado");
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException ao tentar deletar projeto ENCERRADO")
        void shouldThrowWhenDeletingClosedProject() {
            project.setStatus(com.brenogomes.pm.model.entity.ProjectStatus.ENCERRADO);
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.delete(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inicializado ou encerrado");
        }

        @Test
        @DisplayName("Deve lançar EntityNotFoundException quando projeto não encontrado no delete")
        void shouldThrowWhenProjectNotFoundOnDelete() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.delete(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // =========================================================
    // calculateRisk
    // =========================================================

    @Nested
    @DisplayName("calculateRisk")
    class CalculateRisk {

        @Test
        @DisplayName("Deve retornar BAIXO_RISCO para orçamento até 100k e prazo até 3 meses")
        void shouldReturnLowRisk() {
            project.setTotalBudget(new BigDecimal("80000.00"));
            project.setStartDate(LocalDate.of(2024, 1, 1));
            project.setEstimatedEndDate(LocalDate.of(2024, 3, 1));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectMapper.toResponse(project)).thenReturn(projectResponse);

            ProjectResponse result = projectService.findById(1L);

            assertThat(result.getRisk()).isEqualTo(ProjectRisk.BAIXO_RISCO);
        }

        @Test
        @DisplayName("Deve retornar MEDIO_RISCO para orçamento entre 100k e 500k")
        void shouldReturnMediumRiskByBudget() {
            project.setTotalBudget(new BigDecimal("250000.00"));
            project.setStartDate(LocalDate.of(2024, 1, 1));
            project.setEstimatedEndDate(LocalDate.of(2024, 3, 1));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectMapper.toResponse(project)).thenReturn(projectResponse);

            ProjectResponse result = projectService.findById(1L);

            assertThat(result.getRisk()).isEqualTo(ProjectRisk.MEDIO_RISCO);
        }

        @Test
        @DisplayName("Deve retornar MEDIO_RISCO para prazo entre 3 e 6 meses")
        void shouldReturnMediumRiskByDeadline() {
            project.setTotalBudget(new BigDecimal("50000.00"));
            project.setStartDate(LocalDate.of(2024, 1, 1));
            project.setEstimatedEndDate(LocalDate.of(2024, 5, 1));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectMapper.toResponse(project)).thenReturn(projectResponse);

            ProjectResponse result = projectService.findById(1L);

            assertThat(result.getRisk()).isEqualTo(ProjectRisk.MEDIO_RISCO);
        }

        @Test
        @DisplayName("Deve retornar ALTO_RISCO para orçamento acima de 500k")
        void shouldReturnHighRiskByBudget() {
            project.setTotalBudget(new BigDecimal("600000.00"));
            project.setStartDate(LocalDate.of(2024, 1, 1));
            project.setEstimatedEndDate(LocalDate.of(2024, 3, 1));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectMapper.toResponse(project)).thenReturn(projectResponse);

            ProjectResponse result = projectService.findById(1L);

            assertThat(result.getRisk()).isEqualTo(ProjectRisk.ALTO_RISCO);
        }

        @Test
        @DisplayName("Deve retornar ALTO_RISCO para prazo superior a 6 meses")
        void shouldReturnHighRiskByDeadline() {
            project.setTotalBudget(new BigDecimal("50000.00"));
            project.setStartDate(LocalDate.of(2024, 1, 1));
            project.setEstimatedEndDate(LocalDate.of(2024, 9, 1));
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectMapper.toResponse(project)).thenReturn(projectResponse);

            ProjectResponse result = projectService.findById(1L);

            assertThat(result.getRisk()).isEqualTo(ProjectRisk.ALTO_RISCO);
        }
    }
}