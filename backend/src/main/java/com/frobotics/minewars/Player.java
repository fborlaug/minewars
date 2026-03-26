package com.frobotics.minewars;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "player")
public class Player extends PanacheEntity {

    @Column(unique = true, nullable = false, length = 32)
    public String username;

    @Column(nullable = false, length = 72)
    public String passwordHash;

    public int wins;

    public int losses;

    public static Player findByUsername(String username) {
        return find("username", username).firstResult();
    }
}

