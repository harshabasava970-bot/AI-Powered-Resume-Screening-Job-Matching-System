package com.resumescreening.controller;

import com.resumescreening.dto.response.ResumeResponse;
import com.resumescreening.exception.InvalidFileException;
import com.resumescreening.service.ResumeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/candidate/resumes")
@PreAuthorize("hasRole('CANDIDATE')")
public class ResumeController {

    private static final Logger log = LoggerFactory.getLogger(ResumeController.class);

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @GetMapping
    public String listResumes(Authentication auth, Model model) {
        List<ResumeResponse> resumes = resumeService.getResumesForUser(auth.getName());
        model.addAttribute("resumes", resumes);
        model.addAttribute("hasResume", !resumes.isEmpty());
        return "candidate/resumes";
    }

    @GetMapping("/upload")
    public String uploadPage() {
        return "candidate/resume-upload";
    }

    @PostMapping("/upload")
    public String uploadResume(@RequestParam("file") MultipartFile file,
                               Authentication auth,
                               RedirectAttributes redirectAttributes) {
        try {
            ResumeResponse resume = resumeService.uploadResume(file, auth.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Resume uploaded and parsed successfully! Found " +
                    resume.getSkills().size() + " skills.");
            return "redirect:/candidate/resumes/" + resume.getId();
        } catch (InvalidFileException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/candidate/resumes/upload";
        } catch (Exception e) {
            log.error("Resume upload failed for user {}: {}", auth.getName(), e.getMessage());
            redirectAttributes.addFlashAttribute("error",
                    "Upload failed: " + e.getMessage());
            return "redirect:/candidate/resumes/upload";
        }
    }

    @GetMapping("/{id}")
    public String viewResume(@PathVariable Long id, Authentication auth, Model model) {
        ResumeResponse resume = resumeService.getResumeById(id, auth.getName());
        model.addAttribute("resume", resume);
        return "candidate/resume-detail";
    }

    @PostMapping("/{id}/delete")
    public String deleteResume(@PathVariable Long id, Authentication auth,
                               RedirectAttributes redirectAttributes) {
        resumeService.deleteResume(id, auth.getName());
        redirectAttributes.addFlashAttribute("success", "Resume removed successfully.");
        return "redirect:/candidate/resumes";
    }

    @PostMapping("/{id}/reparse")
    public String reParseResume(@PathVariable Long id, Authentication auth,
                                RedirectAttributes redirectAttributes) {
        resumeService.reParseResume(id, auth.getName());
        redirectAttributes.addFlashAttribute("success", "Resume re-parsed successfully.");
        return "redirect:/candidate/resumes/" + id;
    }
}
