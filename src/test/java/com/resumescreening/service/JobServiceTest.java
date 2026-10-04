package com.resumescreening.service;

import com.resumescreening.dto.request.JobRequest;
import com.resumescreening.dto.response.JobResponse;
import com.resumescreening.entity.*;
import com.resumescreening.exception.ResourceNotFoundException;
import com.resumescreening.exception.UnauthorizedException;
import com.resumescreening.repository.*;
import com.resumescreening.service.impl.JobServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JobService Tests")
class JobServiceTest {

    @Mock private JobRepository jobRepository;
    @Mock private JobSkillRepository jobSkillRepository;
    @Mock private RecruiterProfileRepository recruiterProfileRepository;
    @Mock private UserRepository userRepository;
    @Mock private SkillExtractionService skillExtractionService;

    @InjectMocks private JobServiceImpl jobService;

    private User recruiterUser;
    private RecruiterProfile recruiterProfile;
    private JobRequest validJobRequest;

    @BeforeEach
    void setUp() {
        recruiterUser = new User();
        recruiterUser.setId(1L);
        recruiterUser.setUsername("recruiter1");
        recruiterUser.setRole(Role.RECRUITER);

        recruiterProfile = new RecruiterProfile();
        recruiterProfile.setId(1L);
        recruiterProfile.setUser(recruiterUser);

        validJobRequest = new JobRequest();
        validJobRequest.setTitle("Java Backend Developer");
        validJobRequest.setCompanyName("TechCorp");
        validJobRequest.setLocation("Remote");
        validJobRequest.setDescription("We are looking for an experienced Java developer with Spring Boot skills to join our team.");
        validJobRequest.setExperienceRequired("1-3 years");
        validJobRequest.setEmploymentType("FULL_TIME");
        validJobRequest.setRequiredSkills(List.of("Java", "Spring Boot", "MySQL"));
        validJobRequest.setPreferredSkills(List.of("Docker", "AWS"));
    }

    @Test
    @DisplayName("Should create job successfully with skills")
    void shouldCreateJobWithSkills() {
        when(userRepository.findByUsername("recruiter1")).thenReturn(Optional.of(recruiterUser));
        when(recruiterProfileRepository.findByUser(recruiterUser)).thenReturn(Optional.of(recruiterProfile));

        Skill java   = new Skill("Java",       "Programming Language");
        Skill spring = new Skill("Spring Boot", "Framework");
        Skill mysql  = new Skill("MySQL",       "Database");
        when(skillExtractionService.resolveSkillNames(List.of("Java", "Spring Boot", "MySQL")))
                .thenReturn(List.of(java, spring, mysql));
        when(skillExtractionService.resolveSkillNames(List.of("Docker", "AWS")))
                .thenReturn(List.of(new Skill("Docker","DevOps"), new Skill("AWS","Cloud")));

        Job savedJob = new Job();
        savedJob.setId(1L);
        savedJob.setTitle("Java Backend Developer");
        savedJob.setCompanyName("TechCorp");
        savedJob.setRecruiter(recruiterProfile);
        when(jobRepository.save(any(Job.class))).thenReturn(savedJob);
        when(jobSkillRepository.findByJobIdWithSkill(1L)).thenReturn(List.of());

        JobResponse response = jobService.createJob(validJobRequest, "recruiter1");

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Java Backend Developer");
        verify(jobRepository, times(1)).save(any(Job.class));
        verify(jobSkillRepository, atLeast(3)).save(any(JobSkill.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException for non-existent job")
    void shouldThrowForNonExistentJob() {
        when(jobRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jobService.getJobById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Should throw UnauthorizedException when recruiter tries to edit another recruiter's job")
    void shouldThrowWhenEditingAnotherRecruitersJob() {
        RecruiterProfile otherProfile = new RecruiterProfile();
        otherProfile.setId(99L);
        otherProfile.setUser(new User());

        Job job = new Job();
        job.setId(1L);
        job.setRecruiter(otherProfile); // belongs to different recruiter

        when(userRepository.findByUsername("recruiter1")).thenReturn(Optional.of(recruiterUser));
        when(recruiterProfileRepository.findByUser(recruiterUser)).thenReturn(Optional.of(recruiterProfile));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        assertThatThrownBy(() -> jobService.updateJob(1L, validJobRequest, "recruiter1"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Should return all active jobs sorted by date")
    void shouldReturnAllActiveJobs() {
        Job job1 = createJob(1L, "Java Dev");
        Job job2 = createJob(2L, "Python Dev");

        when(jobRepository.findAllActiveOrderByPostedAtDesc()).thenReturn(List.of(job1, job2));
        when(jobSkillRepository.findByJobIdWithSkill(anyLong())).thenReturn(List.of());

        List<JobResponse> jobs = jobService.getAllActiveJobs();

        assertThat(jobs).hasSize(2);
        assertThat(jobs.get(0).getTitle()).isEqualTo("Java Dev");
    }

    @Test
    @DisplayName("Should return jobs filtered by search keyword")
    void shouldSearchJobs() {
        Job job = createJob(1L, "Java Backend Developer");
        when(jobRepository.searchActiveJobs("Java")).thenReturn(List.of(job));
        when(jobSkillRepository.findByJobIdWithSkill(anyLong())).thenReturn(List.of());

        List<JobResponse> results = jobService.searchJobs("Java");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getTitle()).contains("Java");
    }

    @Test
    @DisplayName("Empty search should return all active jobs")
    void emptySearchShouldReturnAllJobs() {
        when(jobRepository.findAllActiveOrderByPostedAtDesc()).thenReturn(List.of());

        List<JobResponse> results = jobService.searchJobs("");

        verify(jobRepository).findAllActiveOrderByPostedAtDesc();
        verify(jobRepository, never()).searchActiveJobs(anyString());
    }

    @Test
    @DisplayName("Should delete job when owned by recruiter")
    void shouldDeleteOwnedJob() {
        Job job = new Job();
        job.setId(1L);
        job.setRecruiter(recruiterProfile);

        when(userRepository.findByUsername("recruiter1")).thenReturn(Optional.of(recruiterUser));
        when(recruiterProfileRepository.findByUser(recruiterUser)).thenReturn(Optional.of(recruiterProfile));
        when(jobRepository.findById(1L)).thenReturn(Optional.of(job));

        jobService.deleteJob(1L, "recruiter1");

        verify(jobRepository, times(1)).delete(job);
    }

    private Job createJob(Long id, String title) {
        Job j = new Job();
        j.setId(id);
        j.setTitle(title);
        j.setCompanyName("TestCo");
        j.setDescription("Test description for the job posting.");
        j.setStatus(JobStatus.ACTIVE);
        j.setRecruiter(recruiterProfile);
        j.setJobSkills(new ArrayList<>());
        return j;
    }
}
