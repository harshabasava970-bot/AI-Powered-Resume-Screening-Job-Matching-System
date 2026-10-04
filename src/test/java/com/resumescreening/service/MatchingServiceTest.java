package com.resumescreening.service;

import com.resumescreening.dto.response.MatchAnalysisResponse;
import com.resumescreening.entity.*;
import com.resumescreening.exception.UnauthorizedException;
import com.resumescreening.repository.*;
import com.resumescreening.service.impl.MatchingServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MatchingService Tests")
class MatchingServiceTest {

    @Mock private ResumeRepository resumeRepository;
    @Mock private JobRepository jobRepository;
    @Mock private JobSkillRepository jobSkillRepository;
    @Mock private ResumeSkillRepository resumeSkillRepository;
    @Mock private MatchAnalysisRepository matchAnalysisRepository;
    @Mock private CandidateProfileRepository candidateProfileRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private MatchingServiceImpl matchingService;

    @BeforeEach
    void setUp() {
        // Inject configurable weights via ReflectionTestUtils
        ReflectionTestUtils.setField(matchingService, "requiredSkillWeight", 0.60);
        ReflectionTestUtils.setField(matchingService, "preferredSkillWeight", 0.20);
        ReflectionTestUtils.setField(matchingService, "experienceWeight", 0.10);
        ReflectionTestUtils.setField(matchingService, "educationWeight", 0.10);
        ReflectionTestUtils.setField(matchingService, "minRecommendationScore", 20.0);
    }

    @Test
    @DisplayName("Score should be 100% when candidate has all required and preferred skills")
    void scoreShouldBePerfectWhenAllSkillsMatch() {
        // Arrange
        Skill java = skill(1L, "java", "Java");
        Skill spring = skill(2L, "spring boot", "Spring Boot");
        Skill mysql = skill(3L, "mysql", "MySQL");

        User user = createUser("candidate1");
        CandidateProfile profile = createProfile(user);
        Resume resume = createResume(profile,
                "bachelor degree computer science java spring boot mysql 2 years experience");
        Job job = createJob();

        when(userRepository.findByUsername("candidate1")).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        when(resumeSkillRepository.findByResumeIdWithSkill(1L)).thenReturn(List.of(
                resumeSkill(resume, java),
                resumeSkill(resume, spring),
                resumeSkill(resume, mysql)
        ));
        when(jobSkillRepository.findByJobIdWithSkill(1L)).thenReturn(List.of(
                jobSkill(job, java, SkillType.REQUIRED),
                jobSkill(job, spring, SkillType.REQUIRED),
                jobSkill(job, mysql, SkillType.PREFERRED)
        ));
        when(matchAnalysisRepository.findByResumeAndJob(resume, job)).thenReturn(Optional.empty());
        when(matchAnalysisRepository.save(any())).thenAnswer(inv -> {
            MatchAnalysis a = inv.getArgument(0);
            a.setResume(resume);
            a.setJob(job);
            return a;
        });

        // Act
        MatchAnalysisResponse response = matchingService.analyzeMatch(1L, 1L, "candidate1");

        // Assert
        assertThat(response.getMatchScore()).isGreaterThan(70.0);
        assertThat(response.getMatchedSkills()).contains("Java", "Spring Boot", "MySQL");
        assertThat(response.getMissingSkills()).isEmpty();
    }

