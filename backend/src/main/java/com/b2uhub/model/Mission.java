package com.b2uhub.model;

import com.b2uhub.model.enums.MissionStatut;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "missions", indexes = {
        @Index(name = "idx_mission_statut", columnList = "statut"),
        @Index(name = "idx_mission_entreprise", columnList = "entreprise_id")
})
@Getter
@Setter
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MissionStatut statut = MissionStatut.OUVERTE;

    @ElementCollection
    @CollectionTable(name = "mission_competences", joinColumns = @JoinColumn(name = "mission_id"))
    @Column(name = "competence")
    private List<String> competencesRequises = new ArrayList<>();

    private Integer dureeSemaines;

    @Column(nullable = false, updatable = false)
    private Instant datePublication = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entreprise_id", nullable = false)
    private Entreprise entreprise;

    @OneToMany(mappedBy = "mission", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Candidature> candidatures = new ArrayList<>();

    @OneToOne(mappedBy = "mission", cascade = CascadeType.ALL)
    private Equipe equipe;
}
