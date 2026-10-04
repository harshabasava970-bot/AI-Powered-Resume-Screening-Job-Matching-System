package com.resumescreening.controller;

import com.resumescreening.dto.response.MatchAnalysisResponse;
import com.resumescreening.dto.response.RecommendationResponse;
import com.resumescreening.dto.response.ResumeResponse;
import com.resumescreening.entity.CandidateProfile;
import com.resumescreening.entity.User;
import com.resumescreening.repository.CandidateProfileRepository;
import com.resumescreening.repository.MatchAnalysisRepository;
import com.resumescreening.security.SecurityUtils;
import com.resumescreening.service.MatchingService;
import com.resumescreening.service.ResumeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/candidate")
@PreAuthorize("hasRole('CANDIDATE')")
public class CandidateDashboardController {

    private final SecurityUtils securityUtils;
    private final ResumeService resumeService;
    private final MatchingService matchingService;
    private final CandidateProfileRepository candidateProfileRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;

    public CandidateDashboardController(SecurityUtils securityUtils,
                                         ResumeService resumeService,
                                         MatchingService matchingService,
                                         CandidateProfileRepository candidateProfileRepository,
                                         MatchAnalysisRepository matchAnalysisRepository) {
        this.securityUtils = securityUtils;
        this.resumeService = resumeService;
        this.matchingService = matchingService;
        this.candidateProfileRepository = candidateProfileRepository;
        this.matchAnalysisRepository = matchAnalysisRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication auth, Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        CandidateProfile profile = candidateProfileRepository.findByUser(user).orElse(null);

        // Resumes
        List<ResumeResponse> resumes = resumeService.getResumesForUser(auth.getName());
        model.addAttribute("resumes", resumes);
        model.addAttribute("hasResume", !resumes.isEmpty());

        // Analysis stats
        List<MatchAnalysisResponse> history = matchingService.getAnalysisHistory(auth.getName());
        model.addAttribute("totalAnalyses", history.size());

        double avgScore = history.stream()
                .mapToDouble(MatchAnalysisResponse::getMatchScore)
                .average().orElse(0.0);
        model.addAttribute("avgScore", Math.round(avgScore * 10.0) / 10.0);

        // Best matching job
        history.stream()
                .max(Comparator.comparingDouble(MatchAnalysisResponse::getMatchScore))
                .ifPresent(best -> model.addAttribute("bestMatch", best));

        // Recent 5 analyses
        model.addAttribute("recentAnalyses", history.stream().limit(5).collect(Collectors.toList()));

        // Most frequently missing skills
        Map<String, Long> missingSkillCounts = new LinkedHashMap<>();
        history.forEach(a -> a.getMissingSkills().forEach(skill ->
                missingSkillCounts.merge(skill, 1L, Long::sum)));
        List<Map.Entry<String, Long>> topMissingSkills = missingSkillCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("topMissingSkills", topMissingSkills);

        // Recommendations (top 3)
        if (!resumes.isEmpty()) {
            try {
                List<RecommendationResponse> recommendations =
                        matchingService.getRecommendations(resumes.get(0).getId(), auth.getName());
                model.addAttribute("recommendations", recommendations.stream().limit(3).collect(Collectors.toList()));
            } catch (Exception e) {
                model.addAttribute("recommendations", List.of());
            }
        } else {
            model.addAttribute("recommendations", List.of());
        }

        model.addAttribute("user", user);
        model.addAttribute("profile", profile);
        return "candidate/dashboard";
    }
}
