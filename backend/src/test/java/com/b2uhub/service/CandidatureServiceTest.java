package com.b2uhub.service;

import com.b2uhub.dto.CandidatureRequest;
import com.b2uhub.dto.CandidatureResponse;
import com.b2uhub.model.Candidature;
import com.b2uhub.model.Entreprise;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.HistoriqueStatut;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.EtudiantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Test unitaire du CandidatureService, sur le même modèle que MissionServiceTest
 * et EquipeServiceTest (Mockito, sans contexte Spring).
 *
 * IMPORTANT : les noms exacts des méthodes de CandidatureService, CandidatureRepository
 * et AiServiceClient utilisés ici (postuler, updateStatut, getHistorique, matchScore, ...)
 * sont déduits du comportement observé dans la collection Postman et des autres tests
 * du projet (MissionServiceTest, EquipeServiceTest, AnalyticsServiceTest). Si les noms
 * réels diffèrent dans le code source, adapter uniquement les appels correspondants :
 * la structure et les cas de test restent valables.
 */
@ExtendWith(MockitoExtension.class)
class CandidatureServiceTest {

    @Mock
    private CandidatureRepository candidatureRepository;
    @Mock
    private MissionService missionService;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private AiServiceClient aiServiceClient;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private CandidatureService candidatureService;

    private Mission mission;
    private Etudiant etudiant;
    private Entreprise entreprise;

    @BeforeEach
    void setUp() {
        entreprise = new Entreprise();
        entreprise.setId(1L);
        entreprise.setNom("TechCorp");

        mission = new Mission();
        mission.setId(10L);
        mission.setTitre("Mission Test");
        mission.setEntreprise(entreprise);

        etudiant = new Etudiant();
        etudiant.setId(100L);
        etudiant.setNom("Alice");
        etudiant.setCompetences(List.of("Java", "Spring"));
    }

    // ---------------------------------------------------------------
    // postuler()
    // ---------------------------------------------------------------

