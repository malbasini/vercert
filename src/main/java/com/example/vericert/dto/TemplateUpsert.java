package com.example.vericert.dto;

import jakarta.validation.constraints.NotBlank;

public record TemplateUpsert(
        Long id,
        @NotBlank(message="Name obbligatorio")
        String name,
        @NotBlank(message="Versione obbligatoria")
        String version,
        @NotBlank(message = "Il campo html è obbligatorio")
        String html,
        String variablesUserJson,
        String systemsVariables,
        boolean active,
        String captchaToken
) {}