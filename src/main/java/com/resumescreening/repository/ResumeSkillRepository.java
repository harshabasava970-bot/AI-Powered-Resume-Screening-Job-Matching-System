package com.resumescreening.repository;

import com.resumescreening.entity.Resume;
import com.resumescreening.entity.ResumeSkill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResumeSkillRepository extends JpaRepository<ResumeSkill, Long> {

    List<ResumeSkill> findByResume(Resume resume);

    void deleteByResume(Resume resume);

    @Query("SELECT rs FROM ResumeSkill rs JOIN FETCH rs.skill WHERE rs.resume.id = :resumeId")
    List<ResumeSkill> findByResumeIdWithSkill(Long resumeId);
}
