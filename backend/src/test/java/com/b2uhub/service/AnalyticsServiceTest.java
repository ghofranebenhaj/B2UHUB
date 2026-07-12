package com.b2uhub.service;

import com.b2uhub.dto.AnalyticsSummaryResponse;
import com.b2uhub.model.Candidature;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.model.enums.MissionStatut;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.MissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private MissionRepository missionRepository;
    @Mock
    private CandidatureRepository candidatureRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @Test
    void getSummary_calculeLesCompteursEtLaMoyenneDesScores() {
        Mission missionOuverte1 = new Mission();
        Mission missionOuverte2 = new Mission();
        Mission missionEnCours = new Mission();

        when(missionRepository.findByStatut(MissionStatut.OUVERTE))
                .thenReturn(List.of(missionOuverte1, missionOuverte2));
        when(missionRepository.findByStatut(MissionStatut.EN_COURS))
                .thenReturn(List.of(missionEnCours));

        Candidature c1 = new Candidature();
        c1.setStatut(CandidatureStatut.EN_ATTENTE);
        c1.setScoreIA(60.0);

        Candidature c2 = new Candidature();
        c2.setStatut(CandidatureStatut.ACCEPTEE);
        c2.setScoreIA(80.0);

        Candidature c3 = new Candidature();
        c3.setStatut(CandidatureStatut.ACCEPTEE);
        c3.setScoreIA(null); // score manquant : ne doit pas casser la moyenne

        when(candidatureRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        AnalyticsSummaryResponse summary = analyticsService.getSummary();

        assertThat(summary.getMissionsOuvertes()).isEqualTo(2);
        assertThat(summary.getMissionsEnCours()).isEqualTo(1);
        assertThat(summary.getCandidaturesEnAttente()).isEqualTo(1);
        assertThat(summary.getCandidaturesAcceptees()).isEqualTo(2);
        assertThat(summary.getScoreMoyen()).isEqualTo(70.0); // moyenne de 60 et 80, le null est ignoré
    }

    @Test
    void getSummary_aucuneDonnee_retourneDesZeros() {
        when(missionRepository.findByStatut(MissionStatut.OUVERTE)).thenReturn(List.of());
        when(missionRepository.findByStatut(MissionStatut.EN_COURS)).thenReturn(List.of());
        when(candidatureRepository.findAll()).thenReturn(List.of());

        AnalyticsSummaryResponse summary = analyticsService.getSummary();

        assertThat(summary.getMissionsOuvertes()).isZero();
        assertThat(summary.getCandidaturesEnAttente()).isZero();
        assertThat(summary.getScoreMoyen()).isZero();
    }
}
