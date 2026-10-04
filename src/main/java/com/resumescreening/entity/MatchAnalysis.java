package com.resumescreening.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "match_analyses", indexes = {
    @Index(name = "idx_analysis_resume", columnList = "resume_id"),
    @Index(name = "idx_analysis_job", columnList = "job_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @NoArgsConstructor
public class MatchAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id", nullable = false)
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(nullable = false)
    private double matchScore; // 0 to 100

    @Column(nullable = false)
    private double requiredSkillScore; // component score

    @Column(nullable = false)
    private double preferredSkillScore; // component score

    @Column(nullable = false)
    private double experienceScore; // component score

    // Comma-separated matched skill names (stored as text for simplicity)
    @Column(columnDefinition = "TEXT")
    private String matchedSkills;

    // Comma-separated missing required skill names
    @Column(columnDefinition = "TEXT")
    private String missingSkills;

    // Comma-separated missing preferred skill names
    @Column(columnDefinition = "TEXT")
    private String missingPreferredSkills;

    @Column(columnDefinition = "TEXT")
    private String scoreBreakdown;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime analyzedAt;
}
