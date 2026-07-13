package com.b2uhub.model;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "admins")
@PrimaryKeyJoinColumn(name = "utilisateur_id")
public class Admin extends Utilisateur {
}
