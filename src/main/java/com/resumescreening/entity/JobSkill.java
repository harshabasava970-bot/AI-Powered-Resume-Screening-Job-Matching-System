package com.resumescreening.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "job_skills", indexes = {
    @Index(name = "idx_job_skill", columnList = "job_id,skill_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class JobSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private SkillType skillType = SkillType.REQUIRED;

    public JobSkill(Job job, Skill skill, SkillType skillType) {
        this.job = job;
        this.skill = skill;
        this.skillType = skillType;
    }
}
