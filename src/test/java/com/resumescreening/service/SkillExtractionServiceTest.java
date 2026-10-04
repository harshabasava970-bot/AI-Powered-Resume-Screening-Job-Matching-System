package com.resumescreening.service;

import com.resumescreening.entity.Skill;
import com.resumescreening.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillExtractionService Tests")
class SkillExtractionServiceTest {

    @Mock
    private SkillRepository skillRepository;

    @InjectMocks
    private SkillExtractionService skillExtractionService;

    private List<Skill> mockSkills;

    @BeforeEach
    void setUp() {
        mockSkills = List.of(
            new Skill("Java", "Programming Language"),
            new Skill("Spring Boot", "Framework"),
            new Skill("MySQL", "Database"),
            new Skill("Docker", "DevOps"),
            new Skill("Python", "Programming Language"),
            new Skill("REST API", "API"),
            new Skill("Git", "DevOps"),
            new Skill("Machine Learning", "AI/ML"),
            new Skill("React", "Framework"),
            new Skill("JavaScript", "Programming Language")
        );
    }

    @Test
    @DisplayName("Should extract Java from resume text")
    void shouldExtractJavaFromText() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        String resumeText = "Experienced Java developer with 3 years of experience";
        List<Skill> extracted = skillExtractionService.extractSkills(resumeText);

        assertThat(extracted).isNotEmpty();
        assertThat(extracted.stream().map(Skill::getName))
                .contains("Java");
    }

    @Test
    @DisplayName("Should extract multi-word skills like Spring Boot")
    void shouldExtractMultiWordSkills() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        String resumeText = "Built microservices using Spring Boot and MySQL database";
        List<Skill> extracted = skillExtractionService.extractSkills(resumeText);

        assertThat(extracted.stream().map(Skill::getName))
                .contains("Spring Boot", "MySQL");
    }

    @Test
    @DisplayName("Should perform case-insensitive skill matching")
    void shouldBeCaseInsensitive() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        // Test uppercase
        List<Skill> upper = skillExtractionService.extractSkills("JAVA SPRING BOOT MYSQL");
        // Test lowercase
        List<Skill> lower = skillExtractionService.extractSkills("java spring boot mysql");
        // Test mixed
        List<Skill> mixed = skillExtractionService.extractSkills("Java Spring Boot MySQL");

        assertThat(upper.stream().map(Skill::getName)).contains("Java");
        assertThat(lower.stream().map(Skill::getName)).contains("Spring Boot");
        assertThat(mixed.stream().map(Skill::getName)).contains("MySQL");
    }

    @Test
    @DisplayName("Should return empty list for empty text")
    void shouldReturnEmptyForEmptyText() {
        List<Skill> result = skillExtractionService.extractSkills("");
        assertThat(result).isEmpty();
        verify(skillRepository, never()).findAll();
    }

    @Test
    @DisplayName("Should return empty list for null text")
    void shouldReturnEmptyForNullText() {
        List<Skill> result = skillExtractionService.extractSkills(null);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should not produce duplicate skills")
    void shouldNotProduceDuplicates() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        // Java mentioned multiple times
        String text = "Java developer with Java experience. Loves Java programming.";
        List<Skill> extracted = skillExtractionService.extractSkills(text);

        long javaCount = extracted.stream()
                .filter(s -> s.getName().equals("Java"))
                .count();
        assertThat(javaCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Should extract three-word skill: Machine Learning")
    void shouldExtractThreeWordSkill() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        String text = "Experience with Machine Learning and deep neural networks";
        List<Skill> extracted = skillExtractionService.extractSkills(text);

        assertThat(extracted.stream().map(Skill::getName))
                .contains("Machine Learning");
    }

    @Test
    @DisplayName("Should extract multiple skills from realistic resume text")
    void shouldExtractMultipleSkillsFromRealisticText() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        String resumeText = """
                John Doe
                Senior Software Engineer
                
                Skills: Java, Python, Spring Boot, React, MySQL, Git, Docker, REST API
                
                Experience:
                - Built REST API services using Java and Spring Boot
                - Managed MySQL databases
                - Containerized applications with Docker
                - Used Git for version control
                """;

        List<Skill> extracted = skillExtractionService.extractSkills(resumeText);

        assertThat(extracted).hasSizeGreaterThanOrEqualTo(5);
        assertThat(extracted.stream().map(Skill::getName))
                .contains("Java", "Python", "Spring Boot", "MySQL", "Git");
    }

    @Test
    @DisplayName("extractSkillNames should return list of skill name strings")
    void shouldExtractSkillNamesAsStrings() {
        when(skillRepository.findAll()).thenReturn(mockSkills);

        List<String> names = skillExtractionService.extractSkillNames("Java Spring Boot MySQL");

        assertThat(names).contains("Java", "Spring Boot", "MySQL");
    }

    @Test
    @DisplayName("resolveSkillNames should create skill if not found in DB")
    void shouldCreateSkillIfNotFoundInDb() {
        Skill existingSkill = new Skill("Java", "Programming Language");
        when(skillRepository.findByNormalizedName("java")).thenReturn(Optional.of(existingSkill));
        when(skillRepository.findByNormalizedName("angular")).thenReturn(Optional.empty());
        Skill newSkill = new Skill("Angular", "Framework");
        when(skillRepository.save(any())).thenReturn(newSkill);

        List<Skill> resolved = skillExtractionService.resolveSkillNames(List.of("Java", "Angular"));

        assertThat(resolved).hasSize(2);
        verify(skillRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("resolveSkillNames should return empty for null input")
    void shouldReturnEmptyForNullSkillNames() {
        List<Skill> result = skillExtractionService.resolveSkillNames(null);
        assertThat(result).isEmpty();
    }
}
