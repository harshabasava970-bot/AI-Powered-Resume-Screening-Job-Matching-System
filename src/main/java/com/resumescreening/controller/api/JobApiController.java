package com.resumescreening.controller.api;

import com.resumescreening.dto.request.JobRequest;
import com.resumescreening.dto.response.ApiResponse;
import com.resumescreening.dto.response.JobResponse;
import com.resumescreening.service.JobService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
public class JobApiController {

    private final JobService jobService;

    public JobApiController(JobService jobService) {
        this.jobService = jobService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<JobResponse>>> getJobs(
            @RequestParam(required = false) String search) {
        List<JobResponse> jobs = search != null && !search.isBlank()
                ? jobService.searchJobs(search)
                : jobService.getAllActiveJobs();
        return ResponseEntity.ok(ApiResponse.success(jobs));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobRequest request, Authentication auth) {
        JobResponse job = jobService.createJob(request, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Job created", job));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @PathVariable Long id,
            @Valid @RequestBody JobRequest request,
            Authentication auth) {
        JobResponse job = jobService.updateJob(id, request, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Job updated", job));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @PathVariable Long id, Authentication auth) {
        jobService.deleteJob(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Job deleted", null));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('RECRUITER')")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getMyJobs(Authentication auth) {
        return ResponseEntity.ok(ApiResponse.success(jobService.getJobsByRecruiter(auth.getName())));
    }
}
