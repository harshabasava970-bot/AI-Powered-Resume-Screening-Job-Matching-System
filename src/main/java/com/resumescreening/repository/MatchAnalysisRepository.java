package com.resumescreening.repository;

import com.resumescreening.entity.CandidateProfile;
import com.resumescreening.entity.Job;
import com.resumescreening.entity.MatchAnalysis;
import com.resumescreening.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchAnalysisRepository extends JpaRepository<MatchAnalysis, Long> {

    Optional<MatchAnalysis> findByResumeAndJob(Resume resume, Job job);

    List<MatchAnalysis> findByResume(Resume resume);

    @Query("SELECT ma FROM MatchAnalysis ma WHERE ma.resume.candidateProfile = :profile ORDER BY ma.analyzedAt DESC")
    List<MatchAnalysis> findByCandidate(CandidateProfile profile);

    @Query("SELECT ma FROM MatchAnalysis ma WHERE ma.resume.candidateProfile = :profile ORDER BY ma.analyzedAt DESC")
    List<MatchAnalysis> findByCandidateOrderByDateDesc(CandidateProfile profile);

    @Query("SELECT ma FROM MatchAnalysis ma WHERE ma.job = :job ORDER BY ma.matchScore DESC")
    List<MatchAnalysis> findByJobOrderByScoreDesc(Job job);

    @Query("SELECT COUNT(ma) FROM MatchAnalysis ma WHERE ma.resume.candidateProfile = :profile")
    long countByCandidate(CandidateProfile profile);

    @Query("SELECT AVG(ma.matchScore) FROM MatchAnalysis ma WHERE ma.resume.candidateProfile = :profile")
    Double avgMatchScoreByCandidate(CandidateProfile profile);

    @Query("SELECT ma FROM MatchAnalysis ma WHERE ma.resume.candidateProfile = :profile ORDER BY ma.matchScore DESC")
    List<MatchAnalysis> findByCandidateOrderByScoreDesc(CandidateProfile profile);

    @Query("SELECT COUNT(ma) FROM MatchAnalysis ma WHERE ma.job.recruiter = :recruiter")
    long countByRecruiter(com.resumescreening.entity.RecruiterProfile recruiter);

    @Query("SELECT AVG(ma.matchScore) FROM MatchAnalysis ma WHERE ma.job.recruiter = :recruiter")
    Double avgMatchScoreByRecruiter(com.resumescreening.entity.RecruiterProfile recruiter);

    @Query("SELECT COUNT(ma) FROM MatchAnalysis ma")
    long countAll();
}
