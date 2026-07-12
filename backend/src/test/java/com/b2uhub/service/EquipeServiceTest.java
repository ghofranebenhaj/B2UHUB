package com.b2uhub.service;

import com.b2uhub.dto.EquipeResponse;
import com.b2uhub.model.Candidature;
import com.b2uhub.model.Entreprise;
import com.b2uhub.model.Equipe;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.EquipeRepository;
import com.b2uhub.repository.EtudiantRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EquipeServiceTest {

    @Mock
    private EquipeRepository equipeRepository;
    @Mock
    private CandidatureRepository candidatureRepository;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private MissionService missionService;
    @Mock
    private AiServiceClient aiServiceClient;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private EquipeService equipeService;

    private Mission mission;
    private Etudiant etudiant;
    private Candidature candidatureAcceptee;

    @BeforeEach
    void setUp() {
        Entreprise entreprise = new Entreprise();
        entreprise.setId(1L);
        entreprise.setNom("TechCorp");

        mission = new Mission();
        mission.setId(10L);
        mission.setTitre("Mission Test");
        mission.setEntreprise(entreprise);

        etudiant = new Etudiant();
        etudiant.setId(100L);
        etudiant.setNom("Alice");
        etudiant.setCompetences(List.of("Java"));

        candidatureAcceptee = new Candidature();
        candidatureAcceptee.setId(1L);
        candidatureAcceptee.setMission(mission);
        candidatureAcceptee.setEtudiant(etudiant);
        candidatureAcceptee.setStatut(CandidatureStatut.ACCEPTEE);
        candidatureAcceptee.setScoreIA(80.0);
    }

    @Test
    void formTeam_aucuneCandidatureAcceptee_doitEchouer() {
        when(missionService.getMission(10L)).thenReturn(mission);
        when(candidatureRepository.findByMissionIdAndStatut(10L, CandidatureStatut.ACCEPTEE))
                .thenReturn(List.of());

        assertThatThrownBy(() -> equipeService.formTeam(10L, 3))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Aucune candidature acceptée");

        verify(equipeRepository, never()).save(any());
    }

    @Test
    void formTeam_serviceIaDisponible_utiliseLeResultatDeLIA() {
        when(missionService.getMission(10L)).thenReturn(mission);
        when(candidatureRepository.findByMissionIdAndStatut(10L, CandidatureStatut.ACCEPTEE))
                .thenReturn(List.of(candidatureAcceptee));
        when(aiServiceClient.formTeam(eq(mission), any(), eq(1)))
                .thenReturn(new AiServiceClient.TeamResult(List.of(100L), 90.0, "bon match", true));
        when(equipeRepository.findByMissionId(10L)).thenReturn(Optional.empty());
        when(etudiantRepository.findById(100L)).thenReturn(Optional.of(etudiant));
        when(equipeRepository.save(any(Equipe.class))).thenAnswer(inv -> {
            Equipe e = inv.getArgument(0);
            e.setId(50L);
            return e;
        });

        EquipeResponse response = equipeService.formTeam(10L, 1);

        assertThat(response.getMissionId()).isEqualTo(10L);
        assertThat(response.getMembresNoms()).containsExactly("Alice");
        verify(notificationService).notify(eq(etudiant), any(), any());
        verify(equipeRepository).save(any(Equipe.class));
    }

    @Test
    void formTeam_serviceIaIndisponible_repliSurLesMeilleursScoresLocaux() {
        when(missionService.getMission(10L)).thenReturn(mission);
        when(candidatureRepository.findByMissionIdAndStatut(10L, CandidatureStatut.ACCEPTEE))
                .thenReturn(List.of(candidatureAcceptee));
        // fallback : l'IA ne renvoie aucun membre -> formation locale par score
        when(aiServiceClient.formTeam(eq(mission), any(), eq(1)))
                .thenReturn(new AiServiceClient.TeamResult(List.of(), 0, "indisponible", false));
        when(equipeRepository.findByMissionId(10L)).thenReturn(Optional.empty());
        when(etudiantRepository.findById(100L)).thenReturn(Optional.of(etudiant));
        when(equipeRepository.save(any(Equipe.class))).thenAnswer(inv -> {
            Equipe e = inv.getArgument(0);
            e.setId(51L);
            return e;
        });

        EquipeResponse response = equipeService.formTeam(10L, 1);

        assertThat(response.getMembresNoms()).containsExactly("Alice");
    }

    @Test
    void getByMission_aucuneEquipe_leveResourceNotFound() {
        when(equipeRepository.findByMissionId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipeService.getByMission(10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByMission_equipeExistante_retourneLesMembres() {
        Equipe equipe = new Equipe();
        equipe.setId(50L);
        equipe.setNom("Équipe Test");
        equipe.setMission(mission);
        equipe.getMembres().add(etudiant);

        when(equipeRepository.findByMissionId(10L)).thenReturn(Optional.of(equipe));

        EquipeResponse response = equipeService.getByMission(10L);

        assertThat(response.getMembresNoms()).containsExactly("Alice");
        assertThat(response.getMissionTitre()).isEqualTo("Mission Test");
    }
}
