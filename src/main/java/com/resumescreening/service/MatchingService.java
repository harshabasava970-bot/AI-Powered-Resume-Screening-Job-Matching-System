package com.resumescreening.service;

import com.resumescreening.dto.response.MatchAnalysisResponse;
import com.resumescreening.dto.response.RecommendationResponse;

import java.util.List;

public interface MatchingService {

    MatchAnalysisResponse analyzeMatch(Long resumeId, Long jobId, String username);

    List<RecommendationResponse> getRecommendations(Long resumeId, String username);

    List<MatchAnalysisResponse> getAnalysisHistory(String username);

    MatchAnalysisResponse getAnalysisById(Long analysisId, String username);
}
