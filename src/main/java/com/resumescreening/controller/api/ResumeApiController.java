package com.resumescreening.controller.api;

import com.resumescreening.dto.response.ApiResponse;
import com.resumescreening.dto.response.ResumeResponse;
import com.resumescreening.service.ResumeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resumes")
@PreAuthorize("hasRole('CANDIDATE')")
public class ResumeApiController {

    private final ResumeService resumeService;

    public ResumeApiController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestParam("file") MultipartFile file,
            Authentication auth) {
        ResumeResponse resume = resumeService.uploadResume(file, auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Resume uploaded successfully", resume));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> getResumes(Authentication auth) {
        List<ResumeResponse> resumes = resumeService.getResumesForUser(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(resumes));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ResumeResponse>> getResume(
            @PathVariable Long id, Authentication auth) {
        ResumeResponse resume = resumeService.getResumeById(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(resume));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @PathVariable Long id, Authentication auth) {
        resumeService.deleteResume(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Resume deleted", null));
    }
}
