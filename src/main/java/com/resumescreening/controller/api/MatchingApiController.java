package com.resumescreening.controller.api;

import com.resumescreening.dto.response.ApiResponse;
import com.resumescreening.dto.response.MatchAnalysisResponse;
import com.resumescreening.dto.response.RecommendationResponse;
import com.resumescreening.service.MatchingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class MatchingApiController {

    private final MatchingService matchingService;

    public MatchingApiController(MatchingService matchingService) {
        this.matchingService = matchingService;
    }

    @PostMapping("/matching/analyze")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<MatchAnalysisResponse>> analyze(
            @RequestParam Long resumeId,
            @RequestParam Long jobId,
            Authentication auth) {
        MatchAnalysisResponse result = matchingService.analyzeMatch(resumeId, jobId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success("Analysis complete", result));
    }

    @GetMapping("/matching/history")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<MatchAnalysisResponse>>> history(Authentication auth) {
        List<MatchAnalysisResponse> history = matchingService.getAnalysisHistory(auth.getName());
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/matching/{id}")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<MatchAnalysisResponse>> getAnalysis(
            @PathVariable Long id, Authentication auth) {
        MatchAnalysisResponse analysis = matchingService.getAnalysisById(id, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(analysis));
    }

    @GetMapping("/recommendations")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<RecommendationResponse>>> recommendations(
            @RequestParam Long resumeId, Authentication auth) {
        List<RecommendationResponse> recs = matchingService.getRecommendations(resumeId, auth.getName());
        return ResponseEntity.ok(ApiResponse.success(recs));
    }
}
