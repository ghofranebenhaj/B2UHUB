package com.b2uhub.repository;

import com.b2uhub.model.Mission;
import com.b2uhub.model.enums.MissionStatut;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MissionRepository extends JpaRepository<Mission, Long> {

    @EntityGraph(attributePaths = {"entreprise"})
    Optional<Mission> findWithEntrepriseById(Long id);

    @EntityGraph(attributePaths = {"entreprise"})
    List<Mission> findByStatut(MissionStatut statut);

    @Query("""
            SELECT DISTINCT m FROM Mission m
            LEFT JOIN FETCH m.entreprise
            WHERE (:statut IS NULL OR m.statut = :statut)
              AND (:competence IS NULL OR :competence = '' OR :competence MEMBER OF m.competencesRequises)
              AND (:titre IS NULL OR :titre = '' OR LOWER(m.titre) LIKE LOWER(CONCAT('%', :titre, '%')))
            ORDER BY m.datePublication DESC
            """)
    List<Mission> search(
            @Param("statut") MissionStatut statut,
            @Param("competence") String competence,
            @Param("titre") String titre
    );
}
