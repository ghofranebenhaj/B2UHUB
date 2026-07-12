package com.b2uhub.repository;

import com.b2uhub.model.Candidature;
import com.b2uhub.model.enums.CandidatureStatut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CandidatureRepository extends JpaRepository<Candidature, Long> {

    List<Candidature> findByMissionId(Long missionId);

    List<Candidature> findByEtudiantId(Long etudiantId);

    List<Candidature> findByMissionIdAndStatut(Long missionId, CandidatureStatut statut);

    Optional<Candidature> findByMissionIdAndEtudiantId(Long missionId, Long etudiantId);

    boolean existsByMissionIdAndEtudiantId(Long missionId, Long etudiantId);

    long countByMissionId(Long missionId);

    boolean existsByMissionIdAndStatut(Long missionId, CandidatureStatut statut);

    long countByEtudiantIdAndStatutIn(Long etudiantId, List<CandidatureStatut> statuts);
}
