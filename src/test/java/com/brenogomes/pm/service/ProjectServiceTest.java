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
import com.brenogomes.pm.model.entity.ProjectStatusEntity;
import com.brenogomes.pm.repository.MemberRepository;
import com.brenogomes.pm.repository.ProjectRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProjectServiceTest {

    @Mock private ProjectRepository projectRepository;
    @Mock private MemberClient memberClient;
    @Mock private ProjectMapper projectMapper;
    @Mock private MemberRepository memberRepository;

    @InjectMocks
    private ProjectService projectService;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private Member buildMember(Long id, String name) {
        return Member.builder().id(id).name(name).email(name.toLowerCase() + "@test.com").build();
    }

    private MemberExternalResponse buildExternalMember(Long id, String name) {
        return new MemberExternalResponse(id, name, name.toLowerCase() + "@test.com", "Gerente");
    }

    private Project buildProject(Long id, ProjectStatusEntity status, BigDecimal budget,
                                 LocalDate start, LocalDate end) {
        return Project.builder()
                .id(id)
                .name("Projeto " + id)
                .status(status)
                .totalBudget(budget)
                .startDate(start)
                .estimatedEndDate(end)
                .members(new ArrayList<>())
                .build();
    }

    private ProjectRequest buildRequest(List<Long> memberIds, double budget,
                                        LocalDate start, LocalDate end,
                                        ProjectStatus status) {
        ProjectRequest req = new ProjectRequest();
        req.setName("Projeto Teste");
        req.setMemberIds(memberIds);
        req.setTotalBudget(budget);
        req.setStartDate(start);
        req.setEstimatedEndDate(end);
        req.setStatus(status);
        return req;
    }

    // -----------------------------------------------------------------------
    // findAll
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("deve retornar lista com risco calculado para cada projeto")
        void deveRetornarListaComRisco() {
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusMonths(2);

            Project p1 = buildProject(1L, ProjectStatusEntity.EM_ANDAMENTO,
                    new BigDecimal("50000.00"), start, end);
            Project p2 = buildProject(2L, ProjectStatusEntity.PLANEJADO,
                    new BigDecimal("600000.00"), start, end.plusMonths(8));

            ProjectResponse r1 = new ProjectResponse();
            ProjectResponse r2 = new ProjectResponse();

            when(projectRepository.findAll()).thenReturn(List.of(p1, p2));
            when(projectMapper.toResponse(p1)).thenReturn(r1);
            when(projectMapper.toResponse(p2)).thenReturn(r2);

            List<ProjectResponse> result = projectService.findAll();

            assertThat(result).hasSize(2);
            assertThat(r1.getRisk()).isEqualTo(ProjectRisk.BAIXO_RISCO);
            assertThat(r2.getRisk()).isEqualTo(ProjectRisk.ALTO_RISCO);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando não há projetos")
        void deveRetornarListaVazia() {
            when(projectRepository.findAll()).thenReturn(List.of());
            assertThat(projectService.findAll()).isEmpty();
        }
    }

    // -----------------------------------------------------------------------
    // findById
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("deve retornar projeto com risco calculado")
        void deveRetornarProjetoComRisco() {
            LocalDate start = LocalDate.now();
            Project project = buildProject(1L, ProjectStatusEntity.EM_ANDAMENTO,
                    new BigDecimal("200000.00"), start, start.plusMonths(4));
            ProjectResponse response = new ProjectResponse();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectMapper.toResponse(project)).thenReturn(response);

            ProjectResponse result = projectService.findById(1L);

            assertThat(result.getRisk()).isEqualTo(ProjectRisk.MEDIO_RISCO);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando projeto não existe")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.findById(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // -----------------------------------------------------------------------
    // create
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("deve criar projeto e retornar resposta com risco")
        void deveCriarProjeto() {
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusMonths(2);

            Member member = buildMember(1L, "João");
            MemberExternalResponse external = buildExternalMember(1L, "João");

            ProjectRequest request = buildRequest(List.of(1L), 50000.0, start, end, ProjectStatus.PLANEJADO);
            Project project = buildProject(1L, ProjectStatusEntity.PLANEJADO,
                    new BigDecimal("50000.00"), start, end);
            ProjectResponse response = new ProjectResponse();

            when(memberClient.findById(1L)).thenReturn(Optional.of(external));
            when(memberRepository.existsById(1L)).thenReturn(false);
            when(memberRepository.saveAll(anyList())).thenReturn(List.of(member));
            when(memberRepository.findAllById(List.of(1L))).thenReturn(List.of(member));
            when(projectRepository.save(any(Project.class))).thenReturn(project);
            when(projectMapper.toResponse(project)).thenReturn(response);

            ProjectResponse result = projectService.create(request);

            assertThat(result).isNotNull();
            assertThat(result.getRisk()).isEqualTo(ProjectRisk.BAIXO_RISCO);
            verify(projectRepository).save(any(Project.class));
        }

        @Test
        @DisplayName("deve lançar exceção quando lista de membros está vazia")
        void deveLancarExcecaoSemMembros() {
            ProjectRequest request = buildRequest(List.of(), 50000.0,
                    LocalDate.now(), LocalDate.now().plusMonths(1), ProjectStatus.PLANEJADO);

            assertThatThrownBy(() -> projectService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pelo menos 1 membro");
        }

        @Test
        @DisplayName("deve lançar exceção quando há mais de 10 membros")
        void deveLancarExcecaoComMaisDe10Membros() {
            List<Long> ids = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L);
            ProjectRequest request = buildRequest(ids, 50000.0,
                    LocalDate.now(), LocalDate.now().plusMonths(1), ProjectStatus.PLANEJADO);

            assertThatThrownBy(() -> projectService.create(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("10 membros");
        }

        @Test
        @DisplayName("deve lançar exceção quando membro não encontrado na API externa")
        void deveLancarExcecaoQuandoMembroNaoEncontrado() {
            ProjectRequest request = buildRequest(List.of(99L), 50000.0,
                    LocalDate.now(), LocalDate.now().plusMonths(1), ProjectStatus.PLANEJADO);

            when(memberClient.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.create(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("99");
        }
    }

    // -----------------------------------------------------------------------
    // update
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("deve atualizar dados do projeto e membros")
        void deveAtualizarProjeto() {
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusMonths(3);

            Member member = buildMember(1L, "Maria");
            MemberExternalResponse external = buildExternalMember(1L, "Maria");

            Project existing = buildProject(1L, ProjectStatusEntity.PLANEJADO,
                    new BigDecimal("80000.00"), start, end);
            existing.setMembers(new ArrayList<>(List.of(buildMember(2L, "Antigo"))));

            ProjectRequest request = buildRequest(List.of(1L), 80000.0, start, end, ProjectStatus.PLANEJADO);
            request.setName("Projeto Atualizado");
            ProjectResponse response = new ProjectResponse();

            when(projectRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(memberClient.findById(1L)).thenReturn(Optional.of(external));
            when(memberRepository.existsById(1L)).thenReturn(false);
            when(memberRepository.saveAll(anyList())).thenReturn(List.of(member));
            when(memberRepository.findAllById(List.of(1L))).thenReturn(List.of(member));
            when(projectRepository.save(existing)).thenReturn(existing);
            when(projectMapper.toResponse(existing)).thenReturn(response);

            ProjectResponse result = projectService.update(1L, request);

            assertThat(result).isNotNull();
            assertThat(existing.getName()).isEqualTo("Projeto Atualizado");
            // membros antigos removidos, novo membro adicionado
            assertThat(existing.getMembers()).containsExactly(member);
            verify(projectRepository).save(existing);
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando projeto não existe")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());
            ProjectRequest request = buildRequest(List.of(1L), 50000.0,
                    LocalDate.now(), LocalDate.now().plusMonths(1), ProjectStatus.PLANEJADO);

            assertThatThrownBy(() -> projectService.update(99L, request))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // -----------------------------------------------------------------------
    // delete
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("deve deletar projeto com status PLANEJADO")
        void deveDeletarProjetoPlanejado() {
            Project project = buildProject(1L, ProjectStatusEntity.PLANEJADO,
                    new BigDecimal("50000.00"), LocalDate.now(), LocalDate.now().plusMonths(1));

            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            projectService.delete(1L);

            verify(projectRepository).delete(project);
        }

        @Test
        @DisplayName("não deve deletar projeto com status INICIADO")
        void naoDeveDeletarIniciado() {
            Project project = buildProject(1L, ProjectStatusEntity.INICIADO,
                    new BigDecimal("50000.00"), LocalDate.now(), LocalDate.now().plusMonths(1));

            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.delete(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("inicializado ou encerrado");

            verify(projectRepository, never()).delete(any());
        }

        @Test
        @DisplayName("não deve deletar projeto com status EM_ANDAMENTO")
        void naoDeveDeletarEmAndamento() {
            Project project = buildProject(1L, ProjectStatusEntity.EM_ANDAMENTO,
                    new BigDecimal("50000.00"), LocalDate.now(), LocalDate.now().plusMonths(1));

            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.delete(1L))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(projectRepository, never()).delete(any());
        }

        @Test
        @DisplayName("não deve deletar projeto com status ENCERRADO")
        void naoDeveDeletarEncerrado() {
            Project project = buildProject(1L, ProjectStatusEntity.ENCERRADO,
                    new BigDecimal("50000.00"), LocalDate.now(), LocalDate.now().plusMonths(1));

            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

            assertThatThrownBy(() -> projectService.delete(1L))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(projectRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lançar EntityNotFoundException quando projeto não existe")
        void deveLancarExcecaoQuandoNaoEncontrado() {
            when(projectRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> projectService.delete(99L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // -----------------------------------------------------------------------
    // calculateRisk (via findById)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("calculateRisk")
    class CalculateRisk {

        private void mockProject(BigDecimal budget, LocalDate start, LocalDate end) {
            Project project = buildProject(1L, ProjectStatusEntity.PLANEJADO, budget, start, end);
            ProjectResponse response = new ProjectResponse();
            when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
            when(projectMapper.toResponse(project)).thenReturn(response);
        }

        @Test
        @DisplayName("ALTO_RISCO quando orçamento > 500k")
        void altoRiscoPorOrcamento() {
            mockProject(new BigDecimal("600000.00"), LocalDate.now(), LocalDate.now().plusMonths(2));
            assertThat(projectService.findById(1L).getRisk()).isEqualTo(ProjectRisk.ALTO_RISCO);
        }

        @Test
        @DisplayName("ALTO_RISCO quando prazo > 6 meses")
        void altoRiscoPorPrazo() {
            mockProject(new BigDecimal("50000.00"), LocalDate.now(), LocalDate.now().plusMonths(7));
            assertThat(projectService.findById(1L).getRisk()).isEqualTo(ProjectRisk.ALTO_RISCO);
        }

        @Test
        @DisplayName("MEDIO_RISCO quando orçamento entre 100k e 500k")
        void medioRiscoPorOrcamento() {
            mockProject(new BigDecimal("200000.00"), LocalDate.now(), LocalDate.now().plusMonths(2));
            assertThat(projectService.findById(1L).getRisk()).isEqualTo(ProjectRisk.MEDIO_RISCO);
        }

        @Test
        @DisplayName("MEDIO_RISCO quando prazo entre 3 e 6 meses")
        void medioRiscoPorPrazo() {
            mockProject(new BigDecimal("50000.00"), LocalDate.now(), LocalDate.now().plusMonths(4));
            assertThat(projectService.findById(1L).getRisk()).isEqualTo(ProjectRisk.MEDIO_RISCO);
        }

        @Test
        @DisplayName("BAIXO_RISCO quando orçamento <= 100k e prazo <= 3 meses")
        void baixoRisco() {
            mockProject(new BigDecimal("50000.00"), LocalDate.now(), LocalDate.now().plusMonths(2));
            assertThat(projectService.findById(1L).getRisk()).isEqualTo(ProjectRisk.BAIXO_RISCO);
        }
    }
}