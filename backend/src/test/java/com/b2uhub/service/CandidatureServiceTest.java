package com.b2uhub.service;

import com.b2uhub.dto.CandidatureRequest;
import com.b2uhub.dto.CandidatureResponse;
import com.b2uhub.model.Candidature;
import com.b2uhub.model.Entreprise;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.model.enums.MissionStatut;
import com.b2uhub.model.enums.RoleUtilisateur;
import com.b2uhub.repository.CandidatureHistoriqueRepository;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.EtudiantRepository;
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
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidatureServiceTest {

    @Mock
    private CandidatureRepository candidatureRepository;
    @Mock
    private CandidatureHistoriqueRepository historiqueRepository;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private MissionService missionService;
    @Mock
    private AiServiceClient aiServiceClient;
    @Mock
    private AiHubService aiHubService;
    @Mock
    private NotificationService notificationService;

    private CandidatureService candidatureService;

    private Mission mission;
    private Etudiant etudiant;

    @BeforeEach
    void setUp() {
        SecurityTestUtils.authenticateAs(100L, RoleUtilisateur.ETUDIANT);
        // seuil de préselection = 70, max candidatures actives = 5 (valeurs par défaut de application.yml)
        candidatureService = new CandidatureService(
                candidatureRepository,
                historiqueRepository,
                etudiantRepository,
                missionService,
                aiServiceClient,
                aiHubService,
                notificationService,
                70.0,
                5
        );

        Entreprise entreprise = new Entreprise();
        entreprise.setId(1L);
        entreprise.setNom("TechCorp");

        mission = new Mission();
        mission.setId(10L);
        mission.setTitre("Mission Test");
        mission.setStatut(MissionStatut.OUVERTE);
        mission.setCompetencesRequises(List.of("Java"));
        mission.setEntreprise(entreprise);

        etudiant = new Etudiant();
        etudiant.setId(100L);
        etudiant.setNom("Alice");
        etudiant.setCompetences(List.of("Java"));
    }

    @AfterEach
    void tearDown() {
        SecurityTestUtils.clearAuthentication();
    }

    @Test
    void testCreerCandidature_succes() {
        CandidatureRequest request = new CandidatureRequest();
        request.setMissionId(10L);
        request.setEtudiantId(100L);

        when(candidatureRepository.existsByMissionIdAndEtudiantId(10L, 100L)).thenReturn(false);
        when(candidatureRepository.countByEtudiantIdAndStatutIn(anyLong(), any())).thenReturn(0L);
        when(missionService.getMission(10L)).thenReturn(mission);
        when(etudiantRepository.findById(100L)).thenReturn(Optional.of(etudiant));
        when(aiHubService.resolvePerformance(etudiant)).thenReturn(75.0);
        when(aiServiceClient.scoreCandidature(any(), any(), anyDouble()))
                .thenReturn(new AiServiceClient.ScoreResult(65.0, "explication test", List.of(), false));
        when(candidatureRepository.save(any(Candidature.class))).thenAnswer(inv -> {
            Candidature c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CandidatureResponse response = candidatureService.postuler(request);

        assertThat(response).isNotNull();
        assertThat(response.getStatut()).isEqualTo(CandidatureStatut.EN_ATTENTE);
        verify(candidatureRepository, atLeastOnce()).save(any(Candidature.class));
    }

    @Test
    void testCreerCandidature_doubleCandidature_doitEchouer() {
        CandidatureRequest request = new CandidatureRequest();
        request.setMissionId(10L);
        request.setEtudiantId(100L);

        when(candidatureRepository.existsByMissionIdAndEtudiantId(10L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> candidatureService.postuler(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("déjà existante");

        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void testCreerCandidature_limiteCandidaturesActivesAtteinte_doitEchouer() {
        CandidatureRequest request = new CandidatureRequest();
        request.setMissionId(10L);
        request.setEtudiantId(100L);

        when(candidatureRepository.existsByMissionIdAndEtudiantId(10L, 100L)).thenReturn(false);
        when(candidatureRepository.countByEtudiantIdAndStatutIn(anyLong(), any())).thenReturn(5L);

        assertThatThrownBy(() -> candidatureService.postuler(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Limite");

        verify(candidatureRepository, never()).save(any());
        verify(missionService, never()).getMission(anyLong());
    }

    @Test
    void testCreerCandidature_scoreAuDessusDuSeuil_estPreselectionneeAutomatiquement() {
        CandidatureRequest request = new CandidatureRequest();
        request.setMissionId(10L);
        request.setEtudiantId(100L);

        when(candidatureRepository.existsByMissionIdAndEtudiantId(10L, 100L)).thenReturn(false);
        when(candidatureRepository.countByEtudiantIdAndStatutIn(anyLong(), any())).thenReturn(0L);
        when(missionService.getMission(10L)).thenReturn(mission);
        when(etudiantRepository.findById(100L)).thenReturn(Optional.of(etudiant));
        when(aiHubService.resolvePerformance(etudiant)).thenReturn(90.0);
        when(aiServiceClient.scoreCandidature(any(), any(), anyDouble()))
                .thenReturn(new AiServiceClient.ScoreResult(85.0, "excellent match", List.of(), true));
        when(candidatureRepository.save(any(Candidature.class))).thenAnswer(inv -> {
            Candidature c = inv.getArgument(0);
            if (c.getId() == null) c.setId(1L);
            return c;
        });

        CandidatureResponse response = candidatureService.postuler(request);

        assertThat(response.getStatut()).isEqualTo(CandidatureStatut.PRESELECTIONNEE);
        verify(historiqueRepository, atLeastOnce()).save(any());
    }

    @Test
    void testAccepterCandidature_creeUneEntreeDansHistorique() {
        SecurityTestUtils.authenticateAs(1L, RoleUtilisateur.ENTREPRISE);
        Candidature candidature = new Candidature();
        candidature.setId(1L);
        candidature.setStatut(CandidatureStatut.ENTRETIEN);
        candidature.setMission(mission);
        candidature.setEtudiant(etudiant);

        when(candidatureRepository.findById(1L)).thenReturn(Optional.of(candidature));
        when(candidatureRepository.save(any(Candidature.class))).thenAnswer(inv -> inv.getArgument(0));

        CandidatureResponse response = candidatureService.updateStatut(1L, CandidatureStatut.ACCEPTEE);

        assertThat(response.getStatut()).isEqualTo(CandidatureStatut.ACCEPTEE);
        verify(historiqueRepository, times(1)).save(any());
    }

    @Test
    void testChangerStatut_candidatureDejaTerminee_doitEchouer() {
        SecurityTestUtils.authenticateAs(1L, RoleUtilisateur.ENTREPRISE);
        Candidature candidature = new Candidature();
        candidature.setId(1L);
        candidature.setStatut(CandidatureStatut.REFUSEE);
        candidature.setMission(mission);
        candidature.setEtudiant(etudiant);

        when(candidatureRepository.findById(1L)).thenReturn(Optional.of(candidature));

        assertThatThrownBy(() -> candidatureService.updateStatut(1L, CandidatureStatut.ACCEPTEE))
                .isInstanceOf(BusinessException.class);

        verify(historiqueRepository, never()).save(any());
    }
}
