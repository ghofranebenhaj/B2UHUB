package com.b2uhub.service;

import com.b2uhub.dto.AiMatchResponse;
import com.b2uhub.model.Etudiant;
import com.b2uhub.model.Mission;
import com.b2uhub.repository.EtudiantRepository;
import com.b2uhub.repository.MissionRepository;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private AiServiceClient aiServiceClient;
    @Mock
    private EtudiantRepository etudiantRepository;
    @Mock
    private MissionRepository missionRepository;

    @InjectMocks
    private MatchingService matchingService;

    private Etudiant etudiant;
    private Mission mission;

    @BeforeEach
    void setUp() {
        etudiant = new Etudiant();
        etudiant.setId(1L);
        etudiant.setCompetences(List.of("Java", "Spring"));

        mission = new Mission();
        mission.setId(10L);
        mission.setCompetencesRequises(List.of("Java", "Angular"));
    }

    @Test
    void match_delegueAuClientIA() {
        AiMatchResponse expected = new AiMatchResponse();
        expected.setSimilarity(75.0);
        expected.setMethode("ai");

        when(etudiantRepository.findById(1L)).thenReturn(Optional.of(etudiant));
        when(missionRepository.findById(10L)).thenReturn(Optional.of(mission));
        when(aiServiceClient.match(etudiant, mission)).thenReturn(expected);

        AiMatchResponse result = matchingService.match(1L, 10L);

        assertThat(result.getSimilarity()).isEqualTo(75.0);
        verify(aiServiceClient).match(etudiant, mission);
    }

    @Test
    void match_etudiantInexistant_leveResourceNotFound() {
        when(etudiantRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchingService.match(99L, 10L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void computeLocalSimilarity_utiliseLeClient() {
        when(aiServiceClient.matchingSimilarity(anyList(), anyList())).thenReturn(0.5);

        double similarity = matchingService.computeLocalSimilarity(etudiant, mission);

        assertThat(similarity).isEqualTo(50.0);
        verify(aiServiceClient).matchingSimilarity(anyList(), anyList());
    }
}