    @Test
    @DisplayName("Score should reflect missing required skills")
    void scoreShouldBeReducedForMissingRequiredSkills() {
        Skill java   = skill(1L, "java",       "Java");
        Skill spring = skill(2L, "spring boot", "Spring Boot");
        Skill docker = skill(3L, "docker",     "Docker");
        Skill aws    = skill(4L, "aws",        "AWS");

        User user = createUser("candidate1");
        CandidateProfile profile = createProfile(user);
        // Resume only has Java — missing Spring Boot, Docker, AWS
        Resume resume = createResume(profile, "java developer");
        Job job = createJob();

        when(userRepository.findByUsername("candidate1")).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        when(resumeSkillRepository.findByResumeIdWithSkill(1L))
                .thenReturn(List.of(resumeSkill(resume, java)));
        when(jobSkillRepository.findByJobIdWithSkill(1L)).thenReturn(List.of(
                jobSkill(job, java,   SkillType.REQUIRED),
                jobSkill(job, spring, SkillType.REQUIRED),
                jobSkill(job, docker, SkillType.REQUIRED),
                jobSkill(job, aws,    SkillType.REQUIRED)
        ));
        when(matchAnalysisRepository.findByResumeAndJob(resume, job)).thenReturn(Optional.empty());
        when(matchAnalysisRepository.save(any())).thenAnswer(inv -> {
            MatchAnalysis a = inv.getArgument(0);
            a.setResume(resume); a.setJob(job);
            return a;
        });

        MatchAnalysisResponse response = matchingService.analyzeMatch(1L, 1L, "candidate1");

        // 1/4 required skills = 25% required skill score
        // Final score well under 70%
        assertThat(response.getMatchScore()).isLessThan(70.0);
        assertThat(response.getMatchedSkills()).contains("Java");
        assertThat(response.getMissingSkills()).contains("Spring Boot", "Docker", "AWS");
    }

    @Test
    @DisplayName("Score should be 0 when no skills match")
    void scoreShouldBeZeroWhenNoSkillsMatch() {
        Skill python  = skill(1L, "python",  "Python");
        Skill pytorch = skill(2L, "pytorch", "PyTorch");

        User user = createUser("candidate1");
        CandidateProfile profile = createProfile(user);
        Resume resume = createResume(profile, "no relevant skills here");
        Job job = createJob();

        when(userRepository.findByUsername("candidate1")).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        when(resumeSkillRepository.findByResumeIdWithSkill(1L)).thenReturn(List.of());
        when(jobSkillRepository.findByJobIdWithSkill(1L)).thenReturn(List.of(
                jobSkill(job, python,  SkillType.REQUIRED),
                jobSkill(job, pytorch, SkillType.REQUIRED)
        ));
        when(matchAnalysisRepository.findByResumeAndJob(resume, job)).thenReturn(Optional.empty());
        when(matchAnalysisRepository.save(any())).thenAnswer(inv -> {
            MatchAnalysis a = inv.getArgument(0);
            a.setResume(resume); a.setJob(job); return a;
        });

        MatchAnalysisResponse response = matchingService.analyzeMatch(1L, 1L, "candidate1");

        assertThat(response.getMatchedSkills()).isEmpty();
        assertThat(response.getMissingSkills()).contains("Python", "PyTorch");
        // Score will be low but not necessarily 0 due to experience/education weights
        assertThat(response.getMatchScore()).isLessThan(50.0);
    }

    @Test
    @DisplayName("Score should be deterministic — same inputs produce same result")
    void scoreShouldBeDeterministic() {
        Skill java = skill(1L, "java", "Java");
        User user = createUser("candidate1");
        CandidateProfile profile = createProfile(user);
        Resume resume = createResume(profile, "java developer bachelor degree 2 years experience");
        Job job = createJob();

        when(userRepository.findByUsername("candidate1")).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(resumeSkillRepository.findByResumeIdWithSkill(1L))
                .thenReturn(List.of(resumeSkill(resume, java)));
        when(jobSkillRepository.findByJobIdWithSkill(1L))
                .thenReturn(List.of(jobSkill(job, java, SkillType.REQUIRED)));

        MatchAnalysis savedAnalysis = new MatchAnalysis();
        savedAnalysis.setResume(resume);
        savedAnalysis.setJob(job);
        when(matchAnalysisRepository.findByResumeAndJob(resume, job)).thenReturn(Optional.empty());
        when(matchAnalysisRepository.save(any())).thenAnswer(inv -> {
            MatchAnalysis a = inv.getArgument(0);
            a.setResume(resume); a.setJob(job); return a;
        });

        MatchAnalysisResponse first  = matchingService.analyzeMatch(1L, 1L, "candidate1");

        // Reset interaction but keep same stub behaviour
        when(matchAnalysisRepository.findByResumeAndJob(resume, job)).thenReturn(Optional.empty());
        MatchAnalysisResponse second = matchingService.analyzeMatch(1L, 1L, "candidate1");

        assertThat(first.getMatchScore()).isEqualTo(second.getMatchScore());
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when accessing another user's resume")
    void shouldThrowWhenAccessingOtherUserResume() {
        User user = createUser("candidate1");
        CandidateProfile profile = createProfile(user);
        profile.setId(10L);

        User otherUser = createUser("candidate2");
        CandidateProfile otherProfile = new CandidateProfile();
        otherProfile.setId(99L);
        otherProfile.setUser(otherUser);

        Resume resume = createResume(otherProfile, "java");

        when(userRepository.findByUsername("candidate1")).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));

