package com.resumescreening.service.impl;

import com.resumescreening.dto.response.MatchAnalysisResponse;
import com.resumescreening.dto.response.RecommendationResponse;
import com.resumescreening.entity.*;
import com.resumescreening.exception.ResourceNotFoundException;
import com.resumescreening.exception.UnauthorizedException;
import com.resumescreening.repository.*;
import com.resumescreening.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core AI/Matching Engine
 *
 * Scoring formula (weights are configurable via application.properties):
 *   Required Skill Score  (default 60%) = matched required skills / total required skills
 *   Preferred Skill Score (default 20%) = matched preferred skills / total preferred skills
 *   Experience Score      (default 10%) = heuristic based on years mentioned in resume text
 *   Education Score       (default 10%) = heuristic based on degree keywords in resume text
 *
 * Final Score = (reqScore * reqWeight + prefScore * prefWeight +
 *                expScore * expWeight + eduScore * eduWeight) * 100
 *
 * The algorithm is deterministic: same inputs always produce the same score.
 * Every component is transparent and explainable.
 */
@Service
@Transactional
public class MatchingServiceImpl implements MatchingService {

    private static final Logger log = LoggerFactory.getLogger(MatchingServiceImpl.class);

    @Value("${app.matching.weight.required-skills:0.60}")
    private double requiredSkillWeight;

    @Value("${app.matching.weight.preferred-skills:0.20}")
    private double preferredSkillWeight;

    @Value("${app.matching.weight.experience:0.10}")
    private double experienceWeight;

    @Value("${app.matching.weight.education:0.10}")
    private double educationWeight;

    @Value("${app.matching.min-recommendation-score:20.0}")
    private double minRecommendationScore;

    private final ResumeRepository resumeRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final UserRepository userRepository;

    public MatchingServiceImpl(ResumeRepository resumeRepository,
                               JobRepository jobRepository,
                               JobSkillRepository jobSkillRepository,
                               ResumeSkillRepository resumeSkillRepository,
                               MatchAnalysisRepository matchAnalysisRepository,
                               CandidateProfileRepository candidateProfileRepository,
                               UserRepository userRepository) {
        this.resumeRepository = resumeRepository;
        this.jobRepository = jobRepository;
        this.jobSkillRepository = jobSkillRepository;
        this.resumeSkillRepository = resumeSkillRepository;
        this.matchAnalysisRepository = matchAnalysisRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.userRepository = userRepository;
    }

