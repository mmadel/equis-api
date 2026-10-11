package com.equestrian.equis.entity.levels;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Immutable;

@Entity
@Table(name = "level")
@Immutable
@Getter
@Setter
public class Level {
    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private Short id;

    @Column(name = "name", nullable = false, unique = true, length = 50)
    private String name;

    @Column(name = "colour", nullable = false, length = 7)
    private String colour;

}
