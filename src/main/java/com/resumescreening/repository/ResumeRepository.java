package com.resumescreening.repository;

import com.resumescreening.entity.CandidateProfile;
import com.resumescreening.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    List<Resume> findByCandidateProfile(CandidateProfile candidateProfile);

    List<Resume> findByCandidateProfileAndIsActive(CandidateProfile candidateProfile, boolean isActive);

    Optional<Resume> findByIdAndCandidateProfile(Long id, CandidateProfile candidateProfile);

    @Query("SELECT r FROM Resume r WHERE r.candidateProfile = :profile AND r.isActive = true ORDER BY r.uploadedAt DESC")
    List<Resume> findActiveResumesByProfile(CandidateProfile profile);

    @Query("SELECT COUNT(r) FROM Resume r WHERE r.candidateProfile.id = :profileId AND r.isActive = true")
    long countActiveByProfileId(Long profileId);
}
