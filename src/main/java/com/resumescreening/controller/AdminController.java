package com.resumescreening.controller;

import com.resumescreening.dto.response.UserResponse;
import com.resumescreening.entity.Role;
import com.resumescreening.repository.*;
import com.resumescreening.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JobRepository jobRepository;
    private final MatchAnalysisRepository matchAnalysisRepository;
    private final ResumeRepository resumeRepository;

    public AdminController(UserService userService,
                           UserRepository userRepository,
                           JobRepository jobRepository,
                           MatchAnalysisRepository matchAnalysisRepository,
                           ResumeRepository resumeRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
        this.jobRepository = jobRepository;
        this.matchAnalysisRepository = matchAnalysisRepository;
        this.resumeRepository = resumeRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        // Stats
        model.addAttribute("totalUsers", userRepository.count());
        model.addAttribute("totalCandidates", userRepository.countByRole(Role.CANDIDATE));
        model.addAttribute("totalRecruiters", userRepository.countByRole(Role.RECRUITER));
        model.addAttribute("totalJobs", jobRepository.count());
        model.addAttribute("activeJobs", jobRepository.countAllActive());
        model.addAttribute("totalAnalyses", matchAnalysisRepository.countAll());
        model.addAttribute("totalResumes", resumeRepository.count());
        model.addAttribute("activeUsers", userRepository.countActiveUsers());

        // Recent data
        model.addAttribute("recentUsers", userRepository.findAllOrderByCreatedAtDesc()
                .stream().limit(5).toList());
        model.addAttribute("recentJobs", jobRepository.findAllOrderByPostedAtDesc()
                .stream().limit(5).toList());

        return "admin/dashboard";
    }

    @GetMapping("/users")
    public String users(Model model) {
        List<UserResponse> allUsers = userService.getAllUsers();
        model.addAttribute("users", allUsers);
        model.addAttribute("totalCount", allUsers.size());
        return "admin/users";
    }

    @GetMapping("/users/candidates")
    public String candidates(Model model) {
        List<UserResponse> candidates = userService.getUsersByRole("CANDIDATE");
        model.addAttribute("users", candidates);
        model.addAttribute("roleFilter", "CANDIDATE");
        model.addAttribute("totalCount", candidates.size());
        return "admin/users";
    }

    @GetMapping("/users/recruiters")
    public String recruiters(Model model) {
        List<UserResponse> recruiters = userService.getUsersByRole("RECRUITER");
        model.addAttribute("users", recruiters);
        model.addAttribute("roleFilter", "RECRUITER");
        model.addAttribute("totalCount", recruiters.size());
        return "admin/users";
    }

    @PostMapping("/users/{id}/toggle")
    public String toggleUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.toggleUserEnabled(id);
        redirectAttributes.addFlashAttribute("success", "User status updated successfully");
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.deleteUser(id);
        redirectAttributes.addFlashAttribute("success", "User deleted successfully");
        return "redirect:/admin/users";
    }

    @GetMapping("/jobs")
    public String jobs(Model model) {
        model.addAttribute("jobs", jobRepository.findAllOrderByPostedAtDesc());
        return "admin/jobs";
    }

    @PostMapping("/jobs/{id}/suspend")
    public String suspendJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        jobRepository.findById(id).ifPresent(job -> {
            job.setStatus(com.resumescreening.entity.JobStatus.SUSPENDED);
            jobRepository.save(job);
        });
        redirectAttributes.addFlashAttribute("success", "Job suspended successfully");
        return "redirect:/admin/jobs";
    }

    @PostMapping("/jobs/{id}/delete")
    public String deleteJob(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        jobRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Job deleted successfully");
        return "redirect:/admin/jobs";
    }
}
