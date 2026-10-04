package com.resumescreening.dto.response;

import com.resumescreening.entity.Job;
import com.resumescreening.entity.SkillType;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter @NoArgsConstructor
public class JobResponse {

    private Long id;
    private String title;
    private String companyName;
    private String location;
    private String employmentType;
    private String experienceRequired;
    private String description;
    private String responsibilities;
    private String qualifications;
    private String salaryRange;
    private LocalDate applicationDeadline;
    private String status;
    private LocalDateTime postedAt;
    private List<String> requiredSkills;
    private List<String> preferredSkills;
    private String recruiterName;
    private Long recruiterId;

    public static JobResponse from(Job job) {
        JobResponse response = new JobResponse();
        response.setId(job.getId());
        response.setTitle(job.getTitle());
        response.setCompanyName(job.getCompanyName());
        response.setLocation(job.getLocation());
        response.setEmploymentType(job.getEmploymentType() != null ? job.getEmploymentType().name() : null);
        response.setExperienceRequired(job.getExperienceRequired());
        response.setDescription(job.getDescription());
        response.setResponsibilities(job.getResponsibilities());
        response.setQualifications(job.getQualifications());
        response.setSalaryRange(job.getSalaryRange());
        response.setApplicationDeadline(job.getApplicationDeadline());
        response.setStatus(job.getStatus().name());
        response.setPostedAt(job.getPostedAt());

        if (job.getRecruiter() != null && job.getRecruiter().getUser() != null) {
            response.setRecruiterName(job.getRecruiter().getUser().getFullName());
            response.setRecruiterId(job.getRecruiter().getId());
        }

        if (job.getJobSkills() != null) {
            response.setRequiredSkills(job.getJobSkills().stream()
                    .filter(js -> js.getSkillType() == SkillType.REQUIRED)
                    .map(js -> js.getSkill().getName())
                    .collect(Collectors.toList()));
            response.setPreferredSkills(job.getJobSkills().stream()
                    .filter(js -> js.getSkillType() == SkillType.PREFERRED)
                    .map(js -> js.getSkill().getName())
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
