package com.b2uhub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "etudiants")
@PrimaryKeyJoinColumn(name = "utilisateur_id")
@Getter
@Setter
public class Etudiant extends Utilisateur {

    private String filiere;

    private Integer anneeEtude;

    @ElementCollection
    @CollectionTable(name = "etudiant_competences", joinColumns = @JoinColumn(name = "etudiant_id"))
    @Column(name = "competence")
    private List<String> competences = new ArrayList<>();

    private Integer anneesExperience;

    private Integer nombreProjetsRealises;

    private Double performanceAnterieure;

    private Integer scoreSoftSkills;

    private Integer disponibiliteHeuresSemaine;

    @Column(columnDefinition = "TEXT")
    private String cvTexte;

    @OneToMany(mappedBy = "etudiant")
    private List<Candidature> candidatures = new ArrayList<>();
}
