package com.resumescreening.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter @Setter @NoArgsConstructor
public class ProfileUpdateRequest {

    @Size(min = 1, max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @Size(max = 100, message = "Location must not exceed 100 characters")
    private String location;

    @Size(max = 200, message = "Headline must not exceed 200 characters")
    private String headline;

    @Size(max = 1000, message = "Summary must not exceed 1000 characters")
    private String summary;

    @Size(max = 200, message = "LinkedIn URL must not exceed 200 characters")
    private String linkedinUrl;

    @Size(max = 200, message = "GitHub URL must not exceed 200 characters")
    private String githubUrl;

    @Size(max = 200, message = "Portfolio URL must not exceed 200 characters")
    private String portfolioUrl;

    // Recruiter-specific fields
    @Size(max = 100, message = "Company name must not exceed 100 characters")
    private String companyName;

    @Size(max = 100, message = "Job title must not exceed 100 characters")
    private String jobTitle;

    @Size(max = 100, message = "Company website must not exceed 100 characters")
    private String companyWebsite;

    @Size(max = 100, message = "Industry must not exceed 100 characters")
    private String industry;
}
