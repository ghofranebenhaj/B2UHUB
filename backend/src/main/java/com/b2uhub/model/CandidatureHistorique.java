package com.b2uhub.model;

import com.b2uhub.model.enums.CandidatureStatut;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "candidature_historique")
@Getter
@Setter
public class CandidatureHistorique {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidature_id", nullable = false)
    private Candidature candidature;

    @Enumerated(EnumType.STRING)
    @Column(name = "ancien_statut", nullable = false)
    private CandidatureStatut ancienStatut;

    @Enumerated(EnumType.STRING)
    @Column(name = "nouveau_statut", nullable = false)
    private CandidatureStatut nouveauStatut;

    @Column(name = "date_changement", nullable = false, updatable = false)
    private Instant dateChangement = Instant.now();
}
