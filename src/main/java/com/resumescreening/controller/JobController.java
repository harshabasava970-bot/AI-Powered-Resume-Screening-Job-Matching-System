package com.resumescreening.controller;

import com.resumescreening.dto.request.JobRequest;
import com.resumescreening.dto.response.JobResponse;
import com.resumescreening.repository.SkillRepository;
import com.resumescreening.service.JobService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class JobController {

    private final JobService jobService;
    private final SkillRepository skillRepository;

    public JobController(JobService jobService, SkillRepository skillRepository) {
        this.jobService = jobService;
        this.skillRepository = skillRepository;
    }

    // ---- Public job browsing (any authenticated user) ----

    @GetMapping("/jobs")
    public String listJobs(@RequestParam(required = false) String search,
                           Model model) {
        List<JobResponse> jobs;
        if (search != null && !search.isBlank()) {
            jobs = jobService.searchJobs(search);
            model.addAttribute("searchQuery", search);
        } else {
            jobs = jobService.getAllActiveJobs();
        }
        model.addAttribute("jobs", jobs);
        model.addAttribute("totalJobs", jobs.size());
        return "jobs/list";
    }

    @GetMapping("/jobs/{id}")
    public String viewJob(@PathVariable Long id, Model model) {
        JobResponse job = jobService.getJobById(id);
        model.addAttribute("job", job);
        return "jobs/detail";
    }

    // ---- Recruiter job management ----

    @GetMapping("/recruiter/jobs")
    @PreAuthorize("hasRole('RECRUITER')")
    public String myJobs(Authentication auth, Model model) {
        List<JobResponse> jobs = jobService.getJobsByRecruiter(auth.getName());
        model.addAttribute("jobs", jobs);
        return "recruiter/jobs";
    }

    @GetMapping("/recruiter/jobs/create")
    @PreAuthorize("hasRole('RECRUITER')")
    public String createJobPage(Model model) {
        model.addAttribute("jobRequest", new JobRequest());
        model.addAttribute("allSkills", skillRepository.findAll());
        model.addAttribute("employmentTypes",
                com.resumescreening.entity.EmploymentType.values());
        return "recruiter/job-form";
    }

    @PostMapping("/recruiter/jobs/create")
    @PreAuthorize("hasRole('RECRUITER')")
    public String createJob(@Valid @ModelAttribute("jobRequest") JobRequest request,
                            BindingResult bindingResult,
                            Authentication auth,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allSkills", skillRepository.findAll());
            model.addAttribute("employmentTypes",
                    com.resumescreening.entity.EmploymentType.values());
            return "recruiter/job-form";
        }
        try {
            JobResponse job = jobService.createJob(request, auth.getName());
            redirectAttributes.addFlashAttribute("success",
                    "Job '" + job.getTitle() + "' created successfully!");
            return "redirect:/recruiter/jobs";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("allSkills", skillRepository.findAll());
            model.addAttribute("employmentTypes",
                    com.resumescreening.entity.EmploymentType.values());
            return "recruiter/job-form";
        }
    }

    @GetMapping("/recruiter/jobs/{id}/edit")
    @PreAuthorize("hasRole('RECRUITER')")
    public String editJobPage(@PathVariable Long id, Authentication auth, Model model) {
        JobResponse job = jobService.getJobById(id);
        model.addAttribute("job", job);
        model.addAttribute("allSkills", skillRepository.findAll());
        model.addAttribute("employmentTypes",
                com.resumescreening.entity.EmploymentType.values());

        // Pre-populate form
        JobRequest request = new JobRequest();
        request.setTitle(job.getTitle());
        request.setCompanyName(job.getCompanyName());
        request.setLocation(job.getLocation());
        request.setEmploymentType(job.getEmploymentType());
        request.setExperienceRequired(job.getExperienceRequired());
        request.setDescription(job.getDescription());
        request.setResponsibilities(job.getResponsibilities());
        request.setQualifications(job.getQualifications());
        request.setSalaryRange(job.getSalaryRange());
        request.setApplicationDeadline(job.getApplicationDeadline());
        request.setRequiredSkills(job.getRequiredSkills());
        request.setPreferredSkills(job.getPreferredSkills());
        model.addAttribute("jobRequest", request);
        return "recruiter/job-form";
    }

    @PostMapping("/recruiter/jobs/{id}/edit")
    @PreAuthorize("hasRole('RECRUITER')")
    public String editJob(@PathVariable Long id,
                          @Valid @ModelAttribute("jobRequest") JobRequest request,
                          BindingResult bindingResult,
                          Authentication auth,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("allSkills", skillRepository.findAll());
            model.addAttribute("employmentTypes",
                    com.resumescreening.entity.EmploymentType.values());
            return "recruiter/job-form";
        }
        try {
            jobService.updateJob(id, request, auth.getName());
            redirectAttributes.addFlashAttribute("success", "Job updated successfully!");
            return "redirect:/recruiter/jobs";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("allSkills", skillRepository.findAll());
            model.addAttribute("employmentTypes",
                    com.resumescreening.entity.EmploymentType.values());
            return "recruiter/job-form";
        }
    }

    @PostMapping("/recruiter/jobs/{id}/delete")
    @PreAuthorize("hasRole('RECRUITER')")
    public String deleteJob(@PathVariable Long id, Authentication auth,
                            RedirectAttributes redirectAttributes) {
        jobService.deleteJob(id, auth.getName());
        redirectAttributes.addFlashAttribute("success", "Job deleted successfully.");
        return "redirect:/recruiter/jobs";
    }
}
