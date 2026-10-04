package com.resumescreening.controller;

import com.resumescreening.dto.response.MatchAnalysisResponse;
import com.resumescreening.dto.response.RecommendationResponse;
import com.resumescreening.dto.response.ResumeResponse;
import com.resumescreening.service.JobService;
import com.resumescreening.service.MatchingService;
import com.resumescreening.service.ResumeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/candidate")
@PreAuthorize("hasRole('CANDIDATE')")
public class MatchingController {

    private final MatchingService matchingService;
    private final ResumeService resumeService;
    private final JobService jobService;

    public MatchingController(MatchingService matchingService,
                              ResumeService resumeService,
                              JobService jobService) {
        this.matchingService = matchingService;
        this.resumeService = resumeService;
        this.jobService = jobService;
    }

    /**
     * Show the analyze page: candidate selects a resume and a job to analyze.
     */
    @GetMapping("/analyze")
    public String analyzePage(Authentication auth, Model model) {
        List<ResumeResponse> resumes = resumeService.getResumesForUser(auth.getName());
        model.addAttribute("resumes", resumes);
        model.addAttribute("jobs", jobService.getAllActiveJobs());
        return "candidate/analyze";
    }

    /**
     * Run analysis for a specific resume+job pair.
     */
    @PostMapping("/analyze")
    public String runAnalysis(@RequestParam Long resumeId,
                              @RequestParam Long jobId,
                              Authentication auth,
                              RedirectAttributes redirectAttributes) {
        try {
            MatchAnalysisResponse result = matchingService.analyzeMatch(resumeId, jobId, auth.getName());
            return "redirect:/candidate/analysis/" + result.getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Analysis failed: " + e.getMessage());
            return "redirect:/candidate/analyze";
        }
    }

    /**
     * Quick analyze directly from a job detail page.
     */
    @GetMapping("/analyze/job/{jobId}")
    public String analyzeFromJob(@PathVariable Long jobId, Authentication auth, Model model) {
        List<ResumeResponse> resumes = resumeService.getResumesForUser(auth.getName());
        model.addAttribute("resumes", resumes);
        model.addAttribute("preselectedJob", jobService.getJobById(jobId));
        model.addAttribute("preselectedJobId", jobId);
        model.addAttribute("jobs", jobService.getAllActiveJobs());
        return "candidate/analyze";
    }

    /**
     * View a specific analysis result.
     */
    @GetMapping("/analysis/{id}")
    public String viewAnalysis(@PathVariable Long id, Authentication auth, Model model) {
        MatchAnalysisResponse analysis = matchingService.getAnalysisById(id, auth.getName());
        model.addAttribute("analysis", analysis);
        model.addAttribute("job", jobService.getJobById(analysis.getJobId()));
        return "candidate/analysis-result";
    }

    /**
     * Analysis history page.
     */
    @GetMapping("/history")
    public String history(Authentication auth, Model model) {
        List<MatchAnalysisResponse> history = matchingService.getAnalysisHistory(auth.getName());
        model.addAttribute("analyses", history);
        model.addAttribute("total", history.size());
        return "candidate/history";
    }

    /**
     * Job recommendations page.
     */
    @GetMapping("/recommendations")
    public String recommendations(Authentication auth, Model model) {
        List<ResumeResponse> resumes = resumeService.getResumesForUser(auth.getName());

        if (resumes.isEmpty()) {
            model.addAttribute("noResume", true);
            model.addAttribute("recommendations", List.of());
            return "candidate/recommendations";
        }

        // Use the most recently uploaded (first active) resume for recommendations
        Long resumeId = resumes.get(0).getId();
        List<RecommendationResponse> recommendations =
                matchingService.getRecommendations(resumeId, auth.getName());

        model.addAttribute("recommendations", recommendations);
        model.addAttribute("resumeId", resumeId);
        model.addAttribute("resumeFileName", resumes.get(0).getOriginalFileName());
        model.addAttribute("noResume", false);
        return "candidate/recommendations";
    }

    /**
     * Recommendations for a specific resume.
     */
    @GetMapping("/recommendations/{resumeId}")
    public String recommendationsForResume(@PathVariable Long resumeId,
                                           Authentication auth, Model model) {
        List<RecommendationResponse> recommendations =
                matchingService.getRecommendations(resumeId, auth.getName());
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("resumeId", resumeId);
        return "candidate/recommendations";
    }
}
