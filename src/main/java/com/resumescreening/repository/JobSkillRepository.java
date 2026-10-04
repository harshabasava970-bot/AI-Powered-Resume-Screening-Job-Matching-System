package com.resumescreening.repository;

import com.resumescreening.entity.Job;
import com.resumescreening.entity.JobSkill;
import com.resumescreening.entity.SkillType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobSkillRepository extends JpaRepository<JobSkill, Long> {

    List<JobSkill> findByJob(Job job);

    List<JobSkill> findByJobAndSkillType(Job job, SkillType skillType);

    void deleteByJob(Job job);

    @Query("SELECT js FROM JobSkill js JOIN FETCH js.skill WHERE js.job.id = :jobId")
    List<JobSkill> findByJobIdWithSkill(Long jobId);

    @Query("SELECT js FROM JobSkill js JOIN FETCH js.skill WHERE js.job.id = :jobId AND js.skillType = :skillType")
    List<JobSkill> findByJobIdAndSkillType(Long jobId, SkillType skillType);
}
