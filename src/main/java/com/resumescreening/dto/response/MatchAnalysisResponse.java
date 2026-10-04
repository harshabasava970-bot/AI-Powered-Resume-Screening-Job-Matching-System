package com.resumescreening.dto.response;

import com.resumescreening.entity.MatchAnalysis;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter @NoArgsConstructor
public class MatchAnalysisResponse {

    private Long id;
    private Long resumeId;
    private String resumeFileName;
    private Long jobId;
    private String jobTitle;
    private String companyName;
    private double matchScore;
    private double requiredSkillScore;
    private double preferredSkillScore;
    private double experienceScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private List<String> missingPreferredSkills;
    private String scoreBreakdown;
    private LocalDateTime analyzedAt;

    public static MatchAnalysisResponse from(MatchAnalysis analysis) {
        MatchAnalysisResponse response = new MatchAnalysisResponse();
        response.setId(analysis.getId());
        response.setMatchScore(Math.round(analysis.getMatchScore() * 10.0) / 10.0);
        response.setRequiredSkillScore(analysis.getRequiredSkillScore());
        response.setPreferredSkillScore(analysis.getPreferredSkillScore());
        response.setExperienceScore(analysis.getExperienceScore());
        response.setScoreBreakdown(analysis.getScoreBreakdown());
        response.setAnalyzedAt(analysis.getAnalyzedAt());

        if (analysis.getResume() != null) {
            response.setResumeId(analysis.getResume().getId());
            response.setResumeFileName(analysis.getResume().getOriginalFileName());
        }
        if (analysis.getJob() != null) {
            response.setJobId(analysis.getJob().getId());
            response.setJobTitle(analysis.getJob().getTitle());
            response.setCompanyName(analysis.getJob().getCompanyName());
        }

        response.setMatchedSkills(parseSkillList(analysis.getMatchedSkills()));
        response.setMissingSkills(parseSkillList(analysis.getMissingSkills()));
        response.setMissingPreferredSkills(parseSkillList(analysis.getMissingPreferredSkills()));

        return response;
    }

    private static List<String> parseSkillList(String csv) {
        if (csv == null || csv.isBlank()) return Collections.emptyList();
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }
}
