package com.resumescreening.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "resume_skills", indexes = {
    @Index(name = "idx_resume_skill", columnList = "resume_id,skill_id"),
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ResumeSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    public ResumeSkill(Resume resume, Skill skill) {
        this.resume = resume;
        this.skill = skill;
    }
}
