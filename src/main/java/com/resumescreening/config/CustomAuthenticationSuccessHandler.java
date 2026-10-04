package com.resumescreening.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import java.io.IOException;

public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("");

        String redirectUrl = switch (role) {
            case "ROLE_CANDIDATE" -> "/candidate/dashboard";
            case "ROLE_RECRUITER" -> "/recruiter/dashboard";
            case "ROLE_ADMIN"     -> "/admin/dashboard";
            default               -> "/";
        };

        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
