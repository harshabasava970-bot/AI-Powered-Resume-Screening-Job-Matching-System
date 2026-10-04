package com.resumescreening.service.impl;

import com.resumescreening.dto.request.JobRequest;
import com.resumescreening.dto.response.JobResponse;
import com.resumescreening.entity.*;
import com.resumescreening.exception.ResourceNotFoundException;
import com.resumescreening.exception.UnauthorizedException;
import com.resumescreening.repository.*;
import com.resumescreening.service.JobService;
import com.resumescreening.service.SkillExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class JobServiceImpl implements JobService {

    private static final Logger log = LoggerFactory.getLogger(JobServiceImpl.class);

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final UserRepository userRepository;
    private final SkillExtractionService skillExtractionService;

    public JobServiceImpl(JobRepository jobRepository,
                          JobSkillRepository jobSkillRepository,
                          RecruiterProfileRepository recruiterProfileRepository,
                          UserRepository userRepository,
                          SkillExtractionService skillExtractionService) {
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.userRepository = userRepository;
        this.skillExtractionService = skillExtractionService;
    }

    @Override
    public JobResponse createJob(JobRequest request, String username) {
        RecruiterProfile recruiter = getRecruiterProfile(username);

        Job job = new Job();
        mapRequestToJob(request, job);
        job.setRecruiter(recruiter);
        job = jobRepository.save(job);

        saveJobSkills(job, request);
        log.info("Job '{}' created by recruiter {}", job.getTitle(), username);
        return buildResponse(job);
    }

    @Override
    public JobResponse updateJob(Long jobId, JobRequest request, String username) {
        Job job = getOwnedJob(jobId, username);
        mapRequestToJob(request, job);
        job = jobRepository.save(job);

        // Replace all existing skills
        jobSkillRepository.deleteByJob(job);
        saveJobSkills(job, request);

        log.info("Job {} updated by {}", jobId, username);
        return buildResponse(job);
    }

    @Override
    public void deleteJob(Long jobId, String username) {
        Job job = getOwnedJob(jobId, username);
        jobRepository.delete(job);
        log.info("Job {} deleted by {}", jobId, username);
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJobById(Long jobId) {
        Job job = getJobEntityById(jobId);
        return buildResponse(job);
    }

    @Override
    @Transactional(readOnly = true)
    public Job getJobEntityById(Long jobId) {
        return jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getAllActiveJobs() {
        return jobRepository.findAllActiveOrderByPostedAtDesc().stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> searchJobs(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllActiveJobs();
        }
        return jobRepository.searchActiveJobs(keyword.trim()).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getJobsByRecruiter(String username) {
        RecruiterProfile recruiter = getRecruiterProfile(username);
        return jobRepository.findByRecruiter(recruiter).stream()
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void changeJobStatus(Long jobId, String status, String username) {
        Job job = getOwnedJob(jobId, username);
        try {
            job.setStatus(JobStatus.valueOf(status.toUpperCase()));
            jobRepository.save(job);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid job status: " + status);
        }
    }

    // ---- Private helpers ----

    private void mapRequestToJob(JobRequest request, Job job) {
        job.setTitle(request.getTitle());
        job.setCompanyName(request.getCompanyName());
        job.setLocation(request.getLocation());
        job.setExperienceRequired(request.getExperienceRequired());
        job.setDescription(request.getDescription());
        job.setResponsibilities(request.getResponsibilities());
        job.setQualifications(request.getQualifications());
        job.setSalaryRange(request.getSalaryRange());
        job.setApplicationDeadline(request.getApplicationDeadline());

        if (request.getEmploymentType() != null && !request.getEmploymentType().isBlank()) {
            try {
                job.setEmploymentType(EmploymentType.valueOf(request.getEmploymentType().toUpperCase()));
            } catch (IllegalArgumentException e) {
                job.setEmploymentType(null);
            }
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            try {
                job.setStatus(JobStatus.valueOf(request.getStatus().toUpperCase()));
            } catch (IllegalArgumentException e) {
                job.setStatus(JobStatus.ACTIVE);
            }
        } else {
            job.setStatus(JobStatus.ACTIVE);
        }
    }

    private void saveJobSkills(Job job, JobRequest request) {
        // Resolve required skills
        if (request.getRequiredSkills() != null) {
            List<Skill> requiredSkills = skillExtractionService.resolveSkillNames(request.getRequiredSkills());
            requiredSkills.forEach(skill ->
                    jobSkillRepository.save(new JobSkill(job, skill, SkillType.REQUIRED)));
        }

        // Resolve preferred skills
        if (request.getPreferredSkills() != null) {
            List<Skill> preferredSkills = skillExtractionService.resolveSkillNames(request.getPreferredSkills());
            preferredSkills.forEach(skill ->
                    jobSkillRepository.save(new JobSkill(job, skill, SkillType.PREFERRED)));
        }
    }

    private Job getOwnedJob(Long jobId, String username) {
        RecruiterProfile recruiter = getRecruiterProfile(username);
        Job job = getJobEntityById(jobId);
        if (!job.getRecruiter().getId().equals(recruiter.getId())) {
            throw new UnauthorizedException("You do not have permission to modify this job");
        }
        return job;
    }

    private RecruiterProfile getRecruiterProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return recruiterProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Recruiter profile not found"));
    }

    private JobResponse buildResponse(Job job) {
        JobResponse response = JobResponse.from(job);
        // Ensure skills are loaded if collection is empty
        if (job.getJobSkills() == null || job.getJobSkills().isEmpty()) {
            List<JobSkill> skills = jobSkillRepository.findByJobIdWithSkill(job.getId());
            response.setRequiredSkills(skills.stream()
                    .filter(js -> js.getSkillType() == SkillType.REQUIRED)
                    .map(js -> js.getSkill().getName())
                    .collect(Collectors.toList()));
            response.setPreferredSkills(skills.stream()
                    .filter(js -> js.getSkillType() == SkillType.PREFERRED)
                    .map(js -> js.getSkill().getName())
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
