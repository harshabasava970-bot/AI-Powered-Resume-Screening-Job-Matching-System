package com.resumescreening.controller;

import com.resumescreening.dto.request.RegisterRequest;
import com.resumescreening.dto.response.UserResponse;
import com.resumescreening.exception.DuplicateResourceException;
import com.resumescreening.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            @RequestParam(required = false) String expired,
                            Model model) {
        if (error != null) {
            model.addAttribute("error", "Invalid username/email or password. Please try again.");
        }
        if (logout != null) {
            model.addAttribute("message", "You have been logged out successfully.");
        }
        if (expired != null) {
            model.addAttribute("error", "Your session has expired. Please login again.");
        }
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                           BindingResult bindingResult,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        // Check for binding errors first
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        // Custom validations
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            model.addAttribute("error", "Passwords do not match");
            return "auth/register";
        }

        try {
            UserResponse user = userService.register(request);
            redirectAttributes.addFlashAttribute("success",
                    "Registration successful! Please login with your credentials.");
            log.info("New user registered: {} with role {}", user.getUsername(), user.getRole());
            return "redirect:/auth/login";

        } catch (DuplicateResourceException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout() {
        // Spring Security handles actual logout via POST /auth/logout
        // This GET just redirects to the login page
        return "redirect:/auth/login?logout=true";
    }
}
