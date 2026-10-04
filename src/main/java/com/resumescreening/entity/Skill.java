package com.resumescreening.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "skills", indexes = {
    @Index(name = "idx_skill_name", columnList = "normalizedName")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    // Lowercase version for case-insensitive matching
    @Column(nullable = false, unique = true, length = 100)
    private String normalizedName;

    @Column(length = 50)
    private String category; // e.g., "Programming Language", "Framework", "Soft Skill"

    @PrePersist
    @PreUpdate
    public void normalize() {
        if (name != null) {
            this.normalizedName = name.toLowerCase().trim();
        }
    }

    public Skill(String name, String category) {
        this.name = name;
        this.category = category;
        this.normalizedName = name.toLowerCase().trim();
    }
}
