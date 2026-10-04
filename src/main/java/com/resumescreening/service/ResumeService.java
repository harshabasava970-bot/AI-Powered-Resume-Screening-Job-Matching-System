package com.resumescreening.service;

import com.resumescreening.dto.response.ResumeResponse;
import com.resumescreening.entity.Resume;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ResumeService {

    ResumeResponse uploadResume(MultipartFile file, String username);

    ResumeResponse getResumeById(Long resumeId, String username);

    List<ResumeResponse> getResumesForUser(String username);

    void deleteResume(Long resumeId, String username);

    Resume getResumeEntityById(Long resumeId);

    void reParseResume(Long resumeId, String username);
}
