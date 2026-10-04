package com.resumescreening.service;

import com.resumescreening.dto.request.JobRequest;
import com.resumescreening.dto.response.JobResponse;
import com.resumescreening.entity.Job;

import java.util.List;

public interface JobService {

    JobResponse createJob(JobRequest request, String username);

    JobResponse updateJob(Long jobId, JobRequest request, String username);

    void deleteJob(Long jobId, String username);

    JobResponse getJobById(Long jobId);

    Job getJobEntityById(Long jobId);

    List<JobResponse> getAllActiveJobs();

    List<JobResponse> searchJobs(String keyword);

    List<JobResponse> getJobsByRecruiter(String username);

    void changeJobStatus(Long jobId, String status, String username);
}
