package com.resumescreening.controller;

import com.resumescreening.entity.Role;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {

            String role = authentication.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .findFirst().orElse("");

            return switch (role) {
                case "ROLE_CANDIDATE" -> "redirect:/candidate/dashboard";
                case "ROLE_RECRUITER" -> "redirect:/recruiter/dashboard";
                case "ROLE_ADMIN"     -> "redirect:/admin/dashboard";
                default               -> "index";
            };
        }
        return "index";
    }

    @GetMapping("/index")
    public String index() {
        return "index";
    }

    @GetMapping("/error/403")
    public String forbidden(Model model) {
        model.addAttribute("message", "You do not have permission to access this page.");
        return "error/403";
    }

    @GetMapping("/error/404")
    public String notFound(Model model) {
        model.addAttribute("message", "The page you are looking for does not exist.");
        return "error/404";
    }

    @GetMapping("/error/500")
    public String serverError() {
        return "error/500";
    }
}
