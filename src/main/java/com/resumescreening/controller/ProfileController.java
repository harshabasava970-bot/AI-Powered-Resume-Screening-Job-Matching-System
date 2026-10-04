package com.resumescreening.controller;

import com.resumescreening.dto.request.ProfileUpdateRequest;
import com.resumescreening.entity.Role;
import com.resumescreening.entity.User;
import com.resumescreening.repository.CandidateProfileRepository;
import com.resumescreening.repository.RecruiterProfileRepository;
import com.resumescreening.security.SecurityUtils;
import com.resumescreening.service.UserService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ProfileController {

    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final CandidateProfileRepository candidateProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;

    public ProfileController(UserService userService,
                             SecurityUtils securityUtils,
                             CandidateProfileRepository candidateProfileRepository,
                             RecruiterProfileRepository recruiterProfileRepository) {
        this.userService = userService;
        this.securityUtils = securityUtils;
        this.candidateProfileRepository = candidateProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
    }

    @GetMapping("/candidate/profile")
    public String candidateProfile(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("user", user);
        model.addAttribute("profile", candidateProfileRepository.findByUser(user).orElse(null));
        model.addAttribute("updateRequest", new ProfileUpdateRequest());
        return "candidate/profile";
    }

    @PostMapping("/candidate/profile")
    public String updateCandidateProfile(@Valid @ModelAttribute("updateRequest") ProfileUpdateRequest request,
                                         BindingResult bindingResult,
                                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "candidate/profile";
        }
        User user = securityUtils.getCurrentUserOrThrow();
        userService.updateCandidateProfile(user.getUsername(), request);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/candidate/profile";
    }

    @GetMapping("/recruiter/profile")
    public String recruiterProfile(Model model) {
        User user = securityUtils.getCurrentUserOrThrow();
        model.addAttribute("user", user);
        model.addAttribute("profile", recruiterProfileRepository.findByUser(user).orElse(null));
        model.addAttribute("updateRequest", new ProfileUpdateRequest());
        return "recruiter/profile";
    }

    @PostMapping("/recruiter/profile")
    public String updateRecruiterProfile(@Valid @ModelAttribute("updateRequest") ProfileUpdateRequest request,
                                         BindingResult bindingResult,
                                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "recruiter/profile";
        }
        User user = securityUtils.getCurrentUserOrThrow();
        userService.updateRecruiterProfile(user.getUsername(), request);
        redirectAttributes.addFlashAttribute("success", "Profile updated successfully!");
        return "redirect:/recruiter/profile";
    }
}
