package com.resumescreening.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class JobRequest {

    @NotBlank(message = "Job title is required")
    @Size(min = 3, max = 150, message = "Job title must be between 3 and 150 characters")
    private String title;

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 150, message = "Company name must be between 2 and 150 characters")
    private String companyName;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    private String employmentType;

    @Size(max = 50, message = "Experience field must not exceed 50 characters")
    private String experienceRequired;

    @NotBlank(message = "Job description is required")
    @Size(min = 50, message = "Job description must be at least 50 characters")
    private String description;

    private String responsibilities;

    private String qualifications;

    @Size(max = 100, message = "Salary range must not exceed 100 characters")
    private String salaryRange;

    private LocalDate applicationDeadline;

    private String status;

    @NotEmpty(message = "At least one required skill must be specified")
    private List<String> requiredSkills;

    private List<String> preferredSkills;
}