    @Test
    void postuler_succes_creeCandidatureEnAttenteAvecScoreIA() {
        CandidatureRequest request = new CandidatureRequest();
        request.setMissionId(10L);
        request.setEtudiantId(100L);

        when(missionService.getMission(10L)).thenReturn(mission);
        when(etudiantRepository.findById(100L)).thenReturn(Optional.of(etudiant));
        when(candidatureRepository.existsByMissionIdAndEtudiantId(10L, 100L)).thenReturn(false);
        when(aiServiceClient.matchScore(etudiant, mission)).thenReturn(82.5);
        when(candidatureRepository.save(any(Candidature.class))).thenAnswer(inv -> {
            Candidature c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        CandidatureResponse response = candidatureService.postuler(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatut()).isEqualTo(CandidatureStatut.EN_ATTENTE);
        assertThat(response.getScoreIA()).isEqualTo(82.5);

        ArgumentCaptor<Candidature> captor = ArgumentCaptor.forClass(Candidature.class);
        verify(candidatureRepository).save(captor.capture());
        assertThat(captor.getValue().getMission()).isEqualTo(mission);
        assertThat(captor.getValue().getEtudiant()).isEqualTo(etudiant);
    }

    @Test
    void postuler_missionInexistante_leveResourceNotFound() {
        CandidatureRequest request = new CandidatureRequest();
        request.setMissionId(999L);
        request.setEtudiantId(100L);

        when(missionService.getMission(999L))
                .thenThrow(new ResourceNotFoundException("Mission introuvable"));

        assertThatThrownBy(() -> candidatureService.postuler(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void postuler_etudiantInexistant_leveResourceNotFound() {
        CandidatureRequest request = new CandidatureRequest();
        request.setMissionId(10L);
        request.setEtudiantId(999L);

        when(missionService.getMission(10L)).thenReturn(mission);
        when(etudiantRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidatureService.postuler(request))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void postuler_dejaCandidatSurLaMission_leveBusinessException() {
        CandidatureRequest request = new CandidatureRequest();
        request.setMissionId(10L);
        request.setEtudiantId(100L);

        when(missionService.getMission(10L)).thenReturn(mission);
        when(etudiantRepository.findById(100L)).thenReturn(Optional.of(etudiant));
        when(candidatureRepository.existsByMissionIdAndEtudiantId(10L, 100L)).thenReturn(true);

        assertThatThrownBy(() -> candidatureService.postuler(request))
                .isInstanceOf(BusinessException.class);

        verify(candidatureRepository, never()).save(any());
    }

    @Test
    void postuler_serviceIaIndisponible_creeQuandMemeLaCandidatureSansScore() {
        CandidatureRequest request = new CandidatureRequest();
        request.setMissionId(10L);
        request.setEtudiantId(100L);

        when(missionService.getMission(10L)).thenReturn(mission);
        when(etudiantRepository.findById(100L)).thenReturn(Optional.of(etudiant));
        when(candidatureRepository.existsByMissionIdAndEtudiantId(10L, 100L)).thenReturn(false);
        // le service IA externe est indisponible : on ne doit pas bloquer la candidature
        when(aiServiceClient.matchScore(etudiant, mission)).thenReturn(null);
        when(candidatureRepository.save(any(Candidature.class))).thenAnswer(inv -> {
            Candidature c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CandidatureResponse response = candidatureService.postuler(request);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getScoreIA()).isNull();
        verify(candidatureRepository).save(any(Candidature.class));
    }

    // ---------------------------------------------------------------
    // findByMission()
    // ---------------------------------------------------------------

    @Test
    void findByMission_delegueAuRepositoryEtMappeEnResponse() {
        Candidature candidature = new Candidature();
        candidature.setId(1L);
        candidature.setMission(mission);
        candidature.setEtudiant(etudiant);
        candidature.setStatut(CandidatureStatut.EN_ATTENTE);
        candidature.setScoreIA(75.0);

        when(candidatureRepository.findByMissionId(10L)).thenReturn(List.of(candidature));

        List<CandidatureResponse> result = candidatureService.findByMission(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEtudiantNom()).isEqualTo("Alice");
        assertThat(result.get(0).getStatut()).isEqualTo(CandidatureStatut.EN_ATTENTE);
        verify(candidatureRepository).findByMissionId(10L);
    }

    // ---------------------------------------------------------------
    // updateStatut()
    // ---------------------------------------------------------------

    @Test
    void updateStatut_versAcceptee_succesEtNotifieLetudiant() {
        Candidature candidature = new Candidature();
        candidature.setId(1L);
        candidature.setMission(mission);
        candidature.setEtudiant(etudiant);
        candidature.setStatut(CandidatureStatut.EN_ATTENTE);

        when(candidatureRepository.findById(1L)).thenReturn(Optional.of(candidature));

        CandidatureResponse response = candidatureService.updateStatut(1L, CandidatureStatut.ACCEPTEE);

        assertThat(response.getStatut()).isEqualTo(CandidatureStatut.ACCEPTEE);
        assertThat(candidature.getStatut()).isEqualTo(CandidatureStatut.ACCEPTEE);
        verify(notificationService).notify(eq(etudiant), any(), any());
    }

    @Test
    void updateStatut_candidatureInexistante_leveResourceNotFound() {
        when(candidatureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidatureService.updateStatut(999L, CandidatureStatut.ACCEPTEE))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(notificationService, never()).notify(any(), any(), any());
    }

    // ---------------------------------------------------------------
    // getHistorique()
    // ---------------------------------------------------------------

    @Test
    void getHistorique_retourneLesEntreesTrieesParDate() {
        Candidature candidature = new Candidature();
        candidature.setId(1L);
        candidature.setMission(mission);
        candidature.setEtudiant(etudiant);
        candidature.setStatut(CandidatureStatut.ACCEPTEE);

        HistoriqueStatut h1 = new HistoriqueStatut();
        h1.setStatut(CandidatureStatut.EN_ATTENTE);
        h1.setDate(LocalDateTime.now().minusDays(2));

        HistoriqueStatut h2 = new HistoriqueStatut();
        h2.setStatut(CandidatureStatut.ACCEPTEE);
        h2.setDate(LocalDateTime.now());

        candidature.setHistorique(List.of(h1, h2));

        when(candidatureRepository.findById(1L)).thenReturn(Optional.of(candidature));

        List<CandidatureStatut> historique = candidatureService.getHistorique(1L);

        assertThat(historique).hasSize(2);
        assertThat(historique).containsExactly(CandidatureStatut.EN_ATTENTE, CandidatureStatut.ACCEPTEE);
    }

    @Test
    void getHistorique_candidatureInexistante_leveResourceNotFound() {
        when(candidatureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidatureService.getHistorique(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
