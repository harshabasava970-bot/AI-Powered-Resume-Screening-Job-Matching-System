package com.resumescreening.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class RecommendationResponse {

    private Long jobId;
    private String jobTitle;
    private String companyName;
    private String location;
    private String employmentType;
    private double matchScore;
    private List<String> matchedSkills;
    private List<String> missingSkills;
    private Long analysisId;
}
