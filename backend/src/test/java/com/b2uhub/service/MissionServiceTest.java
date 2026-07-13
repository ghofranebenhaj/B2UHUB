package com.b2uhub.service;

import com.b2uhub.dto.MissionRequest;
import com.b2uhub.dto.MissionResponse;
import com.b2uhub.model.Entreprise;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.model.enums.MissionStatut;
import com.b2uhub.model.enums.RoleUtilisateur;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.EntrepriseRepository;
import com.b2uhub.repository.MissionRepository;
import com.b2uhub.support.SecurityTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MissionServiceTest {

    @Mock
    private MissionRepository missionRepository;
    @Mock
    private EntrepriseRepository entrepriseRepository;
    @Mock
    private CandidatureRepository candidatureRepository;

    @InjectMocks
    private MissionService missionService;

    private Entreprise entreprise;
    private Mission mission;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.authenticateAs(1L, RoleUtilisateur.ENTREPRISE);
        entreprise = new Entreprise();
        entreprise.setId(1L);
        entreprise.setNom("TechCorp");

        mission = new Mission();
        mission.setId(10L);
        mission.setTitre("Mission Test");
        mission.setStatut(MissionStatut.OUVERTE);
        mission.setCompetencesRequises(List.of("Java"));
        mission.setEntreprise(entreprise);
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearAuthentication();
    }

    @Test
    void findAll_delegueAuRepositoryEtMappeEnResponse() {
        when(missionRepository.search(MissionStatut.OUVERTE, "Java", null))
                .thenReturn(List.of(mission));

        List<MissionResponse> result = missionService.findAll(MissionStatut.OUVERTE, "Java", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitre()).isEqualTo("Mission Test");
        verify(missionRepository).search(MissionStatut.OUVERTE, "Java", null);
    }

    @Test
    void findById_missionExistante_retourneLaMission() {
        when(missionRepository.findWithEntrepriseById(10L)).thenReturn(Optional.of(mission));

        MissionResponse response = missionService.findById(10L);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getEntrepriseNom()).isEqualTo("TechCorp");
    }

    @Test
    void findById_missionInexistante_leveResourceNotFound() {
        when(missionRepository.findWithEntrepriseById(99L)).thenReturn(Optional.empty());
        when(missionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> missionService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_entrepriseInexistante_leveResourceNotFound() {
        MissionRequest request = new MissionRequest();
        request.setTitre("Nouvelle mission");
        request.setEntrepriseId(1L);
        request.setCompetencesRequises(List.of("Java"));

        when(entrepriseRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> missionService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(missionRepository, never()).save(any());
    }

    @Test
    void create_succes_missionOuverteParDefaut() {
        MissionRequest request = new MissionRequest();
        request.setTitre("Nouvelle mission");
        request.setEntrepriseId(1L);
        request.setCompetencesRequises(List.of("Java", "Angular"));
        request.setDureeSemaines(6);

        when(entrepriseRepository.findById(1L)).thenReturn(Optional.of(entreprise));
        when(missionRepository.save(any(Mission.class))).thenAnswer(inv -> {
            Mission m = inv.getArgument(0);
            m.setId(20L);
            return m;
        });

        MissionResponse response = missionService.create(request);

        assertThat(response.getId()).isEqualTo(20L);
        assertThat(response.getStatut()).isEqualTo(MissionStatut.OUVERTE);
        verify(missionRepository).save(any(Mission.class));
    }

    @Test
    void updateStatut_versClotureeSansCandidatureAcceptee_doitEchouer() {
        when(missionRepository.findWithEntrepriseById(10L)).thenReturn(Optional.of(mission));
        when(candidatureRepository.existsByMissionIdAndStatut(10L, CandidatureStatut.ACCEPTEE))
                .thenReturn(false);

        assertThatThrownBy(() -> missionService.updateStatut(10L, MissionStatut.CLOTUREE))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("clôturer");

        assertThat(mission.getStatut()).isEqualTo(MissionStatut.OUVERTE);
    }

    @Test
    void updateStatut_versClotureeAvecCandidatureAcceptee_succes() {
        when(missionRepository.findWithEntrepriseById(10L)).thenReturn(Optional.of(mission));
        when(candidatureRepository.existsByMissionIdAndStatut(10L, CandidatureStatut.ACCEPTEE))
                .thenReturn(true);

        MissionResponse response = missionService.updateStatut(10L, MissionStatut.CLOTUREE);

        assertThat(response.getStatut()).isEqualTo(MissionStatut.CLOTUREE);
    }

    @Test
    void delete_missionExistante_supprimeApresAvoirDetacheEquipeEtCandidatures() {
        mission.setEquipe(new com.b2uhub.model.Equipe());
        when(missionRepository.findWithEntrepriseById(10L)).thenReturn(Optional.of(mission));

        missionService.delete(10L);

        assertThat(mission.getEquipe()).isNull();
        verify(missionRepository).delete(mission);
    }
}
