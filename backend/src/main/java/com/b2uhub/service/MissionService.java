package com.b2uhub.service;

import com.b2uhub.dto.MissionRequest;
import com.b2uhub.dto.MissionResponse;
import com.b2uhub.model.Entreprise;
import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.CandidatureStatut;
import com.b2uhub.model.enums.MissionStatut;
import com.b2uhub.repository.CandidatureRepository;
import com.b2uhub.repository.EntrepriseRepository;
import com.b2uhub.repository.MissionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class MissionService {

    private final MissionRepository missionRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final CandidatureRepository candidatureRepository;

    public MissionService(
            MissionRepository missionRepository,
            EntrepriseRepository entrepriseRepository,
            CandidatureRepository candidatureRepository
    ) {
        this.missionRepository = missionRepository;
        this.entrepriseRepository = entrepriseRepository;
        this.candidatureRepository = candidatureRepository;
    }

    public List<MissionResponse> findAll(MissionStatut statut, String competence, String titre) {
        return missionRepository.search(statut, competence, titre).stream()
                .map(MissionResponse::from)
                .toList();
    }

    public MissionResponse findById(Long id) {
        return MissionResponse.from(getMission(id));
    }

    public MissionResponse create(MissionRequest request) {
        Mission mission = new Mission();
        applyRequest(mission, request);
        if (mission.getStatut() == MissionStatut.CLOTUREE) {
            throw new BadRequestException(
                    "Impossible de clôturer la mission : aucune candidature acceptée."
            );
        }
        return MissionResponse.from(missionRepository.save(mission));
    }

    private void applyRequest(Mission mission, MissionRequest request) {
        Entreprise entreprise = entrepriseRepository.findById(request.getEntrepriseId())
                .orElseThrow(() -> new ResourceNotFoundException("Entreprise introuvable: " + request.getEntrepriseId()));

        MissionStatut nouveauStatut = request.getStatut() != null ? request.getStatut() : MissionStatut.OUVERTE;
        if (nouveauStatut == MissionStatut.CLOTUREE && mission.getStatut() != MissionStatut.CLOTUREE) {
            validateMissionCanBeClosed(mission.getId());
        }

        mission.setTitre(request.getTitre());
        mission.setDescription(request.getDescription());
        mission.setCompetencesRequises(request.getCompetencesRequises() != null ? request.getCompetencesRequises() : List.of());
        mission.setDureeSemaines(request.getDureeSemaines());
        mission.setStatut(nouveauStatut);
        mission.setEntreprise(entreprise);
    }

    public MissionResponse update(Long id, MissionRequest request) {
        Mission mission = getMission(id);
        applyRequest(mission, request);
        return MissionResponse.from(missionRepository.save(mission));
    }

    public MissionResponse updateStatut(Long id, MissionStatut statut) {
        Mission mission = getMission(id);
        if (statut == MissionStatut.CLOTUREE && mission.getStatut() != MissionStatut.CLOTUREE) {
            validateMissionCanBeClosed(id);
        }
        mission.setStatut(statut);
        return MissionResponse.from(mission);
    }

    private void validateMissionCanBeClosed(Long missionId) {
        if (missionId == null) {
            throw new BadRequestException(
                    "Impossible de clôturer la mission : aucune candidature acceptée."
            );
        }
        if (!candidatureRepository.existsByMissionIdAndStatut(missionId, CandidatureStatut.ACCEPTEE)) {
            throw new BadRequestException(
                    "Impossible de clôturer la mission : aucune candidature acceptée."
            );
        }
    }

    public void delete(Long id) {
        Mission mission = getMission(id);
        if (mission.getEquipe() != null) {
            mission.setEquipe(null);
        }
        mission.getCandidatures().clear();
        missionRepository.delete(mission);
    }

    Mission getMission(Long id) {
        return missionRepository.findWithEntrepriseById(id)
                .or(() -> missionRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("Mission introuvable: " + id));
    }
}
