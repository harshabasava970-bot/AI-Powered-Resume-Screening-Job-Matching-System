package com.resumescreening.repository;

import com.resumescreening.entity.Job;
import com.resumescreening.entity.JobStatus;
import com.resumescreening.entity.RecruiterProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobRepository extends JpaRepository<Job, Long> {

    List<Job> findByStatus(JobStatus status);

    List<Job> findByRecruiter(RecruiterProfile recruiter);

    List<Job> findByRecruiterAndStatus(RecruiterProfile recruiter, JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' AND " +
           "(LOWER(j.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.companyName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(j.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Job> searchActiveJobs(String keyword);

    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' ORDER BY j.postedAt DESC")
    List<Job> findAllActiveOrderByPostedAtDesc();

    @Query("SELECT COUNT(j) FROM Job j WHERE j.recruiter = :recruiter AND j.status = 'ACTIVE'")
    long countActiveByRecruiter(RecruiterProfile recruiter);

    @Query("SELECT COUNT(j) FROM Job j WHERE j.status = 'ACTIVE'")
    long countAllActive();

    @Query("SELECT j FROM Job j ORDER BY j.postedAt DESC")
    List<Job> findAllOrderByPostedAtDesc();

    @Query("SELECT j FROM Job j WHERE j.status = 'ACTIVE' AND " +
           "(:location IS NULL OR LOWER(j.location) LIKE LOWER(CONCAT('%', :location, '%'))) AND " +
           "(:employmentType IS NULL OR j.employmentType = :employmentType)")
    List<Job> findActiveJobsByFilters(String location, String employmentType);
}
