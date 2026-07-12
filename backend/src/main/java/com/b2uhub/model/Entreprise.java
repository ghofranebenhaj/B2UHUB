package com.b2uhub.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "entreprises")
@PrimaryKeyJoinColumn(name = "utilisateur_id")
@Getter
@Setter
public class Entreprise extends Utilisateur {

    private String secteur;

    private String siteWeb;

    @OneToMany(mappedBy = "entreprise")
    private List<Mission> missions = new ArrayList<>();
}
