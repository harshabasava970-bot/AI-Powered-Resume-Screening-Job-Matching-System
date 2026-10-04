package com.resumescreening.controller;

import com.resumescreening.dto.response.JobResponse;
import com.resumescreening.entity.MatchAnalysis;
import com.resumescreening.entity.RecruiterProfile;
import com.resumescreening.entity.User;
import com.resumescreening.repository.MatchAnalysisRepository;
import com.resumescreening.repository.RecruiterProfileRepository;
import com.resumescreening.security.SecurityUtils;
import com.resumescreening.service.JobService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/recruiter")
@PreAuthorize("hasRole('RECRUITER')")
public class RecruiterDashboardController {

    private final SecurityUtils securityUtils;
    private final JobService jobService;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;

    public RecruiterDashboardController(SecurityUtils securityUtils,
                                         JobService jobService,
                                         RecruiterProfileRepository recruiterProfileRepository,
                                         MatchAnalysisRepository matchAnalysisRepository) {
        this.securityUtils = securityUtils;
        this.jobService = jobService;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.matchAnalysisRepository = matchAnalysisRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        RecruiterProfile profile = recruiterProfileRepository.findByUser(user).orElse(null);

        // Jobs stats
        List<JobResponse> myJobs = jobService.getJobsByRecruiter(auth.getName());
        long activeJobs = myJobs.stream()
                .filter(j -> "ACTIVE".equals(j.getStatus()))
                .count();

        model.addAttribute("totalJobs", myJobs.size());
        model.addAttribute("activeJobs", activeJobs);
        model.addAttribute("recentJobs", myJobs.stream().limit(5).collect(Collectors.toList()));

        // Match stats
        if (profile != null) {
            long totalMatches = matchAnalysisRepository.countByRecruiter(profile);
            Double avgScore = matchAnalysisRepository.avgMatchScoreByRecruiter(profile);
            model.addAttribute("totalMatches", totalMatches);
            model.addAttribute("avgScore", avgScore != null
                    ? Math.round(avgScore * 10.0) / 10.0 : 0.0);

            // Recent candidate matches for this recruiter's jobs
            List<MatchAnalysis> recentMatches = new java.util.ArrayList<>();
            for (JobResponse job : myJobs.stream().limit(3).collect(Collectors.toList())) {
                com.resumescreening.entity.Job jobEntity = jobService.getJobEntityById(job.getId());
                recentMatches.addAll(
                        matchAnalysisRepository.findByJobOrderByScoreDesc(jobEntity)
                                .stream().limit(3).collect(Collectors.toList()));
            }
            recentMatches.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
            model.addAttribute("recentMatches", recentMatches.stream().limit(5).collect(Collectors.toList()));
        } else {
            model.addAttribute("totalMatches", 0);
            model.addAttribute("avgScore", 0.0);
            model.addAttribute("recentMatches", List.of());
        }

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        return "recruiter/dashboard";
    }

    @GetMapping("/jobs/{jobId}/candidates")
    public String viewCandidatesForJob(@PathVariable Long jobId, Model model) {
        com.resumescreening.entity.Job job = jobService.getJobEntityById(jobId);
        List<MatchAnalysis> candidates = matchAnalysisRepository.findByJobOrderByScoreDesc(job);
        model.addAttribute("job", job);
        model.addAttribute("candidates", candidates);
        return "recruiter/job-candidates";
    }
}
