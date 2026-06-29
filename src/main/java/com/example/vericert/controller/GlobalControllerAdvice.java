package com.example.vericert.controller;

import com.example.vericert.service.CustomUserDetails;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Value("${vericert.base-url:}")
    private String baseUrl;

    @ModelAttribute("appBaseUrl")
    public String appBaseUrl() {
        return baseUrl;
    }

    @ModelAttribute("currentTenant")
    public String currentTenant() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails user) {
            return user.getTenantName();
        }
        return "Nessun tenant";
    }
}