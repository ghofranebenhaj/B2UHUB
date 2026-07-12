package com.b2uhub.model;

import com.b2uhub.model.enums.CandidatureStatut;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "candidatures", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"mission_id", "etudiant_id"})
})
@Getter
@Setter
public class Candidature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CandidatureStatut statut = CandidatureStatut.EN_ATTENTE;

    private Double scoreIA;

    @Column(columnDefinition = "TEXT")
    private String explicationScore;

    @Column(nullable = false, updatable = false)
    private Instant dateCandidature = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "etudiant_id", nullable = false)
    private Etudiant etudiant;
}
