package com.resumescreening.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeInfoExtractor Tests")
class ResumeInfoExtractorTest {

    private ResumeInfoExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new ResumeInfoExtractor();
    }

    @Test
    @DisplayName("Should extract email address from resume text")
    void shouldExtractEmail() {
        String text = "John Doe\njohn.doe@example.com\nSoftware Engineer";
        String email = extractor.extractEmail(text);
        assertThat(email).isEqualTo("john.doe@example.com");
    }

    @Test
    @DisplayName("Should extract phone number in standard format")
    void shouldExtractPhone() {
        String text = "Contact: +1-555-123-4567\nJohn Doe";
        String phone = extractor.extractPhone(text);
        assertThat(phone).isNotNull();
        assertThat(phone).contains("555");
    }

    @Test
    @DisplayName("Should return null for text with no email")
    void shouldReturnNullWhenNoEmail() {
        String text = "John Doe\nSoftware Engineer\nNew York";
        String email = extractor.extractEmail(text);
        assertThat(email).isNull();
    }

    @Test
    @DisplayName("Should return null for text with no phone")
    void shouldReturnNullWhenNoPhone() {
        String text = "John Doe\njohn@example.com\nSoftware Engineer";
        String phone = extractor.extractPhone(text);
        assertThat(phone).isNull();
    }

    @Test
    @DisplayName("Should extract name from first line of resume")
    void shouldExtractNameFromFirstLine() {
        String text = "John Smith\njohn.smith@email.com\n+1-555-0100\n\nEducation\nB.Sc Computer Science";
        String name = extractor.extractName(text);
        assertThat(name).isNotNull();
        // Name should be detected from the first line
        assertThat(name).contains("John");
    }

    @Test
    @DisplayName("Should extract education section")
    void shouldExtractEducationSection() {
        String text = """
                John Doe
                john@example.com
                
                Education
                Bachelor of Science in Computer Science
                University of Technology, 2020-2024
                GPA: 3.8
                
                Experience
                Software Engineer at TechCorp
                """;
        Map<String, String> info = extractor.extractAll(text);
        assertThat(info.get("education")).isNotNull();
        assertThat(info.get("education")).contains("Computer Science");
    }

    @Test
    @DisplayName("Should extract experience section")
    void shouldExtractExperienceSection() {
        String text = """
                Jane Smith
                jane@example.com
                
                Skills
                Java, Python
                
                Work Experience
                Java Developer at ABC Corp, 2022-2024
                - Developed REST APIs using Spring Boot
                - Worked with MySQL databases
                
                Education
                B.Tech Computer Science
                """;
        Map<String, String> info = extractor.extractAll(text);
        assertThat(info.get("experience")).isNotNull();
        assertThat(info.get("experience")).contains("ABC Corp");
    }

    @Test
    @DisplayName("Should return null fields for empty text")
    void shouldHandleEmptyText() {
        Map<String, String> info = extractor.extractAll("");
        assertThat(info).isEmpty();
    }

    @Test
    @DisplayName("Should return null for null text")
    void shouldHandleNullText() {
        Map<String, String> info = extractor.extractAll(null);
        assertThat(info).isEmpty();
    }

    @Test
    @DisplayName("Should handle various email formats")
    void shouldHandleVariousEmailFormats() {
        assertThat(extractor.extractEmail("user.name+tag@domain.co.uk")).isNotNull();
        assertThat(extractor.extractEmail("user_name@subdomain.domain.com")).isNotNull();
        assertThat(extractor.extractEmail("no email here")).isNull();
    }

    @Test
    @DisplayName("Should handle various phone formats")
    void shouldHandleVariousPhoneFormats() {
        assertThat(extractor.extractPhone("555-867-5309")).isNotNull();
        assertThat(extractor.extractPhone("(555) 867-5309")).isNotNull();
        assertThat(extractor.extractPhone("+1 555 867 5309")).isNotNull();
    }
}