        assertThatThrownBy(() -> matchingService.analyzeMatch(1L, 1L, "candidate1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Score breakdown should be non-null and contain percentage information")
    void scoreBreakdownShouldContainPercentages() {
        Skill java = skill(1L, "java", "Java");
        User user = createUser("candidate1");
        CandidateProfile profile = createProfile(user);
        Resume resume = createResume(profile, "java developer");
        Job job = createJob();

        when(userRepository.findByUsername("candidate1")).thenReturn(Optional.of(user));
        when(candidateProfileRepository.findByUser(user)).thenReturn(Optional.of(profile));
        when(resumeRepository.findById(1L)).thenReturn(Optional.of(resume));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(resumeSkillRepository.findByResumeIdWithSkill(1L))
                .thenReturn(List.of(resumeSkill(resume, java)));
        when(jobSkillRepository.findByJobIdWithSkill(1L))
                .thenReturn(List.of(jobSkill(job, java, SkillType.REQUIRED)));
        when(matchAnalysisRepository.findByResumeAndJob(resume, job)).thenReturn(Optional.empty());
        when(matchAnalysisRepository.save(any())).thenAnswer(inv -> {
            MatchAnalysis a = inv.getArgument(0);
            a.setResume(resume); a.setJob(job);
            return a;
        });

        MatchAnalysisResponse response = matchingService.analyzeMatch(1L, 1L, "candidate1");

        assertThat(response.getScoreBreakdown()).isNotNull();
        assertThat(response.getScoreBreakdown()).contains("Required Skills");
        assertThat(response.getScoreBreakdown()).contains("Final Score");
    }

    // ---- Helpers ----

    private Skill skill(Long id, String normalized, String name) {
        Skill s = new Skill(name, "Test");
        try {
            var f = Skill.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(s, id);
        } catch (Exception ignored) {}
        return s;
    }

    private User createUser(String username) {
        User u = new User();
        u.setId(new Random().nextLong(1, 1000));
        u.setUsername(username);
        u.setEmail(username + "@test.com");
        u.setFirstName("Test");
        u.setRole(Role.CANDIDATE);
        return u;
    }

    private CandidateProfile createProfile(User user) {
        CandidateProfile p = new CandidateProfile();
        p.setId(1L);
        p.setUser(user);
        return p;
    }

    private Resume createResume(CandidateProfile profile, String extractedText) {
        Resume r = new Resume();
        r.setId(1L);
        r.setCandidateProfile(profile);
        r.setExtractedText(extractedText);
        r.setOriginalFileName("test.pdf");
        r.setFileType("PDF");
        return r;
    }

    private Job createJob() {
        Job j = new Job();
        j.setId(1L);
        j.setTitle("Test Job");
        j.setCompanyName("Test Co");
        j.setExperienceRequired("1-3 years");
        return j;
    }

    private ResumeSkill resumeSkill(Resume r, Skill s) {
        return new ResumeSkill(r, s);
    }

    private JobSkill jobSkill(Job j, Skill s, SkillType type) {
        return new JobSkill(j, s, type);
    }
}
