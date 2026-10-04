package com.resumescreening.service.impl;

import com.resumescreening.config.FileStorageConfig;
import com.resumescreening.dto.response.ResumeResponse;
import com.resumescreening.entity.*;
import com.resumescreening.exception.InvalidFileException;
import com.resumescreening.exception.ResourceNotFoundException;
import com.resumescreening.exception.UnauthorizedException;
import com.resumescreening.repository.*;
import com.resumescreening.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class ResumeServiceImpl implements ResumeService {

    private static final Logger log = LoggerFactory.getLogger(ResumeServiceImpl.class);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "docx");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final ResumeRepository resumeRepository;
    private final ResumeSkillRepository resumeSkillRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final UserRepository userRepository;
    private final SkillExtractionService skillExtractionService;
    private final ResumeTextExtractor textExtractor;
    private final ResumeInfoExtractor infoExtractor;
    private final FileStorageConfig fileStorageConfig;

    public ResumeServiceImpl(ResumeRepository resumeRepository,
                             ResumeSkillRepository resumeSkillRepository,
                             CandidateProfileRepository candidateProfileRepository,
                             UserRepository userRepository,
                             SkillExtractionService skillExtractionService,
                             ResumeTextExtractor textExtractor,
                             ResumeInfoExtractor infoExtractor,
                             FileStorageConfig fileStorageConfig) {
        this.resumeRepository = resumeRepository;
        this.resumeSkillRepository = resumeSkillRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.userRepository = userRepository;
        this.skillExtractionService = skillExtractionService;
        this.textExtractor = textExtractor;
        this.infoExtractor = infoExtractor;
        this.fileStorageConfig = fileStorageConfig;
    }

    @Override
    public ResumeResponse uploadResume(MultipartFile file, String username) {
        // Validate file
        validateFile(file);

        // Resolve candidate profile
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CandidateProfile profile = candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        // Determine file type
        String originalFilename = Objects.requireNonNull(file.getOriginalFilename());
        String extension = getExtension(originalFilename).toUpperCase();

        // Build stored file path
        String storedFileName = UUID.randomUUID() + "." + extension.toLowerCase();
        Path uploadPath = Paths.get(fileStorageConfig.getUploadDir()).toAbsolutePath().normalize();
        Path targetPath = uploadPath.resolve(storedFileName);

        // Save file to disk
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Resume file stored at: {}", targetPath);
        } catch (IOException e) {
            log.error("Failed to store resume file: {}", e.getMessage());
            throw new RuntimeException("Failed to store resume file. Please try again.");
        }

        // Deactivate previous resumes (keep history but only one active)
        List<Resume> previousResumes = resumeRepository.findActiveResumesByProfile(profile);
        previousResumes.forEach(r -> r.setActive(false));
        resumeRepository.saveAll(previousResumes);

        // Create resume record
        Resume resume = new Resume();
        resume.setCandidateProfile(profile);
        resume.setOriginalFileName(originalFilename);
        resume.setStoredFileName(storedFileName);
        resume.setFileType(extension);
        resume.setFileSize(file.getSize());
        resume.setFilePath(targetPath.toString());
        resume.setActive(true);
        resume.setParsed(false);
        resume = resumeRepository.save(resume);

        // Extract and parse
        parseResume(resume, targetPath, extension);

        return buildResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeResponse getResumeById(Long resumeId, String username) {
        Resume resume = getOwnedResume(resumeId, username);
        return buildResponse(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeResponse> getResumesForUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CandidateProfile profile = candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));

        return resumeRepository.findByCandidateProfile(profile).stream()
                .filter(Resume::isActive)
                .map(this::buildResponse)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteResume(Long resumeId, String username) {
        Resume resume = getOwnedResume(resumeId, username);
        resume.setActive(false);
        resumeRepository.save(resume);
        log.info("Resume {} soft-deleted for user {}", resumeId, username);
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeEntityById(Long resumeId) {
        return resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
    }

    @Override
    public void reParseResume(Long resumeId, String username) {
        Resume resume = getOwnedResume(resumeId, username);
        Path filePath = Paths.get(resume.getFilePath());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("Resume file not found on disk");
        }
        parseResume(resume, filePath, resume.getFileType());
    }

    // ---- Private helpers ----

    private void parseResume(Resume resume, Path filePath, String extension) {
        try (InputStream is = Files.newInputStream(filePath)) {
            // 1. Extract raw text
            String rawText = textExtractor.extract(is, extension);
            resume.setExtractedText(rawText);

            // 2. Extract structured info
            Map<String, String> info = infoExtractor.extractAll(rawText);
            resume.setExtractedName(info.get("name"));
            resume.setExtractedEmail(info.get("email"));
            resume.setExtractedPhone(info.get("phone"));
            resume.setExtractedEducation(info.get("education"));
            resume.setExtractedExperience(info.get("experience"));
            resume.setExtractedProjects(info.get("projects"));

            // 3. Extract skills
            List<Skill> skills = skillExtractionService.extractSkills(rawText);

            // Clear old skills and save new ones
            resumeSkillRepository.deleteByResume(resume);
            List<ResumeSkill> resumeSkills = skills.stream()
                    .map(skill -> new ResumeSkill(resume, skill))
                    .collect(Collectors.toList());
            resumeSkillRepository.saveAll(resumeSkills);
            resume.setResumeSkills(resumeSkills);

            resume.setParsed(true);
            resumeRepository.save(resume);

            log.info("Resume {} parsed successfully: {} skills found", resume.getId(), skills.size());

        } catch (IOException e) {
            log.error("Failed to parse resume {}: {}", resume.getId(), e.getMessage());
            resume.setParsed(false);
            resumeRepository.save(resume);
        }
    }

    private Resume getOwnedResume(Long resumeId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        CandidateProfile profile = candidateProfileRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate profile not found"));
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume", resumeId));
        if (!resume.getCandidateProfile().getId().equals(profile.getId())) {
            throw new UnauthorizedException("You do not have access to this resume");
        }
        return resume;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Please select a file to upload");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidFileException("Invalid file name");
        }

        String extension = getExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException(
                    "Invalid file type: '" + extension + "'. Only PDF and DOCX files are accepted.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.equals("application/octet-stream")
                && !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new InvalidFileException(
                    "Invalid file content type. Only PDF and DOCX files are accepted.");
        }
    }

    private String getExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            throw new InvalidFileException("File has no extension. Please upload a PDF or DOCX file.");
        }
        return filename.substring(dotIndex + 1);
    }

    private ResumeResponse buildResponse(Resume resume) {
        ResumeResponse response = ResumeResponse.from(resume);
        // Eagerly load skills if not already loaded
        if (resume.getResumeSkills() == null || resume.getResumeSkills().isEmpty()) {
            List<ResumeSkill> skills = resumeSkillRepository.findByResumeIdWithSkill(resume.getId());
            response.setSkills(skills.stream()
                    .map(rs -> rs.getSkill().getName())
                    .collect(Collectors.toList()));
        }
        return response;
    }
}
