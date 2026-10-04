package com.resumescreening.dto.response;

import com.resumescreening.entity.Resume;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter @NoArgsConstructor
public class ResumeResponse {

    private Long id;
    private String originalFileName;
    private String fileType;
    private Long fileSize;
    private String extractedName;
    private String extractedEmail;
    private String extractedPhone;
    private String extractedEducation;
    private String extractedExperience;
    private String extractedProjects;
    private boolean parsed;
    private LocalDateTime uploadedAt;
    private List<String> skills;

    public static ResumeResponse from(Resume resume) {
        ResumeResponse response = new ResumeResponse();
        response.setId(resume.getId());
        response.setOriginalFileName(resume.getOriginalFileName());
        response.setFileType(resume.getFileType());
        response.setFileSize(resume.getFileSize());
        response.setExtractedName(resume.getExtractedName() != null ? resume.getExtractedName() : "Not detected");
        response.setExtractedEmail(resume.getExtractedEmail() != null ? resume.getExtractedEmail() : "Not detected");
        response.setExtractedPhone(resume.getExtractedPhone() != null ? resume.getExtractedPhone() : "Not detected");
        response.setExtractedEducation(resume.getExtractedEducation() != null ? resume.getExtractedEducation() : "Not detected");
        response.setExtractedExperience(resume.getExtractedExperience() != null ? resume.getExtractedExperience() : "Not detected");
        response.setExtractedProjects(resume.getExtractedProjects() != null ? resume.getExtractedProjects() : "Not detected");
        response.setParsed(resume.isParsed());
        response.setUploadedAt(resume.getUploadedAt());

        if (resume.getResumeSkills() != null) {
            response.setSkills(resume.getResumeSkills().stream()
                    .map(rs -> rs.getSkill().getName())
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