    @Override
    public MatchAnalysisResponse analyzeMatch(Long resumeId, Long jobId, String username) {
        Resume resume = getOwnedResume(resumeId, username);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Job", jobId));

        // Perform the actual analysis computation
        MatchAnalysis analysis = computeAndPersist(resume, job);
        return MatchAnalysisResponse.from(analysis);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendations(Long resumeId, String username) {
        Resume resume = getOwnedResume(resumeId, username);

        // Get all active jobs
        List<Job> activeJobs = jobRepository.findAllActiveOrderByPostedAtDesc();

        // Score each job against the resume — retrieve existing analyses or recompute
        List<RecommendationResponse> recommendations = new ArrayList<>();

        for (Job job : activeJobs) {
            // Look for existing analysis (avoid recomputing unnecessarily)
            Optional<MatchAnalysis> existing = matchAnalysisRepository.findByResumeAndJob(resume, job);
            MatchAnalysis analysis = existing.orElseGet(() -> computeAndPersist(resume, job));

            if (analysis.getMatchScore() >= minRecommendationScore) {
                RecommendationResponse rec = buildRecommendation(analysis, job);
                recommendations.add(rec);
            }
        }

        // Sort by score descending
        recommendations.sort((a, b) -> Double.compare(b.getMatchScore(), a.getMatchScore()));
        return recommendations;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MatchAnalysisResponse> getAnalysisHistory(String username) {
        CandidateProfile profile = getCandidateProfile(username);
        return matchAnalysisRepository.findByCandidateOrderByDateDesc(profile).stream()
                .map(MatchAnalysisResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MatchAnalysisResponse getAnalysisById(Long analysisId, String username) {
        MatchAnalysis analysis = matchAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResourceNotFoundException("Analysis", analysisId));

        // Verify the analysis belongs to this user
        CandidateProfile profile = getCandidateProfile(username);
        if (!analysis.getResume().getCandidateProfile().getId().equals(profile.getId())) {
            throw new UnauthorizedException("Access denied to this analysis");
        }
        return MatchAnalysisResponse.from(analysis);
    }

    // ============================================================
    //  Core Matching Algorithm
    // ============================================================

    /**
     * Compute a match score between a resume and a job, persist it, and return.
     * If an analysis already exists for this resume+job pair, it is updated.
     */
    private MatchAnalysis computeAndPersist(Resume resume, Job job) {
        // Load resume skills (normalized names for comparison)
        List<ResumeSkill> resumeSkills = resumeSkillRepository.findByResumeIdWithSkill(resume.getId());
        Set<String> candidateSkillNames = resumeSkills.stream()
                .map(rs -> rs.getSkill().getNormalizedName())
                .collect(Collectors.toSet());

        // Load job skills
        List<JobSkill> jobSkills = jobSkillRepository.findByJobIdWithSkill(job.getId());
        List<JobSkill> requiredSkills = jobSkills.stream()
                .filter(js -> js.getSkillType() == SkillType.REQUIRED)
                .collect(Collectors.toList());
        List<JobSkill> preferredSkills = jobSkills.stream()
                .filter(js -> js.getSkillType() == SkillType.PREFERRED)
                .collect(Collectors.toList());

        // --- Required Skill Score ---
        List<String> matchedRequired = new ArrayList<>();
        List<String> missingRequired = new ArrayList<>();
        for (JobSkill js : requiredSkills) {
            if (candidateSkillNames.contains(js.getSkill().getNormalizedName())) {
                matchedRequired.add(js.getSkill().getName());
            } else {
                missingRequired.add(js.getSkill().getName());
            }
        }
        double reqScore = requiredSkills.isEmpty() ? 1.0 :
                (double) matchedRequired.size() / requiredSkills.size();

        // --- Preferred Skill Score ---
        List<String> matchedPreferred = new ArrayList<>();
        List<String> missingPreferred = new ArrayList<>();
        for (JobSkill js : preferredSkills) {
            if (candidateSkillNames.contains(js.getSkill().getNormalizedName())) {
                matchedPreferred.add(js.getSkill().getName());
            } else {
                missingPreferred.add(js.getSkill().getName());
            }
        }
        double prefScore = preferredSkills.isEmpty() ? 1.0 :
                (double) matchedPreferred.size() / preferredSkills.size();

        // --- Experience Score ---
        double expScore = computeExperienceScore(resume.getExtractedText(), job.getExperienceRequired());

        // --- Education Score ---
        double eduScore = computeEducationScore(resume.getExtractedText());

        // --- Final weighted score ---
        double finalScore = (reqScore * requiredSkillWeight
                + prefScore * preferredSkillWeight
                + expScore * experienceWeight
                + eduScore * educationWeight) * 100.0;

        // Clamp to [0, 100]
        finalScore = Math.min(100.0, Math.max(0.0, finalScore));

        // Build breakdown explanation
        String breakdown = buildBreakdown(reqScore, prefScore, expScore, eduScore,
                matchedRequired.size(), requiredSkills.size(),
                matchedPreferred.size(), preferredSkills.size(),
                finalScore);

        // Merge all matched skills
        List<String> allMatched = new ArrayList<>(matchedRequired);
        allMatched.addAll(matchedPreferred);

        // Persist or update
        MatchAnalysis analysis = matchAnalysisRepository.findByResumeAndJob(resume, job)
                .orElse(new MatchAnalysis());

        analysis.setResume(resume);
        analysis.setJob(job);
        analysis.setMatchScore(finalScore);
        analysis.setRequiredSkillScore(reqScore * 100);
        analysis.setPreferredSkillScore(prefScore * 100);
        analysis.setExperienceScore(expScore * 100);
        analysis.setMatchedSkills(String.join(", ", allMatched));
        analysis.setMissingSkills(String.join(", ", missingRequired));
        analysis.setMissingPreferredSkills(String.join(", ", missingPreferred));
        analysis.setScoreBreakdown(breakdown);

        analysis = matchAnalysisRepository.save(analysis);
        log.debug("Match analysis: resume={} job={} score={:.1f}",
                resume.getId(), job.getId(), finalScore);
        return analysis;
    }

    /**
     * Heuristic: extract years of experience from resume text and compare with job requirement.
     * Returns a score between 0.0 and 1.0.
     */
    private double computeExperienceScore(String resumeText, String jobExpRequired) {
        if (resumeText == null || resumeText.isBlank()) return 0.5; // neutral

        // Extract mentioned years from resume
        double resumeYears = extractYearsOfExperience(resumeText);

        // Parse the job requirement
        double requiredYears = parseRequiredExperience(jobExpRequired);

        if (requiredYears <= 0) return 0.7; // no requirement = good for most candidates

        if (resumeYears <= 0) return 0.3; // couldn't detect experience in resume

        if (resumeYears >= requiredYears) return 1.0;
        if (resumeYears >= requiredYears * 0.5) return 0.6;
        return 0.3;
    }

    private double extractYearsOfExperience(String text) {
        // Look for patterns like "3 years", "2+ years", "over 5 years"
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(\\d+)\\+?\\s*(?:year|yr)s?\\s*(?:of)?\\s*(?:experience|exp)",
                java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(text);
        double max = 0;
        while (m.find()) {
            double y = Double.parseDouble(m.group(1));
            if (y > max) max = y;
        }
        return max;
    }

    private double parseRequiredExperience(String expRequired) {
        if (expRequired == null || expRequired.isBlank()) return 0;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+)");
        java.util.regex.Matcher m = p.matcher(expRequired);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 0;
    }

    /**
     * Heuristic: check if resume text mentions a degree — returns score 0.0–1.0.
     */
    private double computeEducationScore(String resumeText) {
        if (resumeText == null || resumeText.isBlank()) return 0.5;

        String lower = resumeText.toLowerCase();
        if (lower.contains("master") || lower.contains("m.sc") || lower.contains("m.tech")
                || lower.contains("mba") || lower.contains("phd") || lower.contains("doctorate")) {
            return 1.0;
        }
        if (lower.contains("bachelor") || lower.contains("b.sc") || lower.contains("b.tech")
                || lower.contains("b.e") || lower.contains("b.e.") || lower.contains("degree")
                || lower.contains("engineering") || lower.contains("computer science")) {
            return 0.85;
        }
        if (lower.contains("diploma") || lower.contains("associate")) {
            return 0.6;
        }
        if (lower.contains("certification") || lower.contains("certified") || lower.contains("course")) {
            return 0.4;
        }
        return 0.3; // no recognized education detected
    }

    private String buildBreakdown(double reqScore, double prefScore, double expScore, double eduScore,
                                   int matchedReq, int totalReq, int matchedPref, int totalPref,
                                   double finalScore) {
        return String.format(
                "Required Skills: %d/%d matched (%.0f%%, weight %.0f%%) | " +
                "Preferred Skills: %d/%d matched (%.0f%%, weight %.0f%%) | " +
                "Experience Score: %.0f%% (weight %.0f%%) | " +
                "Education Score: %.0f%% (weight %.0f%%) | " +
                "Final Score: %.1f%%",
                matchedReq, totalReq, reqScore * 100, requiredSkillWeight * 100,
                matchedPref, totalPref, prefScore * 100, preferredSkillWeight * 100,
                expScore * 100, experienceWeight * 100,
                eduScore * 100, educationWeight * 100,
                finalScore);
    }

    private RecommendationResponse buildRecommendation(MatchAnalysis analysis, Job job) {
        RecommendationResponse rec = new RecommendationResponse();
        rec.setJobId(job.getId());
        rec.setJobTitle(job.getTitle());
        rec.setCompanyName(job.getCompanyName());
        rec.setLocation(job.getLocation());
        rec.setEmploymentType(job.getEmploymentType() != null ? job.getEmploymentType().name() : null);
        rec.setMatchScore(Math.round(analysis.getMatchScore() * 10.0) / 10.0);
        rec.setAnalysisId(analysis.getId());

        rec.setMatchedSkills(analysis.getMatchedSkills() != null
                ? Arrays.asList(analysis.getMatchedSkills().split(",\\s*"))
                : Collections.emptyList());
        rec.setMissingSkills(analysis.getMissingSkills() != null
                ? Arrays.asList(analysis.getMissingSkills().split(",\\s*"))
                : Collections.emptyList());
        return rec;
    }

    private Resume getOwnedResume(Long resumeId, String username) {
        CandidateProfile profile = getCandidateProfile(username);
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
        if (!resume.getCandidateProfile().getId().equals(profile.getId())) {
            throw new UnauthorizedException("You do not have access to this resume");
        }
        return resume;
    }

    private CandidateProfile getCandidateProfile(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));
    }
}
