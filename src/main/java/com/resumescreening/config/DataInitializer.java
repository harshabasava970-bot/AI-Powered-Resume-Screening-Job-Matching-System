package com.resumescreening.config;

import com.resumescreening.entity.*;
import com.resumescreening.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    @Transactional
    CommandLineRunner initData(UserRepository userRepo,
                               CandidateProfileRepository candidateProfileRepo,
                               RecruiterProfileRepository recruiterProfileRepo,
                               SkillRepository skillRepo,
                               JobRepository jobRepo,
                               JobSkillRepository jobSkillRepo,
                               PasswordEncoder passwordEncoder) {
        return args -> {
            // Seed skills
            if (skillRepo.count() == 0) {
                seedSkills(skillRepo);
                log.info("Skills seeded successfully");
            }

            // Create admin
            if (!userRepo.existsByUsername("admin")) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setEmail("admin@resumescreening.com");
                admin.setPassword(passwordEncoder.encode("Admin@1234"));
                admin.setFirstName("System");
                admin.setLastName("Admin");
                admin.setRole(Role.ADMIN);
                admin.setEnabled(true);
                userRepo.save(admin);
                log.info("Admin user created");
            }

            // Create demo recruiter
            if (!userRepo.existsByUsername("recruiter1")) {
                User recruiterUser = new User();
                recruiterUser.setUsername("recruiter1");
                recruiterUser.setEmail("recruiter@techcorp.com");
                recruiterUser.setPassword(passwordEncoder.encode("Recruiter@1234"));
                recruiterUser.setFirstName("Sarah");
                recruiterUser.setLastName("Johnson");
                recruiterUser.setRole(Role.RECRUITER);
                recruiterUser.setEnabled(true);
                userRepo.save(recruiterUser);

                RecruiterProfile recruiterProfile = new RecruiterProfile();
                recruiterProfile.setUser(recruiterUser);
                recruiterProfile.setCompanyName("TechCorp Solutions");
                recruiterProfile.setJobTitle("Senior Recruiter");
                recruiterProfile.setIndustry("Software Development");
                recruiterProfile.setCompanySize("500-1000");
                recruiterProfile.setCompanyWebsite("https://techcorp.example.com");
                recruiterProfileRepo.save(recruiterProfile);

                // Create demo jobs
                seedJobs(recruiterProfile, skillRepo, jobRepo, jobSkillRepo);
                log.info("Demo recruiter and jobs created");
            }

            // Create demo candidate
            if (!userRepo.existsByUsername("candidate1")) {
                User candidateUser = new User();
                candidateUser.setUsername("candidate1");
                candidateUser.setEmail("candidate@example.com");
                candidateUser.setPassword(passwordEncoder.encode("Candidate@1234"));
                candidateUser.setFirstName("Alex");
                candidateUser.setLastName("Kumar");
                candidateUser.setRole(Role.CANDIDATE);
                candidateUser.setEnabled(true);
                userRepo.save(candidateUser);

                CandidateProfile candidateProfile = new CandidateProfile();
                candidateProfile.setUser(candidateUser);
                candidateProfile.setPhone("+1-555-0100");
                candidateProfile.setLocation("San Francisco, CA");
                candidateProfile.setHeadline("Full Stack Java Developer");
                candidateProfile.setSummary("Passionate software engineer with 2+ years of experience in Java and Spring Boot.");
                candidateProfileRepo.save(candidateProfile);

                log.info("Demo candidate created");
            }

            log.info("Data initialization complete");
        };
    }

    private void seedSkills(SkillRepository skillRepo) {
        List<String[]> skills = List.of(
            // Programming Languages
            new String[]{"Java", "Programming Language"},
            new String[]{"Python", "Programming Language"},
            new String[]{"C", "Programming Language"},
            new String[]{"C++", "Programming Language"},
            new String[]{"JavaScript", "Programming Language"},
            new String[]{"TypeScript", "Programming Language"},
            new String[]{"Go", "Programming Language"},
            new String[]{"Rust", "Programming Language"},
            new String[]{"Kotlin", "Programming Language"},
            new String[]{"Swift", "Programming Language"},
            new String[]{"PHP", "Programming Language"},
            new String[]{"Ruby", "Programming Language"},
            new String[]{"Scala", "Programming Language"},
            new String[]{"R", "Programming Language"},
            // Web Technologies
            new String[]{"HTML", "Web Technology"},
            new String[]{"CSS", "Web Technology"},
            new String[]{"HTML5", "Web Technology"},
            new String[]{"CSS3", "Web Technology"},
            new String[]{"Bootstrap", "Web Technology"},
            new String[]{"Tailwind CSS", "Web Technology"},
            // Frameworks
            new String[]{"Spring", "Framework"},
            new String[]{"Spring Boot", "Framework"},
            new String[]{"Spring MVC", "Framework"},
            new String[]{"Spring Security", "Framework"},
            new String[]{"Hibernate", "Framework"},
            new String[]{"JPA", "Framework"},
            new String[]{"React", "Framework"},
            new String[]{"Angular", "Framework"},
            new String[]{"Vue.js", "Framework"},
            new String[]{"Node.js", "Framework"},
            new String[]{"Express.js", "Framework"},
            new String[]{"Django", "Framework"},
            new String[]{"Flask", "Framework"},
            new String[]{"FastAPI", "Framework"},
            // Databases
            new String[]{"SQL", "Database"},
            new String[]{"MySQL", "Database"},
            new String[]{"PostgreSQL", "Database"},
            new String[]{"Oracle", "Database"},
            new String[]{"MongoDB", "Database"},
            new String[]{"Redis", "Database"},
            new String[]{"Elasticsearch", "Database"},
            new String[]{"Cassandra", "Database"},
            new String[]{"SQLite", "Database"},
            // APIs
            new String[]{"REST API", "API"},
            new String[]{"RESTful", "API"},
            new String[]{"GraphQL", "API"},
            new String[]{"gRPC", "API"},
            new String[]{"SOAP", "API"},
            // DevOps & Tools
            new String[]{"Git", "DevOps"},
            new String[]{"GitHub", "DevOps"},
            new String[]{"GitLab", "DevOps"},
            new String[]{"Docker", "DevOps"},
            new String[]{"Kubernetes", "DevOps"},
            new String[]{"Jenkins", "DevOps"},
            new String[]{"Maven", "DevOps"},
            new String[]{"Gradle", "DevOps"},
            new String[]{"Linux", "DevOps"},
            new String[]{"Bash", "DevOps"},
            // Cloud
            new String[]{"AWS", "Cloud"},
            new String[]{"Azure", "Cloud"},
            new String[]{"GCP", "Cloud"},
            new String[]{"Heroku", "Cloud"},
            new String[]{"Vercel", "Cloud"},
            // AI/ML
            new String[]{"Machine Learning", "AI/ML"},
            new String[]{"Deep Learning", "AI/ML"},
            new String[]{"NLP", "AI/ML"},
            new String[]{"TensorFlow", "AI/ML"},
            new String[]{"PyTorch", "AI/ML"},
            new String[]{"Scikit-learn", "AI/ML"},
            new String[]{"Pandas", "AI/ML"},
            new String[]{"NumPy", "AI/ML"},
            new String[]{"Keras", "AI/ML"},
            new String[]{"OpenCV", "AI/ML"},
            new String[]{"Data Science", "AI/ML"},
            new String[]{"Data Analysis", "AI/ML"},
            // Testing
            new String[]{"JUnit", "Testing"},
            new String[]{"Mockito", "Testing"},
            new String[]{"Selenium", "Testing"},
            new String[]{"Jest", "Testing"},
            new String[]{"Pytest", "Testing"},
            // Soft Skills
            new String[]{"Communication", "Soft Skill"},
            new String[]{"Leadership", "Soft Skill"},
            new String[]{"Problem Solving", "Soft Skill"},
            new String[]{"Teamwork", "Soft Skill"},
            new String[]{"Critical Thinking", "Soft Skill"},
            new String[]{"Time Management", "Soft Skill"},
            new String[]{"Agile", "Methodology"},
            new String[]{"Scrum", "Methodology"},
            new String[]{"Jira", "Tool"},
            new String[]{"Postman", "Tool"},
            new String[]{"IntelliJ IDEA", "Tool"},
            new String[]{"VS Code", "Tool"},
            new String[]{"Microservices", "Architecture"},
            new String[]{"System Design", "Architecture"},
            new String[]{"Design Patterns", "Architecture"}
        );

        for (String[] skill : skills) {
            if (!skillRepo.existsByNormalizedName(skill[0].toLowerCase().trim())) {
                skillRepo.save(new Skill(skill[0], skill[1]));
            }
        }
    }

    private void seedJobs(RecruiterProfile recruiter, SkillRepository skillRepo,
                          JobRepository jobRepo, JobSkillRepository jobSkillRepo) {

        // Job 1: Java Backend Developer
        Job job1 = new Job();
        job1.setRecruiter(recruiter);
        job1.setTitle("Java Backend Developer");
        job1.setCompanyName("TechCorp Solutions");
        job1.setLocation("San Francisco, CA");
        job1.setEmploymentType(EmploymentType.FULL_TIME);
        job1.setExperienceRequired("1-3 years");
        job1.setDescription("We are looking for a passionate Java Backend Developer to join our growing engineering team. " +
                "You will be responsible for designing and implementing high-performance RESTful APIs and microservices " +
                "using Java and Spring Boot. You will work closely with frontend developers, database administrators, " +
                "and product managers to deliver scalable solutions.");
        job1.setResponsibilities("Design and develop RESTful APIs using Spring Boot. Write clean, maintainable Java code. " +
                "Collaborate with cross-functional teams. Perform code reviews. Write unit and integration tests.");
        job1.setQualifications("Bachelor's degree in Computer Science or equivalent. " +
                "Strong understanding of OOP principles. Experience with databases and SQL.");
        job1.setSalaryRange("$70,000 - $100,000");
        job1.setApplicationDeadline(LocalDate.now().plusMonths(2));
        job1.setStatus(JobStatus.ACTIVE);
        jobRepo.save(job1);
        addJobSkills(job1, List.of("Java", "Spring Boot", "Spring MVC", "Hibernate", "MySQL", "REST API", "Git"),
                List.of("Docker", "AWS", "Microservices", "JUnit"), skillRepo, jobSkillRepo);

        // Job 2: Full Stack Developer
        Job job2 = new Job();
        job2.setRecruiter(recruiter);
        job2.setTitle("Full Stack Developer");
        job2.setCompanyName("TechCorp Solutions");
        job2.setLocation("Remote");
        job2.setEmploymentType(EmploymentType.FULL_TIME);
        job2.setExperienceRequired("2-4 years");
        job2.setDescription("Join our team as a Full Stack Developer and build end-to-end web applications. " +
                "You will work with both frontend and backend technologies to create seamless user experiences. " +
                "This is a remote position with occasional travel for team meetings.");
        job2.setResponsibilities("Develop responsive web applications. Build and maintain APIs. " +
                "Implement database schemas. Participate in agile ceremonies.");
        job2.setQualifications("Experience with modern JavaScript frameworks. " +
                "Proficiency in Java and Spring Boot. Understanding of RESTful principles.");
        job2.setSalaryRange("$80,000 - $120,000");
        job2.setApplicationDeadline(LocalDate.now().plusMonths(1));
        job2.setStatus(JobStatus.ACTIVE);
        jobRepo.save(job2);
        addJobSkills(job2, List.of("Java", "Spring Boot", "JavaScript", "HTML", "CSS", "MySQL", "Git", "REST API"),
                List.of("React", "Docker", "PostgreSQL", "AWS"), skillRepo, jobSkillRepo);

        // Job 3: ML Engineer
        Job job3 = new Job();
        job3.setRecruiter(recruiter);
        job3.setTitle("Machine Learning Engineer");
        job3.setCompanyName("AI Innovations Inc");
        job3.setLocation("New York, NY");
        job3.setEmploymentType(EmploymentType.FULL_TIME);
        job3.setExperienceRequired("1-2 years");
        job3.setDescription("We are seeking a talented Machine Learning Engineer to help us build and deploy " +
                "AI-powered solutions. You will work on NLP, computer vision, and predictive modeling projects. " +
                "Collaborate with data scientists and software engineers to productionize ML models.");
        job3.setResponsibilities("Build and train ML models. Implement NLP pipelines. " +
                "Deploy models to production. Monitor model performance.");
        job3.setQualifications("Strong Python skills. Understanding of ML algorithms. " +
                "Experience with TensorFlow or PyTorch.");
        job3.setSalaryRange("$90,000 - $130,000");
        job3.setApplicationDeadline(LocalDate.now().plusWeeks(6));
        job3.setStatus(JobStatus.ACTIVE);
        jobRepo.save(job3);
        addJobSkills(job3, List.of("Python", "Machine Learning", "TensorFlow", "NLP", "SQL", "Git"),
                List.of("PyTorch", "Scikit-learn", "Deep Learning", "AWS", "Docker"), skillRepo, jobSkillRepo);

        // Job 4: Software Engineer Intern
        Job job4 = new Job();
        job4.setRecruiter(recruiter);
        job4.setTitle("Software Engineer Intern");
        job4.setCompanyName("StartupXYZ");
        job4.setLocation("Austin, TX");
        job4.setEmploymentType(EmploymentType.INTERNSHIP);
        job4.setExperienceRequired("0-1 year");
        job4.setDescription("Exciting internship opportunity for motivated students and fresh graduates. " +
                "You'll gain hands-on experience building real features that users love. " +
                "Mentorship provided by senior engineers. Potential for full-time conversion.");
        job4.setResponsibilities("Develop new features under guidance. Write and run tests. " +
                "Participate in code reviews. Learn our tech stack.");
        job4.setQualifications("Currently pursuing or recently completed CS degree. " +
                "Basic knowledge of programming. Eager to learn.");
        job4.setSalaryRange("$20 - $30/hour");
        job4.setApplicationDeadline(LocalDate.now().plusMonths(3));
        job4.setStatus(JobStatus.ACTIVE);
        jobRepo.save(job4);
        addJobSkills(job4, List.of("Java", "Python", "SQL", "Git"),
                List.of("Spring Boot", "JavaScript", "HTML", "CSS"), skillRepo, jobSkillRepo);

        // Job 5: DevOps Engineer
        Job job5 = new Job();
        job5.setRecruiter(recruiter);
        job5.setTitle("DevOps Engineer");
        job5.setCompanyName("CloudScale Technologies");
        job5.setLocation("Seattle, WA");
        job5.setEmploymentType(EmploymentType.FULL_TIME);
        job5.setExperienceRequired("3-5 years");
        job5.setDescription("Looking for an experienced DevOps Engineer to streamline our CI/CD pipelines and " +
                "manage our cloud infrastructure. You'll work with cutting-edge tools to ensure high availability " +
                "and performance of our platform.");
        job5.setResponsibilities("Manage cloud infrastructure. Implement CI/CD pipelines. " +
                "Monitor system performance. Ensure security best practices.");
        job5.setQualifications("Strong Linux and scripting skills. Experience with containerization. " +
                "Knowledge of cloud platforms.");
        job5.setSalaryRange("$110,000 - $150,000");
        job5.setApplicationDeadline(LocalDate.now().plusMonths(2));
        job5.setStatus(JobStatus.ACTIVE);
        jobRepo.save(job5);
        addJobSkills(job5, List.of("Docker", "Kubernetes", "AWS", "Linux", "Bash", "Git", "Jenkins"),
                List.of("Terraform", "Python", "Azure", "GCP"), skillRepo, jobSkillRepo);
    }

    private void addJobSkills(Job job, List<String> required, List<String> preferred,
                               SkillRepository skillRepo, JobSkillRepository jobSkillRepo) {
        for (String skillName : required) {
            skillRepo.findByNameIgnoreCase(skillName).ifPresent(skill -> {
                JobSkill js = new JobSkill(job, skill, SkillType.REQUIRED);
                jobSkillRepo.save(js);
            });
        }
        for (String skillName : preferred) {
            skillRepo.findByNameIgnoreCase(skillName).ifPresent(skill -> {
                JobSkill js = new JobSkill(job, skill, SkillType.PREFERRED);
                jobSkillRepo.save(js);
            });
        }
    }
}
